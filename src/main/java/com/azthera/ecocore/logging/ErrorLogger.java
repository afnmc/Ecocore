package com.azthera.ecocore.logging;
 
import java.util.logging.Level;
import java.util.logging.Logger;
 
/**
 * Centralized error logging so every module reports failures in a
 * consistent format, distinguishing recoverable warnings from severe
 * failures that may indicate data loss risk (e.g. a failed database write).
 */
public final class ErrorLogger {
 
    private final Logger logger;
 
    public ErrorLogger(Logger logger) {
        this.logger = logger;
    }
 
    public void logWarning(String context, String message) {
        logger.warning("[" + context + "] " + message);
    }
 
    public void logSevere(String context, String message, Throwable throwable) {
        logger.log(Level.SEVERE, "[" + context + "] " + message, throwable);
    }
 
    public void logSevere(String context, String message) {
        logger.severe("[" + context + "] " + message);
    }
 
    public void logDatabaseFailure(String operation, Throwable throwable) {
        logger.log(Level.SEVERE, "[Database] Operation failed: " + operation, throwable);
    }
}
