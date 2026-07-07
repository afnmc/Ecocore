package com.azthera.ecocore.data.migration;
 
import java.sql.Connection;
import java.sql.SQLException;
 
/**
 * A single versioned schema change. Each implementation represents one
 * migration step (e.g. "V1__InitialSchema") and must be idempotent-safe
 * to run against a fresh database (using CREATE TABLE IF NOT EXISTS style DDL).
 */
public interface MigrationScript {
 
    /**
     * @return the schema version this script upgrades the database TO.
     * Versions must be sequential starting at 1.
     */
    int getVersion();
 
    /**
     * @return short human-readable description, used in logs.
     */
    String getDescription();
 
    /**
     * Applies this migration using the given connection. Implementations
     * should use dialect-agnostic SQL where possible; if dialect-specific
     * syntax is required, branch on {@code dialect}.
     */
    void apply(Connection connection, SqlDialect dialect) throws SQLException;
}
