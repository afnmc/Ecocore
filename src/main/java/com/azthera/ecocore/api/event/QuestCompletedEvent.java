package com.azthera.ecocore.api.event;
 
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
 
/**
 * Fired when a player's quest instance transitions to completed (objective
 * progress reached its required amount), before the reward has necessarily
 * been claimed. Not cancellable, since progress has already been persisted.
 */
public final class QuestCompletedEvent extends Event {
 
    private static final HandlerList HANDLERS = new HandlerList();
 
    private final Player player;
    private final String questInstanceId;
    private final String questDefinitionId;
 
    public QuestCompletedEvent(Player player, String questInstanceId, String questDefinitionId) {
        this.player = player;
        this.questInstanceId = questInstanceId;
        this.questDefinitionId = questDefinitionId;
    }
 
    public Player getPlayer() {
        return player;
    }
 
    public String getQuestInstanceId() {
        return questInstanceId;
    }
 
    public String getQuestDefinitionId() {
        return questDefinitionId;
    }
 
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
 
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
