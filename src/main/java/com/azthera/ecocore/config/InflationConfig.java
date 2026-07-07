package com.azthera.ecocore.config;
 
import org.bukkit.configuration.file.YamlConfiguration;
 
/**
 * Typed view over modules/inflation.yml: how often the economy-wide inflation
 * rate is recalculated, the thresholds that trigger price adjustment, and the
 * step sizes/bounds used to nudge NPC prices, upgrade costs, and tax gently
 * rather than in sharp jumps.
 */
public final class InflationConfig {
 
    private long calculationIntervalSeconds;
    private double highInflationThreshold;
    private double lowInflationThreshold;
    private double npcPriceAdjustmentStep;
    private double upgradeCostAdjustmentStep;
    private double taxAdjustmentStep;
    private double maxNpcPriceMultiplier;
    private double minNpcPriceMultiplier;
 
    public InflationConfig(YamlConfiguration source) {
        load(source);
    }
 
    public void load(YamlConfiguration source) {
        this.calculationIntervalSeconds = source.getLong("calculation-interval-seconds", 600L);
        this.highInflationThreshold = source.getDouble("thresholds.high", 0.08);
        this.lowInflationThreshold = source.getDouble("thresholds.low", -0.05);
        this.npcPriceAdjustmentStep = source.getDouble("adjustment.npc-price-step", 0.01);
        this.upgradeCostAdjustmentStep = source.getDouble("adjustment.upgrade-cost-step", 0.01);
        this.taxAdjustmentStep = source.getDouble("adjustment.tax-step", 0.001);
        this.maxNpcPriceMultiplier = source.getDouble("bounds.max-npc-price-multiplier", 1.75);
        this.minNpcPriceMultiplier = source.getDouble("bounds.min-npc-price-multiplier", 0.6);
    }
 
    public long getCalculationIntervalSeconds() {
        return calculationIntervalSeconds;
    }
 
    public double getHighInflationThreshold() {
        return highInflationThreshold;
    }
 
    public double getLowInflationThreshold() {
        return lowInflationThreshold;
    }
 
    public double getNpcPriceAdjustmentStep() {
        return npcPriceAdjustmentStep;
    }
 
    public double getUpgradeCostAdjustmentStep() {
        return upgradeCostAdjustmentStep;
    }
 
    public double getTaxAdjustmentStep() {
        return taxAdjustmentStep;
    }
 
    public double getMaxNpcPriceMultiplier() {
        return maxNpcPriceMultiplier;
    }
 
    public double getMinNpcPriceMultiplier() {
        return minNpcPriceMultiplier;
    }
}
