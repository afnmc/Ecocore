package com.azthera.ecocore.market;
 
import com.azthera.ecocore.data.model.ShopItemRecord;
 
/**
 * Pure helper functions for reasoning about an item's current demand/supply
 * pressure, used by {@code WeightedEconomyModel} to decide which items the
 * AI market should preferentially act on during a simulation tick.
 */
public final class DemandSupplyCalculator {
 
    /**
     * @return positive values mean demand currently exceeds supply (price
     * pressure upward), negative values mean the opposite.
     */
    public double getNetPressure(ShopItemRecord record) {
        return record.getDemand() - record.getSupply();
    }
 
    /**
     * @return how far the current price sits between minPrice and maxPrice,
     * as a 0.0 (at minPrice) to 1.0 (at maxPrice) fraction. Used to bias the
     * AI market away from pushing an already-extreme price further to its bound.
     */
    public double getPricePositionFraction(ShopItemRecord record) {
        double range = record.getMaxPrice() - record.getMinPrice();
        if (range <= 0) {
            return 0.5;
        }
        return (record.getCurrentPrice() - record.getMinPrice()) / range;
    }
 
    /**
     * @return a weight in [0, 1] representing how "interesting" this item is
     * for the AI market to act on right now — items near price equilibrium
     * with low existing pressure are weighted higher (more room to move),
     * while items already pinned at their bounds are weighted lower.
     */
    public double computeActionWeight(ShopItemRecord record) {
        double positionFraction = getPricePositionFraction(record);
        double distanceFromExtreme = Math.min(positionFraction, 1.0 - positionFraction) * 2.0;
        return Math.max(0.05, distanceFromExtreme);
    }
}
