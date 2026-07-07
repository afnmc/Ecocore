package com.azthera.ecocore.integration;
 
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
 
import java.util.concurrent.TimeUnit;
 
/**
 * SchedulerAdapter implementation backed by Paper's region-threaded scheduler
 * APIs (GlobalRegionScheduler, AsyncScheduler, RegionScheduler, EntityScheduler).
 * Used when the server is running Folia.
 */
public final class FoliaSchedulerAdapter implements SchedulerAdapter {
 
    private final Plugin plugin;
 
    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }
 
    private EcoScheduledTask wrap(ScheduledTask scheduledTask) {
        if (scheduledTask == null) {
            return new EcoScheduledTask(() -> { }, () -> true);
        }
        return new EcoScheduledTask(scheduledTask::cancel, scheduledTask::isCancelled);
    }
 
    @Override
    public EcoScheduledTask runSync(Runnable task) {
        return wrap(Bukkit.getGlobalRegionScheduler().run(plugin, ignored -> task.run()));
    }
 
    @Override
    public EcoScheduledTask runSyncLater(Runnable task, long delayTicks) {
        long safeDelay = Math.max(1, delayTicks);
        return wrap(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, ignored -> task.run(), safeDelay));
    }
 
    @Override
    public EcoScheduledTask runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        long safeDelay = Math.max(1, delayTicks);
        long safePeriod = Math.max(1, periodTicks);
        return wrap(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, ignored -> task.run(), safeDelay, safePeriod));
    }
 
    @Override
    public EcoScheduledTask runAsync(Runnable task) {
        return wrap(Bukkit.getAsyncScheduler().runNow(plugin, ignored -> task.run()));
    }
 
    @Override
    public EcoScheduledTask runAsyncLater(Runnable task, long delayTicks) {
        long delayMillis = delayTicks * 50L;
        return wrap(Bukkit.getAsyncScheduler().runDelayed(plugin, ignored -> task.run(), delayMillis, TimeUnit.MILLISECONDS));
    }
 
    @Override
    public EcoScheduledTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        long delayMillis = delayTicks * 50L;
        long periodMillis = periodTicks * 50L;
        return wrap(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, ignored -> task.run(), delayMillis, periodMillis, TimeUnit.MILLISECONDS));
    }
 
    @Override
    public EcoScheduledTask runAtEntity(Entity entity, Runnable task) {
        return wrap(entity.getScheduler().run(plugin, ignored -> task.run(), null));
    }
 
    @Override
    public EcoScheduledTask runAtLocation(Location location, Runnable task) {
        return wrap(Bukkit.getRegionScheduler().run(plugin, location, ignored -> task.run()));
    }
 
    @Override
    public boolean isGlobalThread() {
        return false;
    }
}
