package com.azthera.ecocore.data.repository;
 
import com.azthera.ecocore.data.dao.TransactionLogDAO;
import com.azthera.ecocore.data.model.TransactionLogEntry;
import com.azthera.ecocore.util.AsyncUtil;
 
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
 
/**
 * Repository for the immutable transaction ledger. Deliberately uncached
 * (append-only, write-heavy, rarely re-read in bulk) — every call goes
 * straight through to {@link TransactionLogDAO} asynchronously. This is the
 * backing store for the Transaction Log, Economy Log, and Audit Log.
 */
public final class TransactionLogRepository {
 
    private final TransactionLogDAO dao;
    private final AsyncUtil asyncUtil;
    private final Logger logger;
 
    public TransactionLogRepository(TransactionLogDAO dao, AsyncUtil asyncUtil, Logger logger) {
        this.dao = dao;
        this.asyncUtil = asyncUtil;
        this.logger = logger;
    }
 
    public CompletableFuture<Long> log(TransactionLogEntry entry) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.insert(entry);
            } catch (SQLException exception) {
                logger.severe("Failed to write transaction log entry for " + entry.getPlayerId() + ": " + exception.getMessage());
                return -1L;
            }
        });
    }
 
    public CompletableFuture<List<TransactionLogEntry>> findForPlayerAsync(UUID playerId, int limit, int offset) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.findForPlayer(playerId, limit, offset);
            } catch (SQLException exception) {
                logger.severe("Failed to load transactions for " + playerId + ": " + exception.getMessage());
                return List.<TransactionLogEntry>of();
            }
        });
    }
 
    public CompletableFuture<List<TransactionLogEntry>> findByTypeAsync(String transactionType, long fromMillis,
                                                                          long toMillis, int limit) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.findByType(transactionType, fromMillis, toMillis, limit);
            } catch (SQLException exception) {
                logger.severe("Failed to load transactions of type " + transactionType + ": " + exception.getMessage());
                return List.<TransactionLogEntry>of();
            }
        });
    }
 
    public CompletableFuture<List<TransactionLogEntry>> findRecentAsync(int limit) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.findRecent(limit);
            } catch (SQLException exception) {
                logger.severe("Failed to load recent transactions: " + exception.getMessage());
                return List.<TransactionLogEntry>of();
            }
        });
    }
 
    public CompletableFuture<Double> sumNetAmountAsync(String currencyId, long fromMillis, long toMillis) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.sumNetAmountForCurrency(currencyId, fromMillis, toMillis);
            } catch (SQLException exception) {
                logger.severe("Failed to sum net amount for " + currencyId + ": " + exception.getMessage());
                return 0.0;
            }
        });
    }
 
    public CompletableFuture<Void> purgeOlderThanAsync(long timestampMillis) {
        return asyncUtil.runAsync(() -> {
            try {
                dao.purgeOlderThan(timestampMillis);
            } catch (SQLException exception) {
                logger.severe("Failed to purge old transaction log entries: " + exception.getMessage());
            }
        });
    }
}
