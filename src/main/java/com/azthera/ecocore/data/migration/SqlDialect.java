package com.azthera.ecocore.data.migration;
 
import com.azthera.ecocore.data.DatabaseType;
 
/**
 * Provides dialect-specific SQL fragments so migration scripts and DAOs can
 * stay mostly dialect-agnostic while still using correct syntax for
 * auto-increment primary keys and upsert statements across SQLite, MySQL,
 * and MariaDB.
 */
public final class SqlDialect {
 
    private final DatabaseType databaseType;
 
    public SqlDialect(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }
 
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
 
    /**
     * @return the correct DDL fragment for an auto-incrementing primary key column.
     */
    public String autoIncrementPrimaryKey() {
        return switch (databaseType) {
            case SQLITE -> "INTEGER PRIMARY KEY AUTOINCREMENT";
            case MYSQL, MARIADB -> "BIGINT AUTO_INCREMENT PRIMARY KEY";
        };
    }
 
    /**
     * @return the correct type for storing large binary blobs (minion storage, etc).
     */
    public String blobType() {
        return switch (databaseType) {
            case SQLITE -> "BLOB";
            case MYSQL, MARIADB -> "LONGBLOB";
        };
    }
 
    /**
     * @return the correct type for arbitrary-length text.
     */
    public String textType() {
        return switch (databaseType) {
            case SQLITE -> "TEXT";
            case MYSQL, MARIADB -> "TEXT";
        };
    }
 
    /**
     * @return the correct "INSERT ... ON CONFLICT/DUPLICATE KEY UPDATE" clause opener,
     * appended after the base INSERT statement's VALUES clause by the caller.
     */
    public String upsertClauseKeyword() {
        return switch (databaseType) {
            case SQLITE -> "ON CONFLICT";
            case MYSQL, MARIADB -> "ON DUPLICATE KEY UPDATE";
        };
    }
 
    public boolean isSqlite() {
        return databaseType == DatabaseType.SQLITE;
    }
}
