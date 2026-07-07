package com.azthera.ecocore.integration;
 
import org.bukkit.Location;
import org.bukkit.entity.Entity;
 
/**
 * Unified scheduling abstraction that lets the rest of the plugin schedule work
 * without caring whether the server is running classic Paper or Folia.
 * Selection of the concrete implementation happens once, in {@code EcoCorePlugin#onEnable}.
 */
public interface SchedulerAdapter {
 
    EcoScheduledTask runSync(Runnable task);
 
    EcoScheduledTask runSyncLater(Runnable task, long delayTicks);
 
    EcoScheduledTask runSyncRepeating(Runnable task, long delayTicks, long periodTicks);
 
    EcoScheduledTask runAsync(Runnable task);
 
    EcoScheduledTask runAsyncLater(Runnable task, long delayTicks);
 
    EcoScheduledTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks);
 
    /**
     * Runs a task on the region thread that owns the given entity (Folia),
     * or simply on the main thread (classic Paper).
     */
    EcoScheduledTask runAtEntity(Entity entity, Runnable task);
 
    /**
     * Runs a task on the region thread that owns the given location (Folia),
     * or simply on the main thread (classic Paper).
     */
    EcoScheduledTask runAtLocation(Location location, Runnable task);
 
    /**
     * @return true if this adapter runs everything on a single global main thread
     * (classic Paper), false if it is region-threaded (Folia).
     */
    boolean isGlobalThread();
}
