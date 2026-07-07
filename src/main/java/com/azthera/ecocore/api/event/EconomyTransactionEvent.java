package com.azthera.ecocore.api.event;
 
import com.azthera.ecocore.economy.TransactionType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
 
import java.util.UUID;
 
/**
 * Fired after every successful economy transaction (deposit, withdraw,
 * transfer leg, shop buy/sell, job/quest reward, tax, admin give/take).
 * This is a monitor-only notification event — it is not cancellable,
 * since the transaction has already been applied and persisted by the time
 * this fires. Other plugins can listen to this for cross-plugin economy
 * hooks (e.g. Discord webhooks, custom leaderboards) without depending on
 * EcoCore's internals directly.
 */
public final class EconomyTransactionEvent extends Event {
 
    private static final HandlerList HANDLERS = new HandlerList();
 
    private final UUID playerId;
    private final String currencyId;
    private final TransactionType transactionType;
    private final double amount;
    private final double balanceAfter;
    private final String description;
 
    public EconomyTransactionEvent(UUID playerId, String currencyId, TransactionType transactionType,
                                    double amount, double balanceAfter, String description) {
        this.playerId = playerId;
        this.currencyId = currencyId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }
 
    public UUID getPlayerId() {
        return playerId;
    }
 
    public String getCurrencyId() {
        return currencyId;
    }
 
    public TransactionType getTransactionType() {
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
 
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
 
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
