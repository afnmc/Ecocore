package com.azthera.ecocore.economy;
 
import com.azthera.ecocore.config.EconomyConfig;
 
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
 
/**
 * Tracks money permanently removed from the economy (tax collected, NPC shop
 * purchase payments, admin removals) rather than transferred between
 * players. This running total feeds {@code MoneySupplyTracker} and the
 * {@code EconomyStatisticsService} so admins can see how much the sink
 * mechanisms are counteracting money creation from job/quest rewards.
 */
public final class MoneySinkManager {
 
    private final Logger logger;
    private final ConcurrentHashMap<String, Double> cumulativeSinkByCurrency = new ConcurrentHashMap<>();
    private EconomyConfig economyConfig;
 
    public MoneySinkManager(EconomyConfig economyConfig, Logger logger) {
        this.economyConfig = economyConfig;
        this.logger = logger;
    }
 
    public void reload(EconomyConfig economyConfig) {
        this.economyConfig = economyConfig;
    }
 
    /**
     * Records that {@code amount} of the given currency has been permanently
     * removed from the economy. Does not touch any player's balance directly —
     * callers are responsible for having already deducted the amount from
     * wherever it came from; this method exists purely for tracking.
     */
    public void recordSink(String currencyId, double amount, String reason) {
        if (amount <= 0) {
            return;
        }
        cumulativeSinkByCurrency.merge(currencyId, amount, Double::sum);
        logger.fine(() -> "Money sink recorded: " + amount + " " + currencyId + " (" + reason + ")");
    }
 
    public double getCumulativeSink(String currencyId) {
        return cumulativeSinkByCurrency.getOrDefault(currencyId, 0.0);
    }
 
    public double getSinkAlertThreshold() {
        return economyConfig.getMoneySinkThreshold();
    }
 
    public void resetCumulativeSink(String currencyId) {
        cumulativeSinkByCurrency.remove(currencyId);
    }
}
