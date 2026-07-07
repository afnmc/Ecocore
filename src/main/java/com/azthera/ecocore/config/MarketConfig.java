package com.azthera.ecocore.config;
 
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
 
import java.util.HashMap;
import java.util.Map;
 
/**
 * Typed view over modules/market.yml: AI market simulation tick interval,
 * action weight, and the full table of configurable market events
 * (Boom, Crash, Inflation, Deflation, Festival, Mining/Farming/Fishing Week).
 */
public final class MarketConfig {
 
    private long tickIntervalSeconds;
    private double aiActionsPerTick;
    private boolean newsBroadcastEnabled;
    private final Map<String, MarketEventDefinition> eventDefinitions = new HashMap<>();
 
    public MarketConfig(YamlConfiguration source) {
        load(source);
    }
 
    public void load(YamlConfiguration source) {
        this.tickIntervalSeconds = source.getLong("simulation.tick-interval-seconds", 300L);
        this.aiActionsPerTick = source.getDouble("simulation.actions-per-tick", 5.0);
        this.newsBroadcastEnabled = source.getBoolean("news.enabled", true);
 
        eventDefinitions.clear();
        ConfigurationSection eventsSection = source.getConfigurationSection("events");
        if (eventsSection != null) {
            for (String eventId : eventsSection.getKeys(false)) {
                ConfigurationSection eventSection = eventsSection.getConfigurationSection(eventId);
                if (eventSection == null) {
                    continue;
                }
                eventDefinitions.put(eventId.toUpperCase(), new MarketEventDefinition(
                    eventId.toUpperCase(),
                    eventSection.getBoolean("enabled", true),
                    eventSection.getDouble("weight", 1.0),
                    eventSection.getLong("min-duration-seconds", 600L),
                    eventSection.getLong("max-duration-seconds", 1800L),
                    eventSection.getDouble("price-modifier", 1.0),
                    eventSection.getDouble("demand-modifier", 1.0)
                ));
            }
        }
    }
 
    public long getTickIntervalSeconds() {
        return tickIntervalSeconds;
    }
 
    public double getAiActionsPerTick() {
        return aiActionsPerTick;
    }
 
    public boolean isNewsBroadcastEnabled() {
        return newsBroadcastEnabled;
    }
 
    public Map<String, MarketEventDefinition> getEventDefinitions() {
        return Map.copyOf(eventDefinitions);
    }
 
    public record MarketEventDefinition(
        String id,
        boolean enabled,
        double weight,
        long minDurationSeconds,
        long maxDurationSeconds,
        double priceModifier,
        double demandModifier
    ) {
    }
}
