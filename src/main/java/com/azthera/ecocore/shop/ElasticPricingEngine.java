package com.azthera.ecocore.shop;
 
import com.azthera.ecocore.data.model.ShopItemRecord;
 
/**
 * Default {@link PricingEngine} implementation using a classic supply/demand
 * elasticity model: price moves proportionally to (demand - supply) scaled
 * by the item's elasticity coefficient, always clamped to [minPrice, maxPrice].
 * Demand/supply values decay slightly on every recalculation so that price
 * pressure naturally fades if no further buying/selling occurs (this is what
 * makes the "harga perlahan turun" behavior work for a quiet economy).
 */
public final class ElasticPricingEngine implements PricingEngine {
 
    private static final double DEMAND_SUPPLY_DECAY = 0.98;
    private static final double MAX_PRESSURE = 50.0;
 
    @Override
    public double onBuy(ShopItemRecord record, int quantity) {
        double newDemand = Math.min(record.getDemand() + quantity, MAX_PRESSURE);
        record.setDemand(newDemand);
        return recalculate(record);
    }
 
    @Override
    public double onSell(ShopItemRecord record, int quantity) {
        double newSupply = Math.min(record.getSupply() + quantity, MAX_PRESSURE);
        record.setSupply(newSupply);
        return recalculate(record);
    }
 
    @Override
    public double recalculate(ShopItemRecord record) {
        double pressure = record.getDemand() - record.getSupply();
        double priceMultiplier = 1.0 + (pressure * record.getElasticity() / 100.0);
        double newPrice = record.getBasePrice() * priceMultiplier;
 
        newPrice = clamp(newPrice, record.getMinPrice(), record.getMaxPrice());
 
        record.setDemand(record.getDemand() * DEMAND_SUPPLY_DECAY);
        record.setSupply(record.getSupply() * DEMAND_SUPPLY_DECAY);
 
        return newPrice;
    }
 
    @Override
    public double applyExternalModifier(ShopItemRecord record, double modifier) {
        double basePrice = record.getCurrentPrice();
        double modifiedPrice = basePrice * modifier;
        return clamp(modifiedPrice, record.getMinPrice(), record.getMaxPrice());
    }
 
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
