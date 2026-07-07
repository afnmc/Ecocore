package com.azthera.ecocore.data.dao.impl;
 
import com.azthera.ecocore.data.DatabaseManager;
import com.azthera.ecocore.data.dao.MinionDAO;
import com.azthera.ecocore.data.migration.SqlDialect;
import com.azthera.ecocore.data.model.MinionData;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
/**
 * JDBC implementation of {@link MinionDAO} backed by the {@code ecocore_minions} table.
 * Unlike the other DAOs, this one exposes separate insert/update methods rather than
 * a single upsert, since minion placement (insert) and periodic state saves (update)
 * are semantically distinct operations with different call sites.
 */
public final class SqlMinionDAO implements MinionDAO {
 
    private final DatabaseManager databaseManager;
    private final SqlDialect dialect;
 
    public SqlMinionDAO(DatabaseManager databaseManager, SqlDialect dialect) {
        this.databaseManager = databaseManager;
        this.dialect = dialect;
    }
 
    @Override
    public Optional<MinionData> find(UUID minionId) throws SQLException {
        String sql = "SELECT minion_id, owner_id, minion_type, world_name, x, y, z, speed_level, capacity_level, " +
            "fortune_level, fuel_remaining, serialized_storage FROM ecocore_minions WHERE minion_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, minionId.toString());
 
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        }
        return Optional.empty();
    }
 
    @Override
    public List<MinionData> findAllForOwner(UUID ownerId) throws SQLException {
        String sql = "SELECT minion_id, owner_id, minion_type, world_name, x, y, z, speed_level, capacity_level, " +
            "fortune_level, fuel_remaining, serialized_storage FROM ecocore_minions WHERE owner_id = ?";
 
        List<MinionData> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerId.toString());
 
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
        }
        return results;
    }
 
    @Override
    public List<MinionData> findAllInWorld(String worldName) throws SQLException {
        String sql = "SELECT minion_id, owner_id, minion_type, world_name, x, y, z, speed_level, capacity_level, " +
            "fortune_level, fuel_remaining, serialized_storage FROM ecocore_minions WHERE world_name = ?";
 
        List<MinionData> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldName);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
        }
        return results;
    }
 
    @Override
    public void insert(MinionData minionData) throws SQLException {
        String sql = "INSERT INTO ecocore_minions (minion_id, owner_id, minion_type, world_name, x, y, z, " +
            "speed_level, capacity_level, fortune_level, fuel_remaining, serialized_storage) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAll(statement, minionData);
            statement.executeUpdate();
        }
    }
 
    @Override
    public void update(MinionData minionData) throws SQLException {
        String sql = "UPDATE ecocore_minions SET owner_id = ?, minion_type = ?, world_name = ?, x = ?, y = ?, z = ?, " +
            "speed_level = ?, capacity_level = ?, fortune_level = ?, fuel_remaining = ?, serialized_storage = ? " +
            "WHERE minion_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, minionData.getOwnerId().toString());
            statement.setString(2, minionData.getMinionType());
            statement.setString(3, minionData.getWorldName());
            statement.setDouble(4, minionData.getX());
            statement.setDouble(5, minionData.getY());
            statement.setDouble(6, minionData.getZ());
            statement.setInt(7, minionData.getSpeedLevel());
            statement.setInt(8, minionData.getCapacityLevel());
            statement.setInt(9, minionData.getFortuneLevel());
            statement.setInt(10, minionData.getFuelRemaining());
            statement.setBytes(11, minionData.getSerializedStorage());
            statement.setString(12, minionData.getMinionId().toString());
            statement.executeUpdate();
        }
    }
 
    @Override
    public void delete(UUID minionId) throws SQLException {
        String sql = "DELETE FROM ecocore_minions WHERE minion_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, minionId.toString());
            statement.executeUpdate();
        }
    }
 
    @Override
    public long countForOwner(UUID ownerId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM ecocore_minions WHERE owner_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerId.toString());
 
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("total");
                }
            }
        }
        return 0L;
    }
 
    private void bindAll(PreparedStatement statement, MinionData minionData) throws SQLException {
        statement.setString(1, minionData.getMinionId().toString());
        statement.setString(2, minionData.getOwnerId().toString());
        statement.setString(3, minionData.getMinionType());
        statement.setString(4, minionData.getWorldName());
        statement.setDouble(5, minionData.getX());
        statement.setDouble(6, minionData.getY());
        statement.setDouble(7, minionData.getZ());
        statement.setInt(8, minionData.getSpeedLevel());
        statement.setInt(9, minionData.getCapacityLevel());
        statement.setInt(10, minionData.getFortuneLevel());
        statement.setInt(11, minionData.getFuelRemaining());
        statement.setBytes(12, minionData.getSerializedStorage());
    }
 
    private MinionData mapRow(ResultSet resultSet) throws SQLException {
        return new MinionData(
            UUID.fromString(resultSet.getString("minion_id")),
            UUID.fromString(resultSet.getString("owner_id")),
            resultSet.getString("minion_type"),
            resultSet.getString("world_name"),
            resultSet.getDouble("x"),
            resultSet.getDouble("y"),
            resultSet.getDouble("z"),
            resultSet.getInt("speed_level"),
            resultSet.getInt("capacity_level"),
            resultSet.getInt("fortune_level"),
            resultSet.getInt("fuel_remaining"),
            resultSet.getBytes("serialized_storage")
        );
    }
}
