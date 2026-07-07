package com.azthera.ecocore.data.repository;
 
import com.azthera.ecocore.data.cache.Cache;
import com.azthera.ecocore.data.cache.InMemoryCache;
import com.azthera.ecocore.data.dao.ShopItemDAO;
import com.azthera.ecocore.data.model.PriceHistoryEntry;
import com.azthera.ecocore.data.model.ShopItemRecord;
import com.azthera.ecocore.util.AsyncUtil;
 
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
 
/**
 * Repository for dynamic shop item pricing/stock state. The entire shop
 * catalog is small enough (hundreds of items at most) to be fully cached in
 * memory at all times, which is what allows {@code PricingEngine} to
 * recalculate prices in real time without hitting the database per-transaction.
 */
public final class ShopItemRepository {
 
    private final ShopItemDAO dao;
    private final AsyncUtil asyncUtil;
    private final Logger logger;
    private final Cache<String, ShopItemRecord> cache = new InMemoryCache<>();
 
    public ShopItemRepository(ShopItemDAO dao, AsyncUtil asyncUtil, Logger logger) {
        this.dao = dao;
        this.asyncUtil = asyncUtil;
        this.logger = logger;
    }
 
    public Optional<ShopItemRecord> getCached(String itemId) {
        return cache.get(itemId);
    }
 
    public Collection<ShopItemRecord> getAllCached() {
        return cache.values();
    }
 
    public CompletableFuture<Void> loadAllIntoCacheAsync() {
        return asyncUtil.runAsync(() -> {
            try {
                List<ShopItemRecord> records = dao.findAll();
                cache.invalidateAll();
                records.forEach(record -> cache.put(record.getItemId(), record));
                logger.info(() -> "Loaded " + records.size() + " shop items into cache.");
            } catch (SQLException exception) {
                logger.severe("Failed to load shop items: " + exception.getMessage());
            }
        });
    }
 
    /**
     * Updates the cache immediately (so the next GUI render or purchase sees
     * the new price instantly) and persists asynchronously.
     */
    public void save(ShopItemRecord record) {
        cache.put(record.getItemId(), record);
        asyncUtil.runAsync(() -> {
            try {
                dao.upsert(record);
            } catch (SQLException exception) {
                logger.severe("Failed to persist shop item " + record.getItemId() + ": " + exception.getMessage());
            }
        });
    }
 
    public CompletableFuture<Void> delete(String itemId) {
        cache.invalidate(itemId);
        return asyncUtil.runAsync(() -> {
            try {
                dao.delete(itemId);
            } catch (SQLException exception) {
                logger.severe("Failed to delete shop item " + itemId + ": " + exception.getMessage());
            }
        });
    }
 
    public void recordPriceHistory(PriceHistoryEntry entry) {
        asyncUtil.runAsync(() -> {
            try {
                dao.insertPriceHistory(entry);
            } catch (SQLException exception) {
                logger.severe("Failed to record price history for " + entry.getItemId() + ": " + exception.getMessage());
            }
        });
    }
 
    public CompletableFuture<List<PriceHistoryEntry>> findPriceHistoryAsync(String itemId, int limit) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.findPriceHistory(itemId, limit);
            } catch (SQLException exception) {
                logger.severe("Failed to load price history for " + itemId + ": " + exception.getMessage());
                return List.<PriceHistoryEntry>of();
            }
        });
    }
 
    public CompletableFuture<Void> purgeOldPriceHistoryAsync(long timestampMillis) {
        return asyncUtil.runAsync(() -> {
            try {
                dao.purgePriceHistoryOlderThan(timestampMillis);
            } catch (SQLException exception) {
                logger.severe("Failed to purge price history: " + exception.getMessage());
            }
        });
    }
}
