package com.azthera.ecocore.api.event;
 
import com.azthera.ecocore.minions.MinionType;
import com.azthera.ecocore.minions.MinionUpgradeService;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
 
import java.util.UUID;
 
/**
 * Fired after a minion upgrade purchase succeeds (Speed, Capacity, or
 * Fortune track). Not cancellable — payment and the level increase have
 * already been applied.
 */
public final class MinionUpgradeEvent extends Event {
 
    private static final HandlerList HANDLERS = new HandlerList();
 
    private final Player owner;
    private final UUID minionId;
    private final MinionType minionType;
    private final MinionUpgradeService.UpgradeTrack track;
    private final int newLevel;
 
    public MinionUpgradeEvent(Player owner, UUID minionId, MinionType minionType,
                               MinionUpgradeService.UpgradeTrack track, int newLevel) {
        this.owner = owner;
        this.minionId = minionId;
        this.minionType = minionType;
        this.track = track;
        this.newLevel = newLevel;
    }
 
    public Player getOwner() {
        return owner;
    }
 
    public UUID getMinionId() {
        return minionId;
    }
 
    public MinionType getMinionType() {
        return minionType;
    }
 
    public MinionUpgradeService.UpgradeTrack getTrack() {
        return track;
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
