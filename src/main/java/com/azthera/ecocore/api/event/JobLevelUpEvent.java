package com.azthera.ecocore.api.event;
 
import com.azthera.ecocore.jobs.JobType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
 
/**
 * Fired whenever a player's job level increases as a result of a job action.
 * Not cancellable — the level-up has already been persisted. Useful for
 * plugins that want to grant custom cosmetic rewards or announcements on
 * top of EcoCore's own reward system.
 */
public final class JobLevelUpEvent extends Event {
 
    private static final HandlerList HANDLERS = new HandlerList();
 
    private final Player player;
    private final JobType jobType;
    private final int previousLevel;
    private final int newLevel;
 
    public JobLevelUpEvent(Player player, JobType jobType, int previousLevel, int newLevel) {
        this.player = player;
        this.jobType = jobType;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }
 
    public Player getPlayer() {
        return player;
    }
 
    public JobType getJobType() {
        return jobType;
    }
 
    public int getPreviousLevel() {
        return previousLevel;
    }
 
    public int getNewLevel() {
        return newLevel;
    }
 
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
 
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
