package com.azthera.ecocore.data.dao.impl;
 
import com.azthera.ecocore.data.DatabaseManager;
import com.azthera.ecocore.data.dao.ShopItemDAO;
import com.azthera.ecocore.data.migration.SqlDialect;
import com.azthera.ecocore.data.model.PriceHistoryEntry;
import com.azthera.ecocore.data.model.ShopItemRecord;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
 
/**
 * JDBC implementation of {@link ShopItemDAO} backed by the
 * {@code ecocore_shop_items} and {@code ecocore_price_history} tables.
 */
public final class SqlShopItemDAO implements ShopItemDAO {
 
    private final DatabaseManager databaseManager;
    private final SqlDialect dialect;
 
    public SqlShopItemDAO(DatabaseManager databaseManager, SqlDialect dialect) {
        this.databaseManager = databaseManager;
        this.dialect = dialect;
    }
 
    @Override
    public Optional<ShopItemRecord> find(String itemId) throws SQLException {
        String sql = "SELECT item_id, base_price, current_price, min_price, max_price, elasticity, demand, supply, " +
            "restock_interval_seconds, stock, last_restock_millis FROM ecocore_shop_items WHERE item_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        }
        return Optional.empty();
    }
 
    @Override
    public List<ShopItemRecord> findAll() throws SQLException {
        String sql = "SELECT item_id, base_price, current_price, min_price, max_price, elasticity, demand, supply, " +
            "restock_interval_seconds, stock, last_restock_millis FROM ecocore_shop_items";
 
        List<ShopItemRecord> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                results.add(mapRow(resultSet));
            }
        }
        return results;
    }
 
    @Override
    public void upsert(ShopItemRecord record) throws SQLException {
        String sql = switch (dialect.getDatabaseType()) {
            case SQLITE -> "INSERT INTO ecocore_shop_items (item_id, base_price, current_price, min_price, max_price, " +
                "elasticity, demand, supply, restock_interval_seconds, stock, last_restock_millis) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(item_id) DO UPDATE SET base_price = excluded.base_price, " +
                "current_price = excluded.current_price, min_price = excluded.min_price, max_price = excluded.max_price, " +
                "elasticity = excluded.elasticity, demand = excluded.demand, supply = excluded.supply, " +
                "restock_interval_seconds = excluded.restock_interval_seconds, stock = excluded.stock, " +
                "last_restock_millis = excluded.last_restock_millis";
            case MYSQL, MARIADB -> "INSERT INTO ecocore_shop_items (item_id, base_price, current_price, min_price, max_price, " +
                "elasticity, demand, supply, restock_interval_seconds, stock, last_restock_millis) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE base_price = VALUES(base_price), current_price = VALUES(current_price), " +
                "min_price = VALUES(min_price), max_price = VALUES(max_price), elasticity = VALUES(elasticity), " +
                "demand = VALUES(demand), supply = VALUES(supply), " +
                "restock_interval_seconds = VALUES(restock_interval_seconds), stock = VALUES(stock), " +
                "last_restock_millis = VALUES(last_restock_millis)";
        };
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.getItemId());
            statement.setDouble(2, record.getBasePrice());
            statement.setDouble(3, record.getCurrentPrice());
            statement.setDouble(4, record.getMinPrice());
            statement.setDouble(5, record.getMaxPrice());
            statement.setDouble(6, record.getElasticity());
            statement.setDouble(7, record.getDemand());
            statement.setDouble(8, record.getSupply());
            statement.setLong(9, record.getRestockIntervalSeconds());
            statement.setInt(10, record.getStock());
            statement.setLong(11, record.getLastRestockMillis());
            statement.executeUpdate();
        }
    }
 
    @Override
    public void delete(String itemId) throws SQLException {
        String sql = "DELETE FROM ecocore_shop_items WHERE item_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);
            statement.executeUpdate();
        }
    }
 
    @Override
    public void insertPriceHistory(PriceHistoryEntry entry) throws SQLException {
        String sql = "INSERT INTO ecocore_price_history (item_id, price, demand, supply, timestamp_millis) " +
            "VALUES (?, ?, ?, ?, ?)";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.getItemId());
            statement.setDouble(2, entry.getPrice());
            statement.setDouble(3, entry.getDemand());
            statement.setDouble(4, entry.getSupply());
            statement.setLong(5, entry.getTimestampMillis());
            statement.executeUpdate();
        }
    }
 
    @Override
    public List<PriceHistoryEntry> findPriceHistory(String itemId, int limit) throws SQLException {
        String sql = "SELECT id, item_id, price, demand, supply, timestamp_millis FROM ecocore_price_history " +
            "WHERE item_id = ? ORDER BY timestamp_millis DESC LIMIT ?";
 
        List<PriceHistoryEntry> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);
            statement.setInt(2, limit);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new PriceHistoryEntry(
                        resultSet.getLong("id"),
                        resultSet.getString("item_id"),
                        resultSet.getDouble("price"),
                        resultSet.getDouble("demand"),
                        resultSet.getDouble("supply"),
                        resultSet.getLong("timestamp_millis")
                    ));
                }
            }
        }
        return results;
    }
 
    @Override
    public void purgePriceHistoryOlderThan(long timestampMillis) throws SQLException {
        String sql = "DELETE FROM ecocore_price_history WHERE timestamp_millis < ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, timestampMillis);
            statement.executeUpdate();
        }
    }
 
    private ShopItemRecord mapRow(ResultSet resultSet) throws SQLException {
        return new ShopItemRecord(
            resultSet.getString("item_id"),
            resultSet.getDouble("base_price"),
            resultSet.getDouble("current_price"),
            resultSet.getDouble("min_price"),
            resultSet.getDouble("max_price"),
            resultSet.getDouble("elasticity"),
            resultSet.getDouble("demand"),
            resultSet.getDouble("supply"),
            resultSet.getLong("restock_interval_seconds"),
            resultSet.getInt("stock"),
            resultSet.getLong("last_restock_millis")
        );
    }
}
