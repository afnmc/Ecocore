package com.azthera.ecocore.data.model;
 
/**
 * Persistent state for a single dynamic shop item: its current pricing
 * position between minPrice and maxPrice, its supply/demand pressure,
 * remaining stock, and when it last restocked. Static definition data
 * (material, category, display name) lives separately in
 * {@code ShopItemDefinition} loaded from config, not in this record.
 */
public final class ShopItemRecord {
 
    private final String itemId;
    private double basePrice;
    private double currentPrice;
    private double minPrice;
    private double maxPrice;
    private double elasticity;
    private double demand;
    private double supply;
    private long restockIntervalSeconds;
    private int stock;
    private long lastRestockMillis;
 
    public ShopItemRecord(String itemId, double basePrice, double currentPrice, double minPrice, double maxPrice,
                           double elasticity, double demand, double supply, long restockIntervalSeconds,
                           int stock, long lastRestockMillis) {
        this.itemId = itemId;
        this.basePrice = basePrice;
        this.currentPrice = currentPrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.elasticity = elasticity;
        this.demand = demand;
        this.supply = supply;
        this.restockIntervalSeconds = restockIntervalSeconds;
        this.stock = stock;
        this.lastRestockMillis = lastRestockMillis;
    }
 
    public String getItemId() {
        return itemId;
    }
 
    public double getBasePrice() {
        return basePrice;
    }
 
    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }
 
    public double getCurrentPrice() {
        return currentPrice;
    }
 
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
 
    public double getMinPrice() {
        return minPrice;
    }
 
    public void setMinPrice(double minPrice) {
        this.minPrice = minPrice;
    }
 
    public double getMaxPrice() {
        return maxPrice;
    }
 
    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }
 
    public double getElasticity() {
        return elasticity;
    }
 
    public void setElasticity(double elasticity) {
        this.elasticity = elasticity;
    }
 
    public double getDemand() {
        return demand;
    }
 
    public void setDemand(double demand) {
        this.demand = demand;
    }
 
    public double getSupply() {
        return supply;
    }
 
    public void setSupply(double supply) {
        this.supply = supply;
    }
 
    public long getRestockIntervalSeconds() {
        return restockIntervalSeconds;
    }
 
    public void setRestockIntervalSeconds(long restockIntervalSeconds) {
        this.restockIntervalSeconds = restockIntervalSeconds;
    }
 
    public int getStock() {
        return stock;
    }
 
    public void setStock(int stock) {
        this.stock = stock;
    }
 
    public long getLastRestockMillis() {
        return lastRestockMillis;
    }
 
    public void setLastRestockMillis(long lastRestockMillis) {
        this.lastRestockMillis = lastRestockMillis;
    }
}
