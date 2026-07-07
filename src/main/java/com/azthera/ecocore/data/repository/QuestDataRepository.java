package com.azthera.ecocore.data.repository;
 
import com.azthera.ecocore.data.cache.Cache;
import com.azthera.ecocore.data.cache.InMemoryCache;
import com.azthera.ecocore.data.dao.QuestDataDAO;
import com.azthera.ecocore.data.model.QuestData;
import com.azthera.ecocore.util.AsyncUtil;
 
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
 
/**
 * Repository for per-player accepted quest instances. Cached per online
 * player, keyed by "{playerId}:{questInstanceId}", to support fast progress
 * updates as the player performs quest-tracked actions.
 */
public final class QuestDataRepository {
 
    private final QuestDataDAO dao;
    private final AsyncUtil asyncUtil;
    private final Logger logger;
    private final Cache<String, QuestData> cache = new InMemoryCache<>();
 
    public QuestDataRepository(QuestDataDAO dao, AsyncUtil asyncUtil, Logger logger) {
        this.dao = dao;
        this.asyncUtil = asyncUtil;
        this.logger = logger;
    }
 
    private String key(UUID playerId, String questInstanceId) {
        return playerId + ":" + questInstanceId;
    }
 
    public Optional<QuestData> getCached(UUID playerId, String questInstanceId) {
        return cache.get(key(playerId, questInstanceId));
    }
 
    /**
     * @return quests currently cached for this player, filtered client-side.
     * Cache does not support prefix scans, so this relies on the caller having
     * already loaded the player's quests via {@link #loadActiveForPlayerAsync}.
     */
    public List<QuestData> getAllCachedForPlayer(UUID playerId) {
        String prefix = playerId + ":";
        return cache.values().stream()
            .filter(quest -> key(quest.getPlayerId(), quest.getQuestInstanceId()).startsWith(prefix))
            .toList();
    }
 
    public CompletableFuture<List<QuestData>> loadActiveForPlayerAsync(UUID playerId, long nowMillis) {
        return asyncUtil.supplyAsync(() -> {
            try {
                List<QuestData> quests = dao.findActiveForPlayer(playerId, nowMillis);
                quests.forEach(quest -> cache.put(key(playerId, quest.getQuestInstanceId()), quest));
                return quests;
            } catch (SQLException exception) {
                logger.severe("Failed to load active quests for " + playerId + ": " + exception.getMessage());
                return List.<QuestData>of();
            }
        });
    }
 
    public void save(QuestData questData) {
        cache.put(key(questData.getPlayerId(), questData.getQuestInstanceId()), questData);
        asyncUtil.runAsync(() -> {
            try {
                dao.upsert(questData);
            } catch (SQLException exception) {
                logger.severe("Failed to persist quest data for " + questData.getPlayerId() + ": " + exception.getMessage());
            }
        });
    }
 
    public CompletableFuture<Void> delete(UUID playerId, String questInstanceId) {
        cache.invalidate(key(playerId, questInstanceId));
        return asyncUtil.runAsync(() -> {
            try {
                dao.delete(playerId, questInstanceId);
            } catch (SQLException exception) {
                logger.severe("Failed to delete quest data for " + playerId + ": " + exception.getMessage());
            }
        });
    }
 
    public CompletableFuture<Void> deleteExpiredAsync(UUID playerId, long nowMillis) {
        return asyncUtil.runAsync(() -> {
            try {
                dao.deleteExpired(playerId, nowMillis);
            } catch (SQLException exception) {
                logger.severe("Failed to delete expired quests for " + playerId + ": " + exception.getMessage());
            }
        });
    }
 
    public void invalidatePlayer(UUID playerId, List<String> questInstanceIds) {
        for (String questInstanceId : questInstanceIds) {
            cache.invalidate(key(playerId, questInstanceId));
        }
    }
}
