package com.azthera.ecocore.sell;
 
import com.azthera.ecocore.util.Result;
import org.bukkit.Material;
import org.bukkit.entity.Player;
 
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
 
/**
 * Tracks which online players have Auto Sell toggled on, and provides the
 * hook used by job-action listeners (e.g. mining, farming) to immediately
 * sell qualifying drops instead of adding them to the player's inventory.
 * Auto Sell only applies to items already sellable per {@code SellFilterManager}
 * and requires the {@code ecocore.sell.autosell} permission, keeping it an
 * opt-in convenience rather than a pay-to-win shortcut.
 */
public final class AutoSellManager {
 
    private final SellService sellService;
    private final SellFilterManager sellFilterManager;
    private final Set<UUID> autoSellEnabledPlayers = ConcurrentHashMap.newKeySet();
 
    public AutoSellManager(SellService sellService, SellFilterManager sellFilterManager) {
        this.sellService = sellService;
        this.sellFilterManager = sellFilterManager;
    }
 
    public void enableAutoSell(Player player) {
        if (player.hasPermission("ecocore.sell.autosell")) {
            autoSellEnabledPlayers.add(player.getUniqueId());
        }
    }
 
    public void disableAutoSell(UUID playerId) {
        autoSellEnabledPlayers.remove(playerId);
    }
 
    public boolean isAutoSellEnabled(UUID playerId) {
        return autoSellEnabledPlayers.contains(playerId);
    }
 
    public void toggleAutoSell(Player player) {
        if (isAutoSellEnabled(player.getUniqueId())) {
            disableAutoSell(player.getUniqueId());
        } else {
            enableAutoSell(player);
        }
    }
 
    /**
     * Attempts to auto-sell a dropped material for the given player. Returns
     * true if the item was consumed by auto-sell (caller should not add it to
     * the inventory/drop it), false if auto-sell does not apply and normal
     * item handling should proceed.
     */
    public boolean tryAutoSell(Player player, Material material, int quantity) {
        if (!isAutoSellEnabled(player.getUniqueId())) {
            return false;
        }
        if (!sellFilterManager.isSellable(material)) {
            return false;
        }
 
        String itemId = material.name().toLowerCase();
        Result<Double> result = sellService.sellByItemId(player, itemId, material, quantity);
        return result.isSuccess();
    }
}
