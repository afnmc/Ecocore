package com.azthera.ecocore.data.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Adds the {@code joined} column to {@code ecocore_job_data}, required by
 * the job join/leave (GUI) system: a player only earns job XP/money while
 * this flag is true. Uses JDBC's dialect-agnostic {@link DatabaseMetaData}
 * to check column existence first, since SQLite/MySQL/MariaDB don't share
 * a common "ADD COLUMN IF NOT EXISTS" syntax.
 */
public final class V2__AddJobJoinedColumn implements MigrationScript {

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public String getDescription() {
        return "Add 'joined' column to ecocore_job_data for the job join/leave (GUI) system.";
    }

    @Override
    public void apply(Connection connection, SqlDialect dialect) throws SQLException {
        if (columnExists(connection, "ecocore_job_data", "joined")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE ecocore_job_data ADD COLUMN joined BOOLEAN NOT NULL DEFAULT FALSE");
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
