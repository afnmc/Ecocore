package com.azthera.ecocore.data.dao.impl;
 
import com.azthera.ecocore.data.DatabaseManager;
import com.azthera.ecocore.data.dao.QuestDataDAO;
import com.azthera.ecocore.data.migration.SqlDialect;
import com.azthera.ecocore.data.model.QuestData;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
/**
 * JDBC implementation of {@link QuestDataDAO} backed by the {@code ecocore_quest_data} table.
 */
public final class SqlQuestDataDAO implements QuestDataDAO {
 
    private final DatabaseManager databaseManager;
    private final SqlDialect dialect;
 
    public SqlQuestDataDAO(DatabaseManager databaseManager, SqlDialect dialect) {
        this.databaseManager = databaseManager;
        this.dialect = dialect;
    }
 
    @Override
    public Optional<QuestData> find(UUID playerId, String questInstanceId) throws SQLException {
        String sql = "SELECT player_id, quest_instance_id, quest_definition_id, progress, required_progress, " +
            "completed, reward_claimed, accepted_at_millis, expires_at_millis FROM ecocore_quest_data " +
            "WHERE player_id = ? AND quest_instance_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, questInstanceId);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        }
        return Optional.empty();
    }
 
    @Override
    public List<QuestData> findAllForPlayer(UUID playerId) throws SQLException {
        String sql = "SELECT player_id, quest_instance_id, quest_definition_id, progress, required_progress, " +
            "completed, reward_claimed, accepted_at_millis, expires_at_millis FROM ecocore_quest_data " +
            "WHERE player_id = ?";
 
        List<QuestData> results = new ArrayList<>();
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
    public List<QuestData> findActiveForPlayer(UUID playerId, long nowMillis) throws SQLException {
        String sql = "SELECT player_id, quest_instance_id, quest_definition_id, progress, required_progress, " +
            "completed, reward_claimed, accepted_at_millis, expires_at_millis FROM ecocore_quest_data " +
            "WHERE player_id = ? AND expires_at_millis > ?";
 
        List<QuestData> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setLong(2, nowMillis);
 
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
        }
        return results;
    }
 
    @Override
    public void upsert(QuestData questData) throws SQLException {
        String sql = switch (dialect.getDatabaseType()) {
            case SQLITE -> "INSERT INTO ecocore_quest_data (player_id, quest_instance_id, quest_definition_id, " +
                "progress, required_progress, completed, reward_claimed, accepted_at_millis, expires_at_millis) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_id, quest_instance_id) DO UPDATE SET progress = excluded.progress, " +
                "required_progress = excluded.required_progress, completed = excluded.completed, " +
                "reward_claimed = excluded.reward_claimed, expires_at_millis = excluded.expires_at_millis";
            case MYSQL, MARIADB -> "INSERT INTO ecocore_quest_data (player_id, quest_instance_id, quest_definition_id, " +
                "progress, required_progress, completed, reward_claimed, accepted_at_millis, expires_at_millis) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE progress = VALUES(progress), required_progress = VALUES(required_progress), " +
                "completed = VALUES(completed), reward_claimed = VALUES(reward_claimed), " +
                "expires_at_millis = VALUES(expires_at_millis)";
        };
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, questData.getPlayerId().toString());
            statement.setString(2, questData.getQuestInstanceId());
            statement.setString(3, questData.getQuestDefinitionId());
            statement.setDouble(4, questData.getProgress());
            statement.setDouble(5, questData.getRequiredProgress());
            statement.setBoolean(6, questData.isCompleted());
            statement.setBoolean(7, questData.isRewardClaimed());
            statement.setLong(8, questData.getAcceptedAtMillis());
            statement.setLong(9, questData.getExpiresAtMillis());
            statement.executeUpdate();
        }
    }
 
    @Override
    public void delete(UUID playerId, String questInstanceId) throws SQLException {
        String sql = "DELETE FROM ecocore_quest_data WHERE player_id = ? AND quest_instance_id = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, questInstanceId);
            statement.executeUpdate();
        }
    }
 
    @Override
    public void deleteExpired(UUID playerId, long nowMillis) throws SQLException {
        String sql = "DELETE FROM ecocore_quest_data WHERE player_id = ? AND expires_at_millis <= ? AND completed = ?";
 
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setLong(2, nowMillis);
            statement.setBoolean(3, false);
            statement.executeUpdate();
        }
    }
 
    private QuestData mapRow(ResultSet resultSet) throws SQLException {
        return new QuestData(
            UUID.fromString(resultSet.getString("player_id")),
            resultSet.getString("quest_instance_id"),
            resultSet.getString("quest_definition_id"),
            resultSet.getDouble("progress"),
            resultSet.getDouble("required_progress"),
            resultSet.getBoolean("completed"),
            resultSet.getBoolean("reward_claimed"),
            resultSet.getLong("accepted_at_millis"),
            resultSet.getLong("expires_at_millis")
        );
    }
}
