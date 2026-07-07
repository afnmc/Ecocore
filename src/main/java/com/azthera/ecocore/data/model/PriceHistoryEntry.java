package com.azthera.ecocore.data.model;
 
/**
 * A single historical price snapshot for a shop item, recorded periodically
 * so admins can inspect price trends over time in the Admin Editor GUI.
 */
public final class PriceHistoryEntry {
 
    private final long id;
    private final String itemId;
    private final double price;
    private final double demand;
    private final double supply;
    private final long timestampMillis;
 
    public PriceHistoryEntry(long id, String itemId, double price, double demand, double supply,
                              long timestampMillis) {
        this.id = id;
        this.itemId = itemId;
        this.price = price;
        this.demand = demand;
        this.supply = supply;
        this.timestampMillis = timestampMillis;
    }
 
    public long getId() {
        return id;
    }
 
    public String getItemId() {
        return itemId;
    }
 
    public double getPrice() {
        return price;
    }
 
    public double getDemand() {
        return demand;
    }
 
    public double getSupply() {
        return supply;
    }
 
    public long getTimestampMillis() {
        return timestampMillis;
    }
}
