package com.azthera.ecocore.gui;
 
import org.bukkit.entity.Player;
 
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
 
/**
 * Tracks which {@link AbstractGui} instance, if any, each online player
 * currently has open. {@code InventoryClickListener} consults this to route
 * clicks to the correct GUI instance's {@code handleClick} method, and
 * cleans up entries on inventory close/player quit to avoid leaking state
 * for offline players — important at 300-player scale.
 */
public final class GuiSessionManager {
 
    private final Map<UUID, AbstractGui> openSessions = new ConcurrentHashMap<>();
 
    public void registerOpenSession(Player player, AbstractGui gui) {
        openSessions.put(player.getUniqueId(), gui);
    }
 
    public Optional<AbstractGui> getOpenSession(UUID playerId) {
        return Optional.ofNullable(openSessions.get(playerId));
    }
 
    public void closeSession(UUID playerId) {
        openSessions.remove(playerId);
    }
 
    public boolean hasOpenSession(UUID playerId) {
        return openSessions.containsKey(playerId);
    }
 
    public int getOpenSessionCount() {
        return openSessions.size();
    }
}
