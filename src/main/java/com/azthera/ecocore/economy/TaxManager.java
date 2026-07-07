package com.azthera.ecocore.economy;
 
import com.azthera.ecocore.config.EconomyConfig;
 
import java.util.Set;
 
/**
 * Decides which transaction types are subject to tax and computes the tax
 * amount owed on a given gross amount. Only income-style transactions
 * (income earned by the player, not player-to-player transfers or admin
 * grants) are taxable, matching a non-pay-to-win, purely sink-based design.
 */
public final class TaxManager {
 
    private static final Set<TransactionType> TAXABLE_TYPES = Set.of(
        TransactionType.SHOP_SELL,
        TransactionType.AUTO_SELL,
        TransactionType.JOB_REWARD,
        TransactionType.QUEST_REWARD,
        TransactionType.MINION_SELL
    );
 
    private EconomyConfig economyConfig;
 
    public TaxManager(EconomyConfig economyConfig) {
        this.economyConfig = economyConfig;
    }
 
    public void reload(EconomyConfig economyConfig) {
        this.economyConfig = economyConfig;
    }
 
    public boolean isTaxable(TransactionType type) {
        return economyConfig.isTaxEnabled() && TAXABLE_TYPES.contains(type);
    }
 
    /**
     * @param grossAmount the amount before tax
     * @return the tax amount to deduct; 0 if tax is disabled or the type is not taxable.
     */
    public double computeTax(double grossAmount) {
        if (!economyConfig.isTaxEnabled() || grossAmount <= 0) {
            return 0.0;
        }
        return grossAmount * economyConfig.getTaxRate();
    }
 
    public double getTaxRate() {
        return economyConfig.getTaxRate();
    }
}
