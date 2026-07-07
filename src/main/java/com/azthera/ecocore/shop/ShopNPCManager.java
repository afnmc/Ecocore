package com.azthera.ecocore.shop;
 
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
 
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
 
/**
 * Tracks which spawned NPC entities correspond to shop GUI trigger points.
 * Uses vanilla Villager entities with AI/gravity disabled and a custom name,
 * rather than a citizens/NPC library, to avoid an extra soft-dependency.
 * Interacting with a tracked NPC opens the {@code ShopGui} for the given category.
 */
public final class ShopNPCManager {
 
    private final Map<UUID, String> npcEntityToCategory = new ConcurrentHashMap<>();
 
    public Villager spawnShopNpc(Location location, String displayName, String categoryId) {
        Villager villager = location.getWorld().spawn(location, Villager.class, spawned -> {
            spawned.setAI(false);
            spawned.setInvulnerable(true);
            spawned.setSilent(true);
            spawned.setPersistent(true);
            spawned.setCustomNameVisible(true);
            spawned.customName(net.kyori.adventure.text.Component.text(displayName));
            spawned.setCollidable(false);
            spawned.setGravity(false);
            spawned.setRemoveWhenFarAway(false);
        });
        npcEntityToCategory.put(villager.getUniqueId(), categoryId);
        return villager;
    }
 
    public boolean isTrackedNpc(UUID entityId) {
        return npcEntityToCategory.containsKey(entityId);
    }
 
    public String getCategoryForNpc(UUID entityId) {
        return npcEntityToCategory.get(entityId);
    }
 
    public void untrackNpc(UUID entityId) {
        npcEntityToCategory.remove(entityId);
    }
 
    public boolean isShopEntityType(EntityType type) {
        return type == EntityType.VILLAGER;
    }
}
