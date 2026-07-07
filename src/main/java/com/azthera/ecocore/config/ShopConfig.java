package com.azthera.ecocore.config;
 
import org.bukkit.configuration.file.YamlConfiguration;
 
/**
 * Typed view over modules/shop.yml: default pricing/elasticity bounds used when
 * an individual shop item does not override them, plus stock and GUI paging defaults.
 */
public final class ShopConfig {
 
    private double defaultElasticity;
    private double defaultMinPriceFactor;
    private double defaultMaxPriceFactor;
    private long defaultRestockSeconds;
    private int defaultStock;
    private boolean realtimePricing;
    private int guiRows;
    private int itemsPerPage;
 
    public ShopConfig(YamlConfiguration source) {
        load(source);
    }
 
    public void load(YamlConfiguration source) {
        this.defaultElasticity = source.getDouble("pricing.default-elasticity", 0.15);
        this.defaultMinPriceFactor = source.getDouble("pricing.default-min-factor", 0.4);
        this.defaultMaxPriceFactor = source.getDouble("pricing.default-max-factor", 2.5);
        this.realtimePricing = source.getBoolean("pricing.realtime", true);
        this.defaultRestockSeconds = source.getLong("stock.default-restock-seconds", 1800L);
        this.defaultStock = source.getInt("stock.default-stock", 64);
        this.guiRows = source.getInt("gui.rows", 6);
        this.itemsPerPage = source.getInt("gui.items-per-page", 45);
    }
 
    public double getDefaultElasticity() {
        return defaultElasticity;
    }
 
    public double getDefaultMinPriceFactor() {
        return defaultMinPriceFactor;
    }
 
    public double getDefaultMaxPriceFactor() {
        return defaultMaxPriceFactor;
    }
 
    public long getDefaultRestockSeconds() {
        return defaultRestockSeconds;
    }
 
    public int getDefaultStock() {
        return defaultStock;
    }
 
    public boolean isRealtimePricing() {
        return realtimePricing;
    }
 
    public int getGuiRows() {
        return guiRows;
    }
 
    public int getItemsPerPage() {
        return itemsPerPage;
    }
}
