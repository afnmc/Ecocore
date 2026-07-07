package com.azthera.ecocore.inflation;
 
import com.azthera.ecocore.config.InflationConfig;
 
/**
 * Combines a {@link MoneySupplyTracker} snapshot's growth rate with
 * {@link VelocityCalculator}'s velocity scaling to produce the economy's
 * current effective inflation rate, and classifies it against the
 * configured high/low thresholds so {@code InflationAdjustmentService} knows
 * whether to nudge prices up, down, or leave them alone.
 */
public final class InflationCalculator {
 
    private final VelocityCalculator velocityCalculator;
    private InflationConfig inflationConfig;
 
    public InflationCalculator(VelocityCalculator velocityCalculator, InflationConfig inflationConfig) {
        this.velocityCalculator = velocityCalculator;
        this.inflationConfig = inflationConfig;
    }
 
    public void reload(InflationConfig inflationConfig) {
        this.inflationConfig = inflationConfig;
    }
 
    public double computeEffectiveInflationRate(MoneySupplyTracker.Snapshot snapshot, double transactionVolume) {
        double rawGrowthRate = snapshot.getGrowthRate();
        double velocity = velocityCalculator.computeVelocity(transactionVolume, snapshot.totalSupply());
        return velocityCalculator.applyVelocityScaling(rawGrowthRate, velocity);
    }
 
    public InflationState classify(double effectiveInflationRate) {
        if (effectiveInflationRate >= inflationConfig.getHighInflationThreshold()) {
            return InflationState.HIGH_INFLATION;
        }
        if (effectiveInflationRate <= inflationConfig.getLowInflationThreshold()) {
            return InflationState.DEFLATION;
        }
        return InflationState.STABLE;
    }
 
    /**
     * Categorizes the current state of the economy relative to configured
     * thresholds, driving whether NPC prices/upgrade costs/tax should drift
     * up, drift down, or hold steady.
     */
    public enum InflationState {
        HIGH_INFLATION,
        STABLE,
        DEFLATION
    }
}
