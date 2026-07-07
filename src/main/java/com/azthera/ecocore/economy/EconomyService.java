package com.azthera.ecocore.economy;
 
import com.azthera.ecocore.util.Result;
 
import java.util.UUID;
 
/**
 * Central entry point for every balance-affecting operation in EcoCore.
 * Reads are served from an in-memory cache (see {@code PlayerAccountRepository})
 * so calls are synchronous and safe to invoke from GUI click handlers, the
 * Vault bridge, or job/quest reward logic without blocking the main thread;
 * persistence to the database happens asynchronously under the hood.
 */
public interface EconomyService {
 
    String getPrimaryCurrencyId();
 
    /**
     * Ensures the player's accounts for every known currency are loaded into
     * cache, creating them with the configured starting balance if missing.
     * Should be called once when a player joins.
     */
    void ensureAccountLoaded(UUID playerId);
 
    double getBalance(UUID playerId);
 
    double getBalance(UUID playerId, String currencyId);
 
    boolean has(UUID playerId, double amount);
 
    boolean has(UUID playerId, String currencyId, double amount);
 
    Result<Double> deposit(UUID playerId, double amount, TransactionType type, String description);
 
    Result<Double> deposit(UUID playerId, String currencyId, double amount, TransactionType type, String description);
 
    Result<Double> withdraw(UUID playerId, double amount, TransactionType type, String description);
 
    Result<Double> withdraw(UUID playerId, String currencyId, double amount, TransactionType type, String description);
 
    /**
     * Moves money from one player to another in the primary currency.
     * Atomic from the caller's perspective: if the deposit leg fails after a
     * successful withdrawal, the withdrawn amount is refunded automatically.
     */
    Result<Void> transfer(UUID fromPlayerId, UUID toPlayerId, double amount, String description);
}
