package com.azthera.ecocore.data.model;
 
import java.util.UUID;
 
/**
 * Immutable audit record of a single economic transaction (shop purchase,
 * sell, job reward, quest reward, tax, sink, admin give, etc). Written once,
 * never updated — this is the ledger backing the Transaction Log and Audit Log.
 */
public final class TransactionLogEntry {
 
    private final long id;
    private final UUID playerId;
    private final String currencyId;
    private final String transactionType;
    private final double amount;
    private final double balanceAfter;
    private final String description;
    private final long timestampMillis;
 
    public TransactionLogEntry(long id, UUID playerId, String currencyId, String transactionType, double amount,
                                double balanceAfter, String description, long timestampMillis) {
        this.id = id;
        this.playerId = playerId;
        this.currencyId = currencyId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestampMillis = timestampMillis;
    }
 
    public long getId() {
        return id;
    }
 
    public UUID getPlayerId() {
        return playerId;
    }
 
    public String getCurrencyId() {
        return currencyId;
    }
 
    public String getTransactionType() {
        return transactionType;
    }
 
    public double getAmount() {
        return amount;
    }
 
    public double getBalanceAfter() {
        return balanceAfter;
    }
 
    public String getDescription() {
        return description;
    }
 
    public long getTimestampMillis() {
        return timestampMillis;
    }
}
