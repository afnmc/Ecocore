package com.azthera.ecocore.data.repository;
 
import com.azthera.ecocore.data.cache.Cache;
import com.azthera.ecocore.data.cache.InMemoryCache;
import com.azthera.ecocore.data.dao.MarketEventDAO;
import com.azthera.ecocore.data.model.MarketEventRecord;
import com.azthera.ecocore.util.AsyncUtil;
 
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
 
/**
 * Repository for market simulation event state (Boom, Crash, Festival, etc).
 * The set of currently-active events is always small, so it is fully cached
 * by event type to let {@code MarketSimulationEngine} and
 * {@code PricingEngine} check "is event X active right now" synchronously
 * on every price calculation without touching the database.
 */
public final class MarketEventRepository {
 
    private final MarketEventDAO dao;
    private final AsyncUtil asyncUtil;
    private final Logger logger;
    private final Cache<String, MarketEventRecord> activeCache = new InMemoryCache<>();
 
    public MarketEventRepository(MarketEventDAO dao, AsyncUtil asyncUtil, Logger logger) {
        this.dao = dao;
        this.asyncUtil = asyncUtil;
        this.logger = logger;
    }
 
    public Optional<MarketEventRecord> getCachedActive(String eventType) {
        return activeCache.get(eventType);
    }
 
    public Collection<MarketEventRecord> getAllCachedActive() {
        return activeCache.values();
    }
 
    public CompletableFuture<Void> loadActiveIntoCacheAsync() {
        return asyncUtil.runAsync(() -> {
            try {
                List<MarketEventRecord> activeEvents = dao.findAllActive();
                activeCache.invalidateAll();
                activeEvents.forEach(event -> activeCache.put(event.getEventType(), event));
                logger.info(() -> "Loaded " + activeEvents.size() + " active market events into cache.");
            } catch (SQLException exception) {
                logger.severe("Failed to load active market events: " + exception.getMessage());
            }
        });
    }
 
    /**
     * Starts a new event: caches it as active immediately and persists async,
     * returning the generated id via the future once the insert completes.
     */
    public CompletableFuture<Long> startEvent(MarketEventRecord record) {
        activeCache.put(record.getEventType(), record);
        return asyncUtil.supplyAsync(() -> {
            try {
                long id = dao.insert(record);
                record.setActive(true);
                return id;
            } catch (SQLException exception) {
                logger.severe("Failed to persist market event " + record.getEventType() + ": " + exception.getMessage());
                return -1L;
            }
        });
    }
 
    public void endEvent(MarketEventRecord record) {
        record.setActive(false);
        activeCache.invalidate(record.getEventType());
        asyncUtil.runAsync(() -> {
            try {
                dao.update(record);
            } catch (SQLException exception) {
                logger.severe("Failed to persist ended market event " + record.getEventType() + ": " + exception.getMessage());
            }
        });
    }
 
    public CompletableFuture<Void> deactivateExpiredAsync(long nowMillis) {
        return asyncUtil.runAsync(() -> {
            try {
                dao.deactivateExpired(nowMillis);
            } catch (SQLException exception) {
                logger.severe("Failed to deactivate expired market events: " + exception.getMessage());
            }
        });
    }
 
    public CompletableFuture<List<MarketEventRecord>> findRecentHistoryAsync(int limit) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.findRecentHistory(limit);
            } catch (SQLException exception) {
                logger.severe("Failed to load market event history: " + exception.getMessage());
                return List.<MarketEventRecord>of();
            }
        });
    }
}
