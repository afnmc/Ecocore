package com.azthera.ecocore.data.dao;
 
import com.azthera.ecocore.data.model.TransactionLogEntry;
 
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
 
/**
 * Raw insert/query contract for the immutable transaction ledger backing
 * the Transaction Log, Economy Log, and Audit Log admin views.
 */
public interface TransactionLogDAO {
 
    /**
     * Inserts a new entry and returns the generated id.
     */
    long insert(TransactionLogEntry entry) throws SQLException;
 
    List<TransactionLogEntry> findForPlayer(UUID playerId, int limit, int offset) throws SQLException;
 
    List<TransactionLogEntry> findByType(String transactionType, long fromMillis, long toMillis, int limit) throws SQLException;
 
    List<TransactionLogEntry> findRecent(int limit) throws SQLException;
 
    /**
     * @return total sum of amounts created (positive entries) minus removed (negative
     * entries) for a currency within a time range, used by MoneySupplyTracker.
     */
    double sumNetAmountForCurrency(String currencyId, long fromMillis, long toMillis) throws SQLException;
 
    void purgeOlderThan(long timestampMillis) throws SQLException;
}
