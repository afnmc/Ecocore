package com.azthera.ecocore.shop;
 
import com.azthera.ecocore.data.model.ShopItemRecord;
 
/**
 * Computes the new current price of a shop item in response to a buy/sell
 * action or a passive market influence (AI market, market events, inflation).
 * Implementations must respect minPrice/maxPrice bounds at all times.
 */
public interface PricingEngine {
 
    /**
     * Recalculates and returns the new current price after a player buy
     * action increased demand by {@code quantity} units. Does not mutate
     * {@code record} — the caller is responsible for applying the result.
     */
    double onBuy(ShopItemRecord record, int quantity);
 
    /**
     * Recalculates and returns the new current price after a player sell
     * action increased supply by {@code quantity} units.
     */
    double onSell(ShopItemRecord record, int quantity);
 
    /**
     * Recalculates price purely from the current demand/supply state without
     * any new action — used by passive decay ticks so demand/supply pressure
     * gradually normalizes back toward equilibrium over time.
     */
    double recalculate(ShopItemRecord record);
 
    /**
     * Applies an external multiplier (from an active market event or
     * inflation adjustment) on top of the item's own elasticity-driven price,
     * still clamped to [minPrice, maxPrice].
     */
    double applyExternalModifier(ShopItemRecord record, double modifier);
}
