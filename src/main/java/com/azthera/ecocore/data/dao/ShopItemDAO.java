package com.azthera.ecocore.data.dao;
 
import com.azthera.ecocore.data.model.PriceHistoryEntry;
import com.azthera.ecocore.data.model.ShopItemRecord;
 
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
 
/**
 * Raw CRUD contract for dynamic shop item pricing/stock state, plus the
 * append-only price history table used by the Admin Editor's trend view.
 */
public interface ShopItemDAO {
 
    Optional<ShopItemRecord> find(String itemId) throws SQLException;
 
    List<ShopItemRecord> findAll() throws SQLException;
 
    void upsert(ShopItemRecord record) throws SQLException;
 
    void delete(String itemId) throws SQLException;
 
    void insertPriceHistory(PriceHistoryEntry entry) throws SQLException;
 
    List<PriceHistoryEntry> findPriceHistory(String itemId, int limit) throws SQLException;
 
    /**
     * Deletes price history entries older than the given timestamp, used by a
     * periodic cleanup task to keep the table from growing unbounded.
     */
    void purgePriceHistoryOlderThan(long timestampMillis) throws SQLException;
}
