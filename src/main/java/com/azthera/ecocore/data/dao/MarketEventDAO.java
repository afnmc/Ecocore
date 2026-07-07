package com.azthera.ecocore.data.dao;
 
import com.azthera.ecocore.data.model.MarketEventRecord;
 
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
 
/**
 * Raw CRUD contract for market simulation event state (Boom, Crash, Festival,
 * Mining/Farming/Fishing Week, etc), so an in-progress event survives a
 * server restart.
 */
public interface MarketEventDAO {
 
    Optional<MarketEventRecord> findActiveByType(String eventType) throws SQLException;
 
    List<MarketEventRecord> findAllActive() throws SQLException;
 
    List<MarketEventRecord> findRecentHistory(int limit) throws SQLException;
 
    /**
     * Inserts a new event record and returns the generated id.
     */
    long insert(MarketEventRecord record) throws SQLException;
 
    void update(MarketEventRecord record) throws SQLException;
 
    void deactivateExpired(long nowMillis) throws SQLException;
}
