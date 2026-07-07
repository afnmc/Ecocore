package com.azthera.ecocore.data.dao;
 
import com.azthera.ecocore.data.model.QuestData;
 
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
/**
 * Raw CRUD contract for per-player accepted quest instances
 * (daily, weekly, monthly, story, repeatable, random, chain).
 */
public interface QuestDataDAO {
 
    Optional<QuestData> find(UUID playerId, String questInstanceId) throws SQLException;
 
    List<QuestData> findAllForPlayer(UUID playerId) throws SQLException;
 
    List<QuestData> findActiveForPlayer(UUID playerId, long nowMillis) throws SQLException;
 
    void upsert(QuestData questData) throws SQLException;
 
    void delete(UUID playerId, String questInstanceId) throws SQLException;
 
    /**
     * Deletes all expired, uncompleted quest instances for the given player,
     * called during the daily/weekly/monthly quest rotation reset.
     */
    void deleteExpired(UUID playerId, long nowMillis) throws SQLException;
}
