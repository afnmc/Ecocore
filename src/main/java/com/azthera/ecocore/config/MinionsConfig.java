package com.azthera.ecocore.config;
 
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
 
import java.util.HashMap;
import java.util.Map;
 
/**
 * Typed view over modules/minions.yml: the global module toggle (so this
 * feature can be disabled entirely if a third-party minion plugin is used
 * instead), the per-player minion limit, storage/fuel defaults, and per-type
 * upgrade ceilings for Miner, Farmer, Lumberjack, Fishing, and Collector minions.
 */
public final class MinionsConfig {
 
    private boolean moduleEnabled;
    private int maxMinionsPerPlayer;
    private int baseStorageSlots;
    private int baseFuelCapacity;
    private final Map<String, MinionTypeDefinition> minionTypeDefinitions = new HashMap<>();
 
    public MinionsConfig(YamlConfiguration source) {
        load(source);
    }
 
    public void load(YamlConfiguration source) {
        this.moduleEnabled = source.getBoolean("enabled", true);
        this.maxMinionsPerPlayer = source.getInt("limits.max-per-player", 5);
        this.baseStorageSlots = source.getInt("defaults.storage-slots", 9);
        this.baseFuelCapacity = source.getInt("defaults.fuel-capacity", 64);
 
        minionTypeDefinitions.clear();
        ConfigurationSection typesSection = source.getConfigurationSection("types");
        if (typesSection != null) {
            for (String typeId : typesSection.getKeys(false)) {
                ConfigurationSection typeSection = typesSection.getConfigurationSection(typeId);
                if (typeSection == null) {
                    continue;
                }
                minionTypeDefinitions.put(typeId.toUpperCase(), new MinionTypeDefinition(
                    typeId.toUpperCase(),
                    typeSection.getBoolean("enabled", true),
                    typeSection.getDouble("base-action-interval-seconds", 5.0),
                    typeSection.getInt("max-upgrade-speed-level", 5),
                    typeSection.getInt("max-upgrade-capacity-level", 5),
                    typeSection.getInt("max-upgrade-fortune-level", 5)
                ));
            }
        }
    }
 
    public boolean isModuleEnabled() {
        return moduleEnabled;
    }
 
    public int getMaxMinionsPerPlayer() {
        return maxMinionsPerPlayer;
    }
 
    public int getBaseStorageSlots() {
        return baseStorageSlots;
    }
 
    public int getBaseFuelCapacity() {
        return baseFuelCapacity;
    }
 
    public Map<String, MinionTypeDefinition> getMinionTypeDefinitions() {
        return Map.copyOf(minionTypeDefinitions);
    }
 
    public record MinionTypeDefinition(
        String id,
        boolean enabled,
        double baseActionIntervalSeconds,
        int maxUpgradeSpeedLevel,
        int maxUpgradeCapacityLevel,
        int maxUpgradeFortuneLevel
    ) {
    }
}
