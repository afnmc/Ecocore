package com.azthera.ecocore.data;
 
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
 
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
 
/**
 * Owns the HikariCP connection pool for whichever backend (SQLite, MySQL, MariaDB)
 * the server owner selected in config.yml. All DAO implementations obtain their
 * connections exclusively through this class — no other class should construct
 * a JDBC connection directly.
 */
public final class DatabaseManager {
 
    private final Plugin plugin;
    private final Logger logger;
    private HikariDataSource dataSource;
    private DatabaseType databaseType;
 
    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
 
    public void connect(YamlConfiguration mainConfig) {
        this.databaseType = DatabaseType.fromString(mainConfig.getString("database.type", "SQLITE"));
 
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("EcoCore-Pool");
        hikariConfig.setMaximumPoolSize(mainConfig.getInt("database.pool.maximum-pool-size", 10));
        hikariConfig.setMinimumIdle(mainConfig.getInt("database.pool.minimum-idle", 2));
        hikariConfig.setConnectionTimeout(mainConfig.getLong("database.pool.connection-timeout-ms", 30000L));
 
        switch (databaseType) {
            case SQLITE -> configureSqlite(hikariConfig, mainConfig);
            case MYSQL -> configureMysql(hikariConfig, mainConfig);
            case MARIADB -> configureMariadb(hikariConfig, mainConfig);
        }
 
        this.dataSource = new HikariDataSource(hikariConfig);
        logger.info(() -> "Connected to database using " + databaseType + " backend.");
    }
 
    private void configureSqlite(HikariConfig hikariConfig, YamlConfiguration mainConfig) {
        String fileName = mainConfig.getString("database.sqlite.file", "ecocore.db");
        File databaseFile = new File(plugin.getDataFolder(), fileName);
        hikariConfig.setDriverClassName(DatabaseType.SQLITE.getDriverClassName());
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.addDataSourceProperty("journal_mode", "WAL");
        hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
    }
 
    private void configureMysql(HikariConfig hikariConfig, YamlConfiguration mainConfig) {
        String host = mainConfig.getString("database.mysql.host", "localhost");
        int port = mainConfig.getInt("database.mysql.port", 3306);
        String database = mainConfig.getString("database.mysql.database", "ecocore");
        boolean useSsl = mainConfig.getBoolean("database.mysql.use-ssl", false);
 
        hikariConfig.setDriverClassName(DatabaseType.MYSQL.getDriverClassName());
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
            + "?useSSL=" + useSsl + "&autoReconnect=true&characterEncoding=utf8");
        hikariConfig.setUsername(mainConfig.getString("database.mysql.username", "root"));
        hikariConfig.setPassword(mainConfig.getString("database.mysql.password", ""));
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    }
 
    private void configureMariadb(HikariConfig hikariConfig, YamlConfiguration mainConfig) {
        String host = mainConfig.getString("database.mariadb.host", "localhost");
        int port = mainConfig.getInt("database.mariadb.port", 3306);
        String database = mainConfig.getString("database.mariadb.database", "ecocore");
        boolean useSsl = mainConfig.getBoolean("database.mariadb.use-ssl", false);
 
        hikariConfig.setDriverClassName(DatabaseType.MARIADB.getDriverClassName());
        hikariConfig.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database
            + "?useSSL=" + useSsl + "&autoReconnect=true&characterEncoding=utf8");
        hikariConfig.setUsername(mainConfig.getString("database.mariadb.username", "root"));
        hikariConfig.setPassword(mainConfig.getString("database.mariadb.password", ""));
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    }
 
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseManager has not been connected yet.");
        }
        return dataSource.getConnection();
    }
 
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
 
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed.");
        }
    }
 
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
