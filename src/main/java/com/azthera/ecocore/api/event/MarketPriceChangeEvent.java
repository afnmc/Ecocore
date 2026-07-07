package com.azthera.ecocore.api.event;
 
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
 
/**
 * Fired whenever a shop item's current price changes, regardless of cause
 * (player buy/sell, AI market simulation, active market event modifier, or
 * inflation adjustment). Not cancellable — the price change has already
 * been applied and persisted. Useful for plugins driving external price
 * displays (web dashboards, Discord bots) that want to react to price
 * movement without polling.
 */
public final class MarketPriceChangeEvent extends Event {
 
    private static final HandlerList HANDLERS = new HandlerList();
 
    private final String itemId;
    private final double previousPrice;
    private final double newPrice;
    private final String cause;
 
    public MarketPriceChangeEvent(String itemId, double previousPrice, double newPrice, String cause) {
        this.itemId = itemId;
        this.previousPrice = previousPrice;
        this.newPrice = newPrice;
        this.cause = cause;
    }
 
    public String getItemId() {
        return itemId;
    }
 
    public double getPreviousPrice() {
        return previousPrice;
    }
 
    public double getNewPrice() {
        return newPrice;
    }
 
    public String getCause() {
        return cause;
    }
 
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
 
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
