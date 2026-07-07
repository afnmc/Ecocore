package com.azthera.ecocore.data.repository;
 
import com.azthera.ecocore.data.cache.Cache;
import com.azthera.ecocore.data.cache.InMemoryCache;
import com.azthera.ecocore.data.dao.MinionDAO;
import com.azthera.ecocore.data.model.MinionData;
import com.azthera.ecocore.util.AsyncUtil;
 
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
 
/**
 * Repository for placed minion instances. Cached by minionId; owner-scoped
 * lookups are served from cache when possible (filtering cached values) to
 * avoid a database round-trip every time a player opens their minion GUI,
 * falling back to the database on cache miss (e.g. right after server start).
 */
public final class MinionRepository {
 
    private final MinionDAO dao;
    private final AsyncUtil asyncUtil;
    private final Logger logger;
    private final Cache<String, MinionData> cache = new InMemoryCache<>();
 
    public MinionRepository(MinionDAO dao, AsyncUtil asyncUtil, Logger logger) {
        this.dao = dao;
        this.asyncUtil = asyncUtil;
        this.logger = logger;
    }
 
    public Optional<MinionData> getCached(UUID minionId) {
        return cache.get(minionId.toString());
    }
 
    public List<MinionData> getAllCachedForOwner(UUID ownerId) {
        return cache.values().stream()
            .filter(minion -> minion.getOwnerId().equals(ownerId))
            .toList();
    }
 
    public CompletableFuture<List<MinionData>> loadForOwnerAsync(UUID ownerId) {
        return asyncUtil.supplyAsync(() -> {
            try {
                List<MinionData> minions = dao.findAllForOwner(ownerId);
                minions.forEach(minion -> cache.put(minion.getMinionId().toString(), minion));
                return minions;
            } catch (SQLException exception) {
                logger.severe("Failed to load minions for owner " + ownerId + ": " + exception.getMessage());
                return List.<MinionData>of();
            }
        });
    }
 
    public CompletableFuture<List<MinionData>> loadForWorldAsync(String worldName) {
        return asyncUtil.supplyAsync(() -> {
            try {
                List<MinionData> minions = dao.findAllInWorld(worldName);
                minions.forEach(minion -> cache.put(minion.getMinionId().toString(), minion));
                return minions;
            } catch (SQLException exception) {
                logger.severe("Failed to load minions for world " + worldName + ": " + exception.getMessage());
                return List.<MinionData>of();
            }
        });
    }
 
    public CompletableFuture<Void> insert(MinionData minionData) {
        cache.put(minionData.getMinionId().toString(), minionData);
        return asyncUtil.runAsync(() -> {
            try {
                dao.insert(minionData);
            } catch (SQLException exception) {
                logger.severe("Failed to insert minion " + minionData.getMinionId() + ": " + exception.getMessage());
            }
        });
    }
 
    public void save(MinionData minionData) {
        cache.put(minionData.getMinionId().toString(), minionData);
        asyncUtil.runAsync(() -> {
            try {
                dao.update(minionData);
            } catch (SQLException exception) {
                logger.severe("Failed to update minion " + minionData.getMinionId() + ": " + exception.getMessage());
            }
        });
    }
 
    public CompletableFuture<Void> delete(UUID minionId) {
        cache.invalidate(minionId.toString());
        return asyncUtil.runAsync(() -> {
            try {
                dao.delete(minionId);
            } catch (SQLException exception) {
                logger.severe("Failed to delete minion " + minionId + ": " + exception.getMessage());
            }
        });
    }
 
    public CompletableFuture<Long> countForOwnerAsync(UUID ownerId) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.countForOwner(ownerId);
            } catch (SQLException exception) {
                logger.severe("Failed to count minions for owner " + ownerId + ": " + exception.getMessage());
                return 0L;
            }
        });
    }
 
    /**
     * Fast synchronous count from cache, used by {@code MinionLimitPolicy} to
     * reject placement immediately without waiting on an async round-trip.
     * Assumes the owner's minions have already been loaded via
     * {@link #loadForOwnerAsync} at join time.
     */
    public int countCachedForOwner(UUID ownerId) {
        return getAllCachedForOwner(ownerId).size();
    }
}
