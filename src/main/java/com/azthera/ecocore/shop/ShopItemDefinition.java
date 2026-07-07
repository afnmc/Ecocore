package com.azthera.ecocore.shop;
 
/**
 * Static, config-defined metadata for a shop item: what material it maps
 * to, which category it belongs to, and the initial pricing parameters used
 * only when the item's {@code ShopItemRecord} does not yet exist in the
 * database (first-ever load). All mutable pricing/stock state lives in
 * {@code ShopItemRecord}, not here.
 */
public final class ShopItemDefinition {
 
    private final String itemId;
    private final String categoryId;
    private final String material;
    private final String displayName;
    private final int customModelData;
    private final double initialBasePrice;
    private final double initialMinPrice;
    private final double initialMaxPrice;
    private final double initialElasticity;
    private final long initialRestockSeconds;
    private final int initialStock;
    private final boolean buyable;
    private final boolean sellable;
 
    public ShopItemDefinition(String itemId, String categoryId, String material, String displayName,
                               int customModelData, double initialBasePrice, double initialMinPrice,
                               double initialMaxPrice, double initialElasticity, long initialRestockSeconds,
                               int initialStock, boolean buyable, boolean sellable) {
        this.itemId = itemId;
        this.categoryId = categoryId;
        this.material = material;
        this.displayName = displayName;
        this.customModelData = customModelData;
        this.initialBasePrice = initialBasePrice;
        this.initialMinPrice = initialMinPrice;
        this.initialMaxPrice = initialMaxPrice;
        this.initialElasticity = initialElasticity;
        this.initialRestockSeconds = initialRestockSeconds;
        this.initialStock = initialStock;
        this.buyable = buyable;
        this.sellable = sellable;
    }
 
    public String getItemId() {
        return itemId;
    }
 
    public String getCategoryId() {
        return categoryId;
    }
 
    public String getMaterial() {
        return material;
    }
 
    public String getDisplayName() {
        return displayName;
    }
 
    public int getCustomModelData() {
        return customModelData;
    }
 
    public double getInitialBasePrice() {
        return initialBasePrice;
    }
 
    public double getInitialMinPrice() {
        return initialMinPrice;
    }
 
    public double getInitialMaxPrice() {
        return initialMaxPrice;
    }
 
    public double getInitialElasticity() {
        return initialElasticity;
    }
 
    public long getInitialRestockSeconds() {
        return initialRestockSeconds;
    }
 
    public int getInitialStock() {
        return initialStock;
    }
 
    public boolean isBuyable() {
        return buyable;
    }
 
    public boolean isSellable() {
        return sellable;
    }
}
