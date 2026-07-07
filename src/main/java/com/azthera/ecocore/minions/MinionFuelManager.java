package com.azthera.ecocore.minions;
 
import org.bukkit.Material;
 
import java.util.Map;
 
/**
 * Maps fuel item materials to the number of minion action-ticks they grant,
 * and provides helpers for checking/consuming fuel from a minion's storage
 * inventory. Coal is the baseline fuel; other combustible items grant
 * proportionally more or less, mirroring vanilla furnace burn time ratios
 * loosely without depending on Bukkit's furnace recipe API.
 */
public final class MinionFuelManager {
 
    private static final Map<Material, Integer> FUEL_VALUES = Map.of(
        Material.COAL, 80,
        Material.CHARCOAL, 80,
        Material.COAL_BLOCK, 800,
        Material.BLAZE_ROD, 120,
        Material.LAVA_BUCKET, 1000
    );
 
    public boolean isFuel(Material material) {
        return FUEL_VALUES.containsKey(material);
    }
 
    public int getFuelValue(Material material) {
        return FUEL_VALUES.getOrDefault(material, 0);
    }
 
    /**
     * Attempts to refuel a minion instance by consuming one fuel item from
     * its storage inventory. Returns true if fuel was found and consumed.
     */
    public boolean tryRefuelFromStorage(MinionInstance instance) {
        var inventory = instance.getStorageInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            var item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            if (isFuel(item.getType())) {
                int fuelValue = getFuelValue(item.getType());
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) {
                    inventory.setItem(slot, null);
                }
                instance.addFuel(fuelValue);
                return true;
            }
        }
        return false;
    }
}
