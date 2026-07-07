package com.azthera.ecocore.data.migration;
 
import com.azthera.ecocore.data.DatabaseManager;
 
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;
 
/**
 * Applies pending {@link MigrationScript} implementations in version order,
 * tracking the current schema version in the {@code ecocore_schema_version}
 * table so migrations only ever run once against a given database.
 */
public final class SchemaMigrator {
 
    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final List<MigrationScript> migrationScripts;
 
    public SchemaMigrator(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
        this.migrationScripts = List.of(
            new V1__InitialSchema()
        );
    }
 
    public void migrate() {
        SqlDialect dialect = new SqlDialect(databaseManager.getDatabaseType());
 
        try (Connection connection = databaseManager.getConnection()) {
            int currentVersion = readCurrentVersion(connection);
 
            for (MigrationScript script : migrationScripts) {
                if (script.getVersion() > currentVersion) {
                    logger.info(() -> "Applying migration V" + script.getVersion() + ": " + script.getDescription());
                    script.apply(connection, dialect);
                    writeVersion(connection, script.getVersion());
                    currentVersion = script.getVersion();
                }
            }
        } catch (SQLException exception) {
            logger.severe("Database migration failed: " + exception.getMessage());
            throw new RuntimeException("Failed to migrate database schema", exception);
        }
    }
 
    private int readCurrentVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS ecocore_schema_version (version INTEGER NOT NULL)");
 
            try (ResultSet resultSet = statement.executeQuery("SELECT version FROM ecocore_schema_version LIMIT 1")) {
                if (resultSet.next()) {
                    return resultSet.getInt("version");
                }
            }
        }
        return 0;
    }
 
    private void writeVersion(Connection connection, int version) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM ecocore_schema_version");
            statement.execute("INSERT INTO ecocore_schema_version (version) VALUES (" + version + ")");
        }
    }
}
