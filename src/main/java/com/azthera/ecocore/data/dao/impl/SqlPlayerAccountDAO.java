package com.azthera.ecocore.data.dao.impl;
 
import com.azthera.ecocore.data.DatabaseManager;
import com.azthera.ecocore.data.dao.PlayerAccountDAO;
import com.azthera.ecocore.data.migration.SqlDialect;
import com.azthera.ecocore.data.model.PlayerAccount;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
/**
 * JDBC implementation of {@link PlayerAccountDAO} backed by the
 * {@code ecocore_player_accounts} table. Upsert syntax branches on dialect
 * since SQLite and MySQL/MariaDB use different "insert or update" clauses.
 */
public final class SqlPlayerAccountDAO implements PlayerAccountDAO {
 
    private final DatabaseManager databaseManager;
    private final SqlDialect dialect;
 
    public SqlPlayerAccountDAO(DatabaseManager databaseManager, SqlDialect dialect) {
        this.databaseManager = databaseManager;
        this.dialect = dialect;
    }
 
    @Override
    public Optional<PlayerAccount> find(UUID playerId, String currencyId) throws SQLException {
        String sql = "SELECT player_id, currency_id, balance, last_updated_millis " +
            "FROM ecocore_player_accounts WHERE player_id = ? AND currency_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, currencyId);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        }
        return Optional.empty();
    }
 
    @Override
    public List<PlayerAccount> findAllForPlayer(UUID playerId) throws SQLException {
        String sql = "SELECT player_id, currency_id, balance, last_updated_millis " +
            "FROM ecocore_player_accounts WHERE player_id = ?";
 
        List<PlayerAccount> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
 
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
        }
        return results;
    }
 
    @Override
    public List<PlayerAccount> findAllForCurrency(String currencyId, int limit, int offset) throws SQLException {
        String sql = "SELECT player_id, currency_id, balance, last_updated_millis " +
            "FROM ecocore_player_accounts WHERE currency_id = ? " +
            "ORDER BY balance DESC LIMIT ? OFFSET ?";
 
        List<PlayerAccount> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currencyId);
            statement.setInt(2, limit);
            statement.setInt(3, offset);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
        }
        return results;
    }
 
    @Override
    public void upsert(PlayerAccount account) throws SQLException {
        String sql = switch (dialect.getDatabaseType()) {
            case SQLITE -> "INSERT INTO ecocore_player_accounts (player_id, currency_id, balance, last_updated_millis) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(player_id, currency_id) DO UPDATE SET balance = excluded.balance, " +
                "last_updated_millis = excluded.last_updated_millis";
            case MYSQL, MARIADB -> "INSERT INTO ecocore_player_accounts (player_id, currency_id, balance, last_updated_millis) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE balance = VALUES(balance), last_updated_millis = VALUES(last_updated_millis)";
        };
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getPlayerId().toString());
            statement.setString(2, account.getCurrencyId());
            statement.setDouble(3, account.getBalance());
            statement.setLong(4, account.getLastUpdatedMillis());
            statement.executeUpdate();
        }
    }
 
    @Override
    public void delete(UUID playerId, String currencyId) throws SQLException {
        String sql = "DELETE FROM ecocore_player_accounts WHERE player_id = ? AND currency_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, currencyId);
            statement.executeUpdate();
        }
    }
 
    @Override
    public long countAccountsForCurrency(String currencyId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM ecocore_player_accounts WHERE currency_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currencyId);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("total");
                }
            }
        }
        return 0L;
    }
 
    @Override
    public double sumBalancesForCurrency(String currencyId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(balance), 0) AS total FROM ecocore_player_accounts WHERE currency_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currencyId);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("total");
                }
            }
        }
        return 0.0;
    }
 
    private PlayerAccount mapRow(ResultSet resultSet) throws SQLException {
        return new PlayerAccount(
            UUID.fromString(resultSet.getString("player_id")),
            resultSet.getString("currency_id"),
            resultSet.getDouble("balance"),
            resultSet.getLong("last_updated_millis")
        );
    }
}
