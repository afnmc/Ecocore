package com.azthera.ecocore.integration;
 
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
 
/**
 * SchedulerAdapter implementation backed by the classic single-threaded
 * Bukkit/Paper scheduler. Used when the server is NOT running Folia.
 */
public final class BukkitSchedulerAdapter implements SchedulerAdapter {
 
    private final Plugin plugin;
    private final BukkitScheduler scheduler;
 
    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();
    }
 
    private EcoScheduledTask wrap(BukkitTask bukkitTask) {
        return new EcoScheduledTask(bukkitTask::cancel, bukkitTask::isCancelled);
    }
 
    @Override
    public EcoScheduledTask runSync(Runnable task) {
        return wrap(scheduler.runTask(plugin, task));
    }
 
    @Override
    public EcoScheduledTask runSyncLater(Runnable task, long delayTicks) {
        return wrap(scheduler.runTaskLater(plugin, task, delayTicks));
    }
 
    @Override
    public EcoScheduledTask runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        return wrap(scheduler.runTaskTimer(plugin, task, delayTicks, periodTicks));
    }
 
    @Override
    public EcoScheduledTask runAsync(Runnable task) {
        return wrap(scheduler.runTaskAsynchronously(plugin, task));
    }
 
    @Override
    public EcoScheduledTask runAsyncLater(Runnable task, long delayTicks) {
        return wrap(scheduler.runTaskLaterAsynchronously(plugin, task, delayTicks));
    }
 
    @Override
    public EcoScheduledTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        return wrap(scheduler.runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks));
    }
 
    @Override
    public EcoScheduledTask runAtEntity(Entity entity, Runnable task) {
        return runSync(task);
    }
 
    @Override
    public EcoScheduledTask runAtLocation(Location location, Runnable task) {
        return runSync(task);
    }
 
    @Override
    public boolean isGlobalThread() {
        return true;
    }
}
