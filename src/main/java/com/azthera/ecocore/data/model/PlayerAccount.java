package com.azthera.ecocore.data.model;
 
import java.util.UUID;
 
/**
 * Represents a single player's balance record for one currency.
 * Multi-currency support is achieved by having one row per (player, currencyId) pair.
 */
public final class PlayerAccount {
 
    private final UUID playerId;
    private final String currencyId;
    private double balance;
    private long lastUpdatedMillis;
 
    public PlayerAccount(UUID playerId, String currencyId, double balance, long lastUpdatedMillis) {
        this.playerId = playerId;
        this.currencyId = currencyId;
        this.balance = balance;
        this.lastUpdatedMillis = lastUpdatedMillis;
    }
 
    public UUID getPlayerId() {
        return playerId;
    }
 
    public String getCurrencyId() {
        return currencyId;
    }
 
    public double getBalance() {
        return balance;
    }
 
    public void setBalance(double balance) {
        this.balance = balance;
    }
 
    public long getLastUpdatedMillis() {
        return lastUpdatedMillis;
    }
 
    public void setLastUpdatedMillis(long lastUpdatedMillis) {
        this.lastUpdatedMillis = lastUpdatedMillis;
    }
}
