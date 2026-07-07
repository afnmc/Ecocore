package com.azthera.ecocore.inflation;
 
/**
 * Computes the velocity of money: how quickly currency changes hands
 * relative to the total money supply. A high transaction volume against a
 * small money supply indicates a "hot" economy (velocity > 1), while a large
 * idle supply with little transaction activity indicates a "cold" one
 * (velocity closer to 0). This feeds into {@code InflationCalculator} because
 * a high-velocity economy with rapidly growing supply inflates faster than a
 * slow one with the same nominal money creation.
 */
public final class VelocityCalculator {
 
    /**
     * @param transactionVolume total absolute value of transactions (buys, sells,
     * rewards, tax) that occurred in the measurement window.
     * @param totalMoneySupply the total money supply at the time of measurement.
     * @return velocity as a ratio; 0 if supply is zero to avoid division errors.
     */
    public double computeVelocity(double transactionVolume, double totalMoneySupply) {
        if (totalMoneySupply <= 0) {
            return 0.0;
        }
        return transactionVolume / totalMoneySupply;
    }
 
    /**
     * Scales a raw inflation growth rate by velocity to produce an effective
     * rate — a fast-circulating economy experiences the effect of new money
     * creation sooner and more sharply than a slow one.
     */
    public double applyVelocityScaling(double rawGrowthRate, double velocity) {
        double velocityFactor = 1.0 + Math.min(1.0, Math.max(0.0, velocity - 1.0)) * 0.5;
        return rawGrowthRate * velocityFactor;
    }
}
