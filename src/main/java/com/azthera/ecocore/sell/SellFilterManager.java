package com.azthera.ecocore.sell;
 
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
 
import java.util.HashSet;
import java.util.Set;
 
/**
 * Owns the whitelist/blacklist of sellable materials for the Sell module.
 * If a whitelist is configured (non-empty), only whitelisted materials are
 * sellable and the blacklist is ignored; otherwise every material is
 * sellable except those explicitly blacklisted. Config-driven, reloadable.
 */
public final class SellFilterManager {
 
    private final Set<Material> whitelist = new HashSet<>();
    private final Set<Material> blacklist = new HashSet<>();
 
    public SellFilterManager(YamlConfiguration sellSection) {
        load(sellSection);
    }
 
    public void load(YamlConfiguration sellSection) {
        whitelist.clear();
        blacklist.clear();
 
        for (String materialName : sellSection.getStringList("whitelist")) {
            parseAndAdd(materialName, whitelist);
        }
        for (String materialName : sellSection.getStringList("blacklist")) {
            parseAndAdd(materialName, blacklist);
        }
    }
 
    private void parseAndAdd(String materialName, Set<Material> target) {
        Material material = Material.matchMaterial(materialName);
        if (material != null) {
            target.add(material);
        }
    }
 
    public boolean isSellable(Material material) {
        if (!whitelist.isEmpty()) {
            return whitelist.contains(material);
        }
        return !blacklist.contains(material);
    }
 
    public boolean hasWhitelist() {
        return !whitelist.isEmpty();
    }
}
