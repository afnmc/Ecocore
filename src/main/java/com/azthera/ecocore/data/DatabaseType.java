package com.azthera.ecocore.data;
 
/**
 * Supported relational database backends. Each type maps to a specific
 * JDBC driver class and URL format used by {@code DatabaseManager}.
 */
public enum DatabaseType {
 
    SQLITE("org.sqlite.JDBC"),
    MYSQL("com.mysql.cj.jdbc.Driver"),
    MARIADB("org.mariadb.jdbc.Driver");
 
    private final String driverClassName;
 
    DatabaseType(String driverClassName) {
        this.driverClassName = driverClassName;
    }
 
    public String getDriverClassName() {
        return driverClassName;
    }
 
    public static DatabaseType fromString(String value) {
        if (value == null) {
            return SQLITE;
        }
        try {
            return DatabaseType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return SQLITE;
        }
    }
 
    public boolean isFileBased() {
        return this == SQLITE;
    }
}
