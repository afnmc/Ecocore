package com.azthera.ecocore.data.dao.impl;
 
import com.azthera.ecocore.data.DatabaseManager;
import com.azthera.ecocore.data.dao.TransactionLogDAO;
import com.azthera.ecocore.data.migration.SqlDialect;
import com.azthera.ecocore.data.model.TransactionLogEntry;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
 
/**
 * JDBC implementation of {@link TransactionLogDAO} backed by the immutable
 * {@code ecocore_transaction_log} table.
 */
public final class SqlTransactionLogDAO implements TransactionLogDAO {
 
    private final DatabaseManager databaseManager;
    private final SqlDialect dialect;
 
    public SqlTransactionLogDAO(DatabaseManager databaseManager, SqlDialect dialect) {
        this.databaseManager = databaseManager;
        this.dialect = dialect;
    }
 
    @Override
    public long insert(TransactionLogEntry entry) throws SQLException {
        String sql = "INSERT INTO ecocore_transaction_log (player_id, currency_id, transaction_type, amount, " +
            "balance_after, description, timestamp_millis) VALUES (?, ?, ?, ?, ?, ?, ?)";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, entry.getPlayerId().toString());
            statement.setString(2, entry.getCurrencyId());
            statement.setString(3, entry.getTransactionType());
            statement.setDouble(4, entry.getAmount());
            statement.setDouble(5, entry.getBalanceAfter());
            statement.setString(6, entry.getDescription());
            statement.setLong(7, entry.getTimestampMillis());
            statement.executeUpdate();
 
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }
        return -1L;
    }
 
    @Override
    public List<TransactionLogEntry> findForPlayer(UUID playerId, int limit, int offset) throws SQLException {
        String sql = "SELECT id, player_id, currency_id, transaction_type, amount, balance_after, description, " +
            "timestamp_millis FROM ecocore_transaction_log WHERE player_id = ? " +
            "ORDER BY timestamp_millis DESC LIMIT ? OFFSET ?";
 
        List<TransactionLogEntry> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
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
    public List<TransactionLogEntry> findByType(String transactionType, long fromMillis, long toMillis, int limit)
        throws SQLException {
        String sql = "SELECT id, player_id, currency_id, transaction_type, amount, balance_after, description, " +
            "timestamp_millis FROM ecocore_transaction_log " +
            "WHERE transaction_type = ? AND timestamp_millis BETWEEN ? AND ? " +
            "ORDER BY timestamp_millis DESC LIMIT ?";
 
        List<TransactionLogEntry> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transactionType);
            statement.setLong(2, fromMillis);
            statement.setLong(3, toMillis);
            statement.setInt(4, limit);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
        }
        return results;
    }
 
    @Override
    public List<TransactionLogEntry> findRecent(int limit) throws SQLException {
        String sql = "SELECT id, player_id, currency_id, transaction_type, amount, balance_after, description, " +
            "timestamp_millis FROM ecocore_transaction_log ORDER BY timestamp_millis DESC LIMIT ?";
 
        List<TransactionLogEntry> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
        }
        return results;
    }
 
    @Override
    public double sumNetAmountForCurrency(String currencyId, long fromMillis, long toMillis) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM ecocore_transaction_log " +
            "WHERE currency_id = ? AND timestamp_millis BETWEEN ? AND ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currencyId);
            statement.setLong(2, fromMillis);
            statement.setLong(3, toMillis);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("total");
                }
            }
        }
        return 0.0;
    }
 
    @Override
    public void purgeOlderThan(long timestampMillis) throws SQLException {
        String sql = "DELETE FROM ecocore_transaction_log WHERE timestamp_millis < ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, timestampMillis);
            statement.executeUpdate();
        }
    }
 
    private TransactionLogEntry mapRow(ResultSet resultSet) throws SQLException {
        return new TransactionLogEntry(
            resultSet.getLong("id"),
            UUID.fromString(resultSet.getString("player_id")),
            resultSet.getString("currency_id"),
            resultSet.getString("transaction_type"),
            resultSet.getDouble("amount"),
            resultSet.getDouble("balance_after"),
            resultSet.getString("description"),
            resultSet.getLong("timestamp_millis")
        );
    }
}
