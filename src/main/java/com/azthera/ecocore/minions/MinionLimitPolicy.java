package com.azthera.ecocore.minions;
 
import com.azthera.ecocore.config.MinionsConfig;
import com.azthera.ecocore.data.repository.MinionRepository;
import org.bukkit.entity.Player;
 
import java.util.UUID;
 
/**
 * Enforces the per-player minion placement limit. Checks the cached count
 * synchronously (assuming the player's minions were loaded on join) so
 * placement can be rejected instantly without an async round-trip, with the
 * {@code ecocore.minions.bypasslimit} permission allowing staff to exceed it.
 */
public final class MinionLimitPolicy {
 
    private final MinionRepository minionRepository;
    private MinionsConfig minionsConfig;
 
    public MinionLimitPolicy(MinionRepository minionRepository, MinionsConfig minionsConfig) {
        this.minionRepository = minionRepository;
        this.minionsConfig = minionsConfig;
    }
 
    public void reload(MinionsConfig minionsConfig) {
        this.minionsConfig = minionsConfig;
    }
 
    public boolean canPlaceAnother(Player player) {
        if (player.hasPermission("ecocore.minions.bypasslimit")) {
            return true;
        }
        int currentCount = minionRepository.countCachedForOwner(player.getUniqueId());
        return currentCount < minionsConfig.getMaxMinionsPerPlayer();
    }
 
    public int getRemainingSlots(UUID playerId, Player playerForPermissionCheck) {
        if (playerForPermissionCheck.hasPermission("ecocore.minions.bypasslimit")) {
            return Integer.MAX_VALUE;
        }
        int currentCount = minionRepository.countCachedForOwner(playerId);
        return Math.max(0, minionsConfig.getMaxMinionsPerPlayer() - currentCount);
    }
 
    public int getMaxMinionsPerPlayer() {
        return minionsConfig.getMaxMinionsPerPlayer();
    }
}
