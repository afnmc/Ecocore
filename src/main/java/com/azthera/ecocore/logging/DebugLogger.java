package com.azthera.ecocore.logging;
 
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.function.Supplier;
 
/**
 * Central toggle and sink for verbose debug output. Wraps the plugin's
 * standard {@link Logger} so debug lines are prefixed consistently and only
 * emitted when debug mode is on, avoiding log spam in production while
 * still allowing {@code /eco debug} to enable it on demand without a restart.
 */
public final class DebugLogger {
 
    private final Logger logger;
    private final AtomicBoolean debugEnabled;
 
    public DebugLogger(Logger logger, boolean initialState) {
        this.logger = logger;
        this.debugEnabled = new AtomicBoolean(initialState);
    }
 
    public boolean toggle() {
        return debugEnabled.updateAndGet(current -> !current);
    }
 
    public void setEnabled(boolean enabled) {
        debugEnabled.set(enabled);
    }
 
    public boolean isEnabled() {
        return debugEnabled.get();
    }
 
    public void log(String message) {
        if (debugEnabled.get()) {
            logger.info("[DEBUG] " + message);
        }
    }
 
    public void log(Supplier<String> messageSupplier) {
        if (debugEnabled.get()) {
            logger.info("[DEBUG] " + messageSupplier.get());
        }
    }
}
