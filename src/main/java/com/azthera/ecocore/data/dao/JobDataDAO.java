package com.azthera.ecocore.data.dao;
 
import com.azthera.ecocore.data.model.JobData;
 
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
/**
 * Raw CRUD contract for per-player, per-job progress records
 * (level, experience, prestige tier, lifetime action count).
 */
public interface JobDataDAO {
 
    Optional<JobData> find(UUID playerId, String jobId) throws SQLException;
 
    List<JobData> findAllForPlayer(UUID playerId) throws SQLException;
 
    void upsert(JobData jobData) throws SQLException;
 
    void delete(UUID playerId, String jobId) throws SQLException;
 
    /**
     * @return top N players for a given job ordered by level then experience descending,
     * used for job leaderboards.
     */
    List<JobData> findTopByJob(String jobId, int limit) throws SQLException;
}
