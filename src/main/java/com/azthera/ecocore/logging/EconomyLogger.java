package com.azthera.ecocore.logging;
 
import java.util.logging.Logger;
 
/**
 * Logs economy-wide macro events (inflation cycles, market event
 * start/end, money supply anomalies) distinct from per-player transaction
 * logging. Intended for server owners tailing the console/log file to spot
 * economic trends without cross-referencing thousands of individual
 * transaction rows.
 */
public final class EconomyLogger {
 
    private final Logger logger;
 
    public EconomyLogger(Logger logger) {
        this.logger = logger;
    }
 
    public void logInflationCycle(double effectiveRate, String state) {
        logger.info(() -> "[Economy] Inflation cycle: rate=" + String.format("%.4f", effectiveRate) + " state=" + state);
    }
 
    public void logMarketEvent(String eventType, boolean started) {
        logger.info(() -> "[Economy] Market event " + eventType + " " + (started ? "started" : "ended") + ".");
    }
 
    public void logMoneySinkAlert(String currencyId, double cumulativeSink, double threshold) {
        logger.warning(() -> "[Economy] Cumulative money sink for " + currencyId + " reached "
            + cumulativeSink + " (threshold: " + threshold + ").");
    }
 
    public void logSuspiciousBalance(java.util.UUID playerId, String currencyId, double balance) {
        logger.warning(() -> "[Economy] Suspicious balance detected for " + playerId + " in " + currencyId + ": " + balance);
    }
}
