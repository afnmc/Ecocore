package com.azthera.ecocore.data.dao.impl;
 
import com.azthera.ecocore.data.DatabaseManager;
import com.azthera.ecocore.data.dao.MarketEventDAO;
import com.azthera.ecocore.data.migration.SqlDialect;
import com.azthera.ecocore.data.model.MarketEventRecord;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
 
/**
 * JDBC implementation of {@link MarketEventDAO} backed by the {@code ecocore_market_events} table.
 */
public final class SqlMarketEventDAO implements MarketEventDAO {
 
    private final DatabaseManager databaseManager;
    private final SqlDialect dialect;
 
    public SqlMarketEventDAO(DatabaseManager databaseManager, SqlDialect dialect) {
        this.databaseManager = databaseManager;
        this.dialect = dialect;
    }
 
    @Override
    public Optional<MarketEventRecord> findActiveByType(String eventType) throws SQLException {
        String sql = "SELECT id, event_type, started_at_millis, ends_at_millis, price_modifier, demand_modifier, " +
            "active FROM ecocore_market_events WHERE event_type = ? AND active = ? " +
            "ORDER BY started_at_millis DESC LIMIT 1";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, eventType);
            statement.setBoolean(2, true);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        }
        return Optional.empty();
    }
 
    @Override
    public List<MarketEventRecord> findAllActive() throws SQLException {
        String sql = "SELECT id, event_type, started_at_millis, ends_at_millis, price_modifier, demand_modifier, " +
            "active FROM ecocore_market_events WHERE active = ?";
 
        List<MarketEventRecord> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, true);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
        }
        return results;
    }
 
    @Override
    public List<MarketEventRecord> findRecentHistory(int limit) throws SQLException {
        String sql = "SELECT id, event_type, started_at_millis, ends_at_millis, price_modifier, demand_modifier, " +
            "active FROM ecocore_market_events ORDER BY started_at_millis DESC LIMIT ?";
 
        List<MarketEventRecord> results = new ArrayList<>();
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
    public long insert(MarketEventRecord record) throws SQLException {
        String sql = "INSERT INTO ecocore_market_events (event_type, started_at_millis, ends_at_millis, " +
            "price_modifier, demand_modifier, active) VALUES (?, ?, ?, ?, ?, ?)";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, record.getEventType());
            statement.setLong(2, record.getStartedAtMillis());
            statement.setLong(3, record.getEndsAtMillis());
            statement.setDouble(4, record.getPriceModifier());
            statement.setDouble(5, record.getDemandModifier());
            statement.setBoolean(6, record.isActive());
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
    public void update(MarketEventRecord record) throws SQLException {
        String sql = "UPDATE ecocore_market_events SET ends_at_millis = ?, price_modifier = ?, " +
            "demand_modifier = ?, active = ? WHERE id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, record.getEndsAtMillis());
            statement.setDouble(2, record.getPriceModifier());
            statement.setDouble(3, record.getDemandModifier());
            statement.setBoolean(4, record.isActive());
            statement.setLong(5, record.getId());
            statement.executeUpdate();
        }
    }
 
    @Override
    public void deactivateExpired(long nowMillis) throws SQLException {
        String sql = "UPDATE ecocore_market_events SET active = ? WHERE active = ? AND ends_at_millis <= ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, false);
            statement.setBoolean(2, true);
            statement.setLong(3, nowMillis);
            statement.executeUpdate();
        }
    }
 
    private MarketEventRecord mapRow(ResultSet resultSet) throws SQLException {
        return new MarketEventRecord(
            resultSet.getLong("id"),
            resultSet.getString("event_type"),
            resultSet.getLong("started_at_millis"),
            resultSet.getLong("ends_at_millis"),
            resultSet.getDouble("price_modifier"),
            resultSet.getDouble("demand_modifier"),
            resultSet.getBoolean("active")
        );
    }
}
