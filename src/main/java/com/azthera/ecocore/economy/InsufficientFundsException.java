package com.azthera.ecocore.economy;
 
import java.util.UUID;
 
/**
 * Thrown (and wrapped inside a {@code Result.Failure}) when a withdrawal is
 * attempted for more than the player's current balance in the given currency.
 */
public final class InsufficientFundsException extends RuntimeException {
 
    private final UUID playerId;
    private final String currencyId;
    private final double requestedAmount;
    private final double availableBalance;
 
    public InsufficientFundsException(UUID playerId, String currencyId, double requestedAmount, double availableBalance) {
        super("Player " + playerId + " has insufficient funds in currency '" + currencyId + "': requested "
            + requestedAmount + " but only " + availableBalance + " available.");
        this.playerId = playerId;
        this.currencyId = currencyId;
        this.requestedAmount = requestedAmount;
        this.availableBalance = availableBalance;
    }
 
    public UUID getPlayerId() {
        return playerId;
    }
 
    public String getCurrencyId() {
        return currencyId;
    }
 
    public double getRequestedAmount() {
        return requestedAmount;
    }
 
    public double getAvailableBalance() {
        return availableBalance;
    }
}
