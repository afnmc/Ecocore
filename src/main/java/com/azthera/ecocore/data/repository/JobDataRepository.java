package com.azthera.ecocore.data.repository;
 
import com.azthera.ecocore.data.cache.Cache;
import com.azthera.ecocore.data.cache.InMemoryCache;
import com.azthera.ecocore.data.dao.JobDataDAO;
import com.azthera.ecocore.data.model.JobData;
import com.azthera.ecocore.util.AsyncUtil;
 
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
 
/**
 * Repository for per-player job progress (level, XP, prestige). Cached per
 * online player so XP gain on every fish/block/kill does not hit the database
 * synchronously; persistence happens async and on player quit.
 */
public final class JobDataRepository {
 
    private final JobDataDAO dao;
    private final AsyncUtil asyncUtil;
    private final Logger logger;
    private final Cache<String, JobData> cache = new InMemoryCache<>();
 
    public JobDataRepository(JobDataDAO dao, AsyncUtil asyncUtil, Logger logger) {
        this.dao = dao;
        this.asyncUtil = asyncUtil;
        this.logger = logger;
    }
 
    private String key(UUID playerId, String jobId) {
        return playerId + ":" + jobId;
    }
 
    public Optional<JobData> getCached(UUID playerId, String jobId) {
        return cache.get(key(playerId, jobId));
    }
 
    public CompletableFuture<List<JobData>> loadForPlayerAsync(UUID playerId) {
        return asyncUtil.supplyAsync(() -> {
            try {
                List<JobData> jobDataList = dao.findAllForPlayer(playerId);
                jobDataList.forEach(data -> cache.put(key(playerId, data.getJobId()), data));
                return jobDataList;
            } catch (SQLException exception) {
                logger.severe("Failed to load job data for " + playerId + ": " + exception.getMessage());
                return List.<JobData>of();
            }
        });
    }
 
    public void save(JobData jobData) {
        cache.put(key(jobData.getPlayerId(), jobData.getJobId()), jobData);
        asyncUtil.runAsync(() -> {
            try {
                dao.upsert(jobData);
            } catch (SQLException exception) {
                logger.severe("Failed to persist job data for " + jobData.getPlayerId() + ": " + exception.getMessage());
            }
        });
    }
 
    public CompletableFuture<Void> delete(UUID playerId, String jobId) {
        cache.invalidate(key(playerId, jobId));
        return asyncUtil.runAsync(() -> {
            try {
                dao.delete(playerId, jobId);
            } catch (SQLException exception) {
                logger.severe("Failed to delete job data for " + playerId + ": " + exception.getMessage());
            }
        });
    }
 
    public void invalidatePlayer(UUID playerId, List<String> jobIds) {
        for (String jobId : jobIds) {
            cache.invalidate(key(playerId, jobId));
        }
    }
 
    public CompletableFuture<List<JobData>> findTopByJobAsync(String jobId, int limit) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.findTopByJob(jobId, limit);
            } catch (SQLException exception) {
                logger.severe("Failed to load job leaderboard for " + jobId + ": " + exception.getMessage());
                return List.<JobData>of();
            }
        });
    }
}
