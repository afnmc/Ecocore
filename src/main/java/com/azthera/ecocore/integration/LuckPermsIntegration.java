package com.azthera.ecocore.integration;
 
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
 
import java.util.Optional;
import java.util.logging.Logger;
 
/**
 * Thin wrapper around the LuckPerms API used for permission-group-based
 * lookups (e.g. sell bonus tiers, prestige cosmetic prefixes) beyond what
 * simple {@code Player#hasPermission} checks cover. Safe to construct even
 * if LuckPerms is not installed — {@link #isAvailable()} must be checked
 * (or {@link #tryInitialize()} must have returned true) before calling any
 * other method, since LuckPerms is a soft-dependency.
 */
public final class LuckPermsIntegration {
 
    private final Logger logger;
    private LuckPerms luckPerms;
    private boolean available;
 
    public LuckPermsIntegration(Logger logger) {
        this.logger = logger;
    }
 
    public boolean tryInitialize() {
        try {
            this.luckPerms = LuckPermsProvider.get();
            this.available = true;
            logger.info("LuckPerms integration enabled.");
        } catch (IllegalStateException exception) {
            this.available = false;
            logger.info("LuckPerms not found; permission-group features will use vanilla permission checks only.");
        }
        return available;
    }
 
    public boolean isAvailable() {
        return available;
    }
 
    public Optional<String> getPrimaryGroup(Player player) {
        if (!available) {
            return Optional.empty();
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(user.getPrimaryGroup());
    }
 
    public boolean isInGroup(Player player, String groupName) {
        if (!available) {
            return false;
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return false;
        }
        return user.getInheritedGroups(user.getQueryOptions()).stream()
            .anyMatch(group -> group.getName().equalsIgnoreCase(groupName));
    }
 
    public LuckPerms getLuckPermsApi() {
        if (!available) {
            throw new IllegalStateException("LuckPerms is not available.");
        }
        return luckPerms;
    }
}
