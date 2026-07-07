package com.azthera.ecocore.logging;
 
import java.util.UUID;
import java.util.logging.Logger;
 
/**
 * Records administrative actions (config reload, admin give/take, shop item
 * edits via Admin Editor GUI, manual market event triggers) distinct from
 * regular player transactions, so server owners can audit staff activity
 * specifically. Complements {@link TransactionLogger}, which only covers
 * balance-affecting events, not configuration/state changes.
 */
public final class AuditLogger {
 
    private final Logger logger;
 
    public AuditLogger(Logger logger) {
        this.logger = logger;
    }
 
    public void logAdminAction(String adminName, String action, String details) {
        logger.info(() -> "[Audit] " + adminName + " performed '" + action + "': " + details);
    }
 
    public void logConfigReload(String adminName) {
        logger.info(() -> "[Audit] " + adminName + " reloaded EcoCore configuration.");
    }
 
    public void logShopItemEdit(String adminName, String itemId, String field, String oldValue, String newValue) {
        logger.info(() -> "[Audit] " + adminName + " edited shop item '" + itemId + "' field '" + field
            + "' from '" + oldValue + "' to '" + newValue + "'.");
    }
 
    public void logAdminCurrencyChange(String adminName, UUID targetPlayerId, double amount, boolean given) {
        logger.info(() -> "[Audit] " + adminName + " " + (given ? "gave" : "took") + " " + amount
            + " to/from player " + targetPlayerId);
    }
}
