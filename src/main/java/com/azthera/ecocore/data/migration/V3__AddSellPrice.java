package com.azthera.ecocore.data.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Adds the {@code sell_price} column to {@code ecocore_shop_items}, enabling
 * separate buy/sell pricing for every shop item. The sell price defaults to
 * 70% of the current buy price (30% spread) for existing records, matching
 * a typical NPC shop economy where selling back incurs a penalty.
 *
 * <p>This separation allows:</p>
 * <ul>
 *   <li>Admin to configure buy and sell prices independently per item</li>
 *   <li>The shop to display both prices clearly in the GUI</li>
 *   <li>Sell GUI to use the sell price instead of the buy price</li>
 *   <li>Better economy control (prevents arbitrage exploits)</li>
 * </ul>
 */
public final class V3__AddSellPrice implements MigrationScript {
    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public String getDescription() {
        return "Add 'sell_price' column to ecocore_shop_items for separate buy/sell pricing.";
    }

    @Override
    public void apply(Connection connection, SqlDialect dialect) throws SQLException {
        if (columnExists(connection, "ecocore_shop_items", "sell_price")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE ecocore_shop_items ADD COLUMN sell_price DOUBLE NOT NULL DEFAULT 0");
            // Default sell_price = 70% of current_price (30% spread) for existing records
            statement.execute("UPDATE ecocore_shop_items SET sell_price = current_price * 0.7 WHERE sell_price = 0");
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            return resultSet.next();
        }
    }
}
