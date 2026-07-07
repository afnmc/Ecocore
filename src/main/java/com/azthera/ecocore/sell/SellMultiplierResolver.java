package com.azthera.ecocore.sell;
 
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
 
import java.util.HashMap;
import java.util.Map;
 
/**
 * Resolves the final sell-price multiplier for a given material and player,
 * combining a global multiplier, a per-material override, and a permission-based
 * bonus (e.g. donor ranks) — always additive on top of the shop's own dynamic
 * price, never bypassing it, to keep the sell system non-pay-to-win (donor
 * bonuses affect convenience/rate, never create money out of nothing beyond
 * a modest configurable percentage).
 */
public final class SellMultiplierResolver {
 
    private double globalMultiplier;
    private final Map<Material, Double> perMaterialMultipliers = new HashMap<>();
 
    public SellMultiplierResolver(YamlConfiguration sellSection) {
        load(sellSection);
    }
 
    public void load(YamlConfiguration sellSection) {
        this.globalMultiplier = sellSection.getDouble("multiplier.global", 1.0);
        perMaterialMultipliers.clear();
 
        var overridesSection = sellSection.getConfigurationSection("multiplier.per-material");
        if (overridesSection != null) {
            for (String materialName : overridesSection.getKeys(false)) {
                Material material = Material.matchMaterial(materialName);
                if (material != null) {
                    perMaterialMultipliers.put(material, overridesSection.getDouble(materialName, 1.0));
                }
            }
        }
    }
 
    public double resolveMultiplier(Player player, Material material) {
        double multiplier = globalMultiplier * perMaterialMultipliers.getOrDefault(material, 1.0);
        multiplier *= resolvePermissionBonus(player);
        return multiplier;
    }
 
    private double resolvePermissionBonus(Player player) {
        if (player.hasPermission("ecocore.sell.bonus.tier3")) {
            return 1.15;
        }
        if (player.hasPermission("ecocore.sell.bonus.tier2")) {
            return 1.10;
        }
        if (player.hasPermission("ecocore.sell.bonus.tier1")) {
            return 1.05;
        }
        return 1.0;
    }
}
