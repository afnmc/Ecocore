package com.azthera.ecocore.integration;
 
import java.util.function.BooleanSupplier;
 
/**
 * Abstraction over a scheduled task handle so that callers do not need to know
 * whether the underlying implementation is a classic {@code BukkitTask} or a
 * Folia {@code ScheduledTask}.
 */
public final class EcoScheduledTask {
 
    private final Runnable cancelAction;
    private final BooleanSupplier cancelledSupplier;
 
    public EcoScheduledTask(Runnable cancelAction, BooleanSupplier cancelledSupplier) {
        this.cancelAction = cancelAction;
        this.cancelledSupplier = cancelledSupplier;
    }
 
    public void cancel() {
        cancelAction.run();
    }
 
    public boolean isCancelled() {
        return cancelledSupplier.getAsBoolean();
    }
}
