package com.azthera.ecocore.data.migration;
 
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
 
/**
 * Creates every table EcoCore needs from a completely empty database.
 * Uses CREATE TABLE IF NOT EXISTS everywhere so this script is safe to
 * re-run (SchemaMigrator only runs it once per fresh install, but the
 * safety net costs nothing).
 */
public final class V1__InitialSchema implements MigrationScript {
 
    @Override
    public int getVersion() {
        return 1;
    }
 
    @Override
    public String getDescription() {
        return "Initial schema: accounts, shop items, jobs, quests, minions, logs.";
    }
 
    @Override
    public void apply(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ecocore_player_accounts (
                    player_id VARCHAR(36) NOT NULL,
                    currency_id VARCHAR(32) NOT NULL,
                    balance DOUBLE NOT NULL DEFAULT 0,
                    last_updated_millis BIGINT NOT NULL,
                    PRIMARY KEY (player_id, currency_id)
                )
                """);
 
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ecocore_shop_items (
                    item_id VARCHAR(64) NOT NULL PRIMARY KEY,
                    base_price DOUBLE NOT NULL,
                    current_price DOUBLE NOT NULL,
                    min_price DOUBLE NOT NULL,
                    max_price DOUBLE NOT NULL,
                    elasticity DOUBLE NOT NULL,
                    demand DOUBLE NOT NULL,
                    supply DOUBLE NOT NULL,
                    restock_interval_seconds BIGINT NOT NULL,
                    stock INTEGER NOT NULL,
                    last_restock_millis BIGINT NOT NULL
                )
                """);
 
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ecocore_price_history (
                    id %s,
                    item_id VARCHAR(64) NOT NULL,
                    price DOUBLE NOT NULL,
                    demand DOUBLE NOT NULL,
                    supply DOUBLE NOT NULL,
                    timestamp_millis BIGINT NOT NULL
                )
                """.formatted(dialect.autoIncrementPrimaryKey()));
 
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ecocore_job_data (
                    player_id VARCHAR(36) NOT NULL,
                    job_id VARCHAR(32) NOT NULL,
                    level INTEGER NOT NULL DEFAULT 1,
                    experience DOUBLE NOT NULL DEFAULT 0,
                    prestige_tier INTEGER NOT NULL DEFAULT 0,
                    total_actions_performed BIGINT NOT NULL DEFAULT 0,
                    last_action_millis BIGINT NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_id, job_id)
                )
                """);
 
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ecocore_quest_data (
                    player_id VARCHAR(36) NOT NULL,
                    quest_instance_id VARCHAR(64) NOT NULL,
                    quest_definition_id VARCHAR(64) NOT NULL,
                    progress DOUBLE NOT NULL DEFAULT 0,
                    required_progress DOUBLE NOT NULL DEFAULT 0,
                    completed BOOLEAN NOT NULL DEFAULT FALSE,
                    reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
                    accepted_at_millis BIGINT NOT NULL,
                    expires_at_millis BIGINT NOT NULL,
                    PRIMARY KEY (player_id, quest_instance_id)
                )
                """);
 
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ecocore_minions (
                    minion_id VARCHAR(36) NOT NULL PRIMARY KEY,
                    owner_id VARCHAR(36) NOT NULL,
                    minion_type VARCHAR(32) NOT NULL,
                    world_name VARCHAR(64) NOT NULL,
                    x DOUBLE NOT NULL,
                    y DOUBLE NOT NULL,
                    z DOUBLE NOT NULL,
                    speed_level INTEGER NOT NULL DEFAULT 0,
                    capacity_level INTEGER NOT NULL DEFAULT 0,
                    fortune_level INTEGER NOT NULL DEFAULT 0,
                    fuel_remaining INTEGER NOT NULL DEFAULT 0,
                    serialized_storage %s
                )
                """.formatted(dialect.blobType()));
 
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ecocore_transaction_log (
                    id %s,
                    player_id VARCHAR(36) NOT NULL,
                    currency_id VARCHAR(32) NOT NULL,
                    transaction_type VARCHAR(32) NOT NULL,
                    amount DOUBLE NOT NULL,
                    balance_after DOUBLE NOT NULL,
                    description %s,
                    timestamp_millis BIGINT NOT NULL
                )
                """.formatted(dialect.autoIncrementPrimaryKey(), dialect.textType()));
 
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ecocore_market_events (
                    id %s,
                    event_type VARCHAR(32) NOT NULL,
                    started_at_millis BIGINT NOT NULL,
                    ends_at_millis BIGINT NOT NULL,
                    price_modifier DOUBLE NOT NULL,
                    demand_modifier DOUBLE NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE
                )
                """.formatted(dialect.autoIncrementPrimaryKey()));
 
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ecocore_schema_version (
                    version INTEGER NOT NULL
                )
                """);
 
            statement.execute(
                "CREATE INDEX IF NOT EXISTS idx_price_history_item ON ecocore_price_history (item_id, timestamp_millis)"
            );
            statement.execute(
                "CREATE INDEX IF NOT EXISTS idx_transaction_log_player ON ecocore_transaction_log (player_id, timestamp_millis)"
            );
            statement.execute(
                "CREATE INDEX IF NOT EXISTS idx_minions_owner ON ecocore_minions (owner_id)"
            );
        }
    }
}
