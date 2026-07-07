package com.azthera.ecocore.data.dao;
 
import com.azthera.ecocore.data.model.MinionData;
 
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
/**
 * Raw CRUD contract for placed minion instances, including their location,
 * upgrade levels, fuel, and serialized storage contents.
 */
public interface MinionDAO {
 
    Optional<MinionData> find(UUID minionId) throws SQLException;
 
    List<MinionData> findAllForOwner(UUID ownerId) throws SQLException;
 
    List<MinionData> findAllInWorld(String worldName) throws SQLException;
 
    void insert(MinionData minionData) throws SQLException;
 
    void update(MinionData minionData) throws SQLException;
 
    void delete(UUID minionId) throws SQLException;
 
    long countForOwner(UUID ownerId) throws SQLException;
}
