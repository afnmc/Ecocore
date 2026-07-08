package com.azthera.ecocore.data.dao.impl;

import com.azthera.ecocore.data.DatabaseManager;
import com.azthera.ecocore.data.dao.JobDataDAO;
import com.azthera.ecocore.data.migration.SqlDialect;
import com.azthera.ecocore.data.model.JobData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of {@link JobDataDAO} backed by the {@code ecocore_job_data} table.
 */
public final class SqlJobDataDAO implements JobDataDAO {

    private final DatabaseManager databaseManager;
    private final SqlDialect dialect;

    public SqlJobDataDAO(DatabaseManager databaseManager, SqlDialect dialect) {
        this.databaseManager = databaseManager;
        this.dialect = dialect;
    }

    @Override
    public Optional<JobData> find(UUID playerId, String jobId) throws SQLException {
        String sql = "SELECT player_id, job_id, level, experience, prestige_tier, total_actions_performed, " +
            "last_action_millis, joined FROM ecocore_job_data WHERE player_id = ? AND job_id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, jobId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<JobData> findAllForPlayer(UUID playerId) throws SQLException {
        String sql = "SELECT player_id, job_id, level, experience, prestige_tier, total_actions_performed, " +
            "last_action_millis, joined FROM ecocore_job_data WHERE player_id = ?";

        List<JobData> results = new ArrayList<>();
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
    public void upsert(JobData jobData) throws SQLException {
        String sql = switch (dialect.getDatabaseType()) {
            case SQLITE -> "INSERT INTO ecocore_job_data (player_id, job_id, level, experience, prestige_tier, " +
                "total_actions_performed, last_action_millis, joined) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_id, job_id) DO UPDATE SET level = excluded.level, experience = excluded.experience, " +
                "prestige_tier = excluded.prestige_tier, total_actions_performed = excluded.total_actions_performed, " +
                "last_action_millis = excluded.last_action_millis, joined = excluded.joined";
            case MYSQL, MARIADB -> "INSERT INTO ecocore_job_data (player_id, job_id, level, experience, prestige_tier, " +
                "total_actions_performed, last_action_millis, joined) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE level = VALUES(level), experience = VALUES(experience), " +
                "prestige_tier = VALUES(prestige_tier), total_actions_performed = VALUES(total_actions_performed), " +
                "last_action_millis = VALUES(last_action_millis), joined = VALUES(joined)";
        };

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, jobData.getPlayerId().toString());
            statement.setString(2, jobData.getJobId());
            statement.setInt(3, jobData.getLevel());
            statement.setDouble(4, jobData.getExperience());
            statement.setInt(5, jobData.getPrestigeTier());
            statement.setLong(6, jobData.getTotalActionsPerformed());
            statement.setLong(7, jobData.getLastActionMillis());
            statement.setBoolean(8, jobData.isJoined());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(UUID playerId, String jobId) throws SQLException {
        String sql = "DELETE FROM ecocore_job_data WHERE player_id = ? AND job_id = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, jobId);
            statement.executeUpdate();
        }
    }

    @Override
    public List<JobData> findTopByJob(String jobId, int limit) throws SQLException {
        String sql = "SELECT player_id, job_id, level, experience, prestige_tier, total_actions_performed, " +
            "last_action_millis, joined FROM ecocore_job_data WHERE job_id = ? " +
            "ORDER BY level DESC, experience DESC LIMIT ?";

        List<JobData> results = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, jobId);
            statement.setInt(2, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
        }
        return results;
    }

    private JobData mapRow(ResultSet resultSet) throws SQLException {
        return new JobData(
            UUID.fromString(resultSet.getString("player_id")),
            resultSet.getString("job_id"),
            resultSet.getInt("level"),
            resultSet.getDouble("experience"),
            resultSet.getInt("prestige_tier"),
            resultSet.getLong("total_actions_performed"),
            resultSet.getLong("last_action_millis"),
            resultSet.getBoolean("joined")
        );
    }
    }
