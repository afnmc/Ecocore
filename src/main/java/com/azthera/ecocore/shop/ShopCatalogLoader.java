package com.azthera.ecocore.shop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.logging.Logger;

/**
 * Parses {@code modules/shop-items.yml} into {@link ShopCategory} and
 * {@link ShopItemDefinition} instances and registers them with a
 * {@link ShopManager}. This is the only supported way to define the shop
 * catalog — server owners add/edit/remove items purely through that YAML
 * file and run {@code /eco reload}; no Java code or recompilation is
 * required.
 *
 * <p>Every item is validated before registration: invalid materials are
 * skipped with a warning rather than crashing the whole catalog load, and
 * price bounds are sanitized (see {@link #sanitizePriceBounds}) so a
 * malformed entry (e.g. {@code max-price} lower than {@code base-price}, or
 * a missing/negative value) can never let {@code ElasticPricingEngine}
 * clamp to a broken or unbounded range.</p>
 */
public final class ShopCatalogLoader {

    private final Logger logger;

    public ShopCatalogLoader(Logger logger) {
        this.logger = logger;
    }

    /**
     * Loads every category and item defined in {@code catalogConfig} and
     * registers them with {@code shopManager}. Safe to call again on
     * {@code /eco reload}: {@link ShopManager#registerCategory} and
     * {@link ShopManager#registerItemDefinition} simply overwrite the prior
     * in-memory definition for the same id, they never touch the
     * already-persisted {@code ShopItemRecord} pricing state.
     */
    public void loadInto(ShopManager shopManager, YamlConfiguration catalogConfig) {
        loadCategories(shopManager, catalogConfig);
        loadItems(shopManager, catalogConfig);
    }

    private void loadCategories(ShopManager shopManager, YamlConfiguration catalogConfig) {
        ConfigurationSection categoriesSection = catalogConfig.getConfigurationSection("categories");
        if (categoriesSection == null) {
            logger.warning("modules/shop-items.yml has no 'categories' section; shop GUI will have no tabs.");
            return;
        }

        for (String categoryId : categoriesSection.getKeys(false)) {
            ConfigurationSection section = categoriesSection.getConfigurationSection(categoryId);
            if (section == null) {
                continue;
            }
            String displayName = section.getString("display-name", categoryId);
            String icon = section.getString("icon", "CHEST");
            int sortOrder = section.getInt("sort-order", 0);

            shopManager.registerCategory(new ShopCategory(categoryId, displayName, icon, sortOrder));
        }
    }

    private void loadItems(ShopManager shopManager, YamlConfiguration catalogConfig) {
        ConfigurationSection itemsSection = catalogConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            logger.warning("modules/shop-items.yml has no 'items' section; the shop will be empty.");
            return;
        }

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection section = itemsSection.getConfigurationSection(itemId);
            if (section == null) {
                continue;
            }
            registerItem(shopManager, itemId, section);
        }
    }

    private void registerItem(ShopManager shopManager, String itemId, ConfigurationSection section) {
        String category = section.getString("category");
        String material = section.getString("material");

        if (category == null || category.isBlank()) {
            logger.warning(() -> "Shop item '" + itemId + "' is missing 'category', skipping.");
            return;
        }
        if (material == null || org.bukkit.Material.matchMaterial(material) == null) {
            logger.warning(() -> "Shop item '" + itemId + "' has invalid or missing 'material' (" + material + "), skipping.");
            return;
        }

        String displayName = section.getString("display-name", itemId);
        int customModelData = section.getInt("custom-model-data", 0);
        double basePrice = Math.max(0.0, section.getDouble("base-price", 1.0));
        double minPrice = Math.max(0.0, section.getDouble("min-price", basePrice * 0.4));
        double maxPrice = section.getDouble("max-price", basePrice * 2.5);
        double elasticity = clamp(section.getDouble("elasticity", 0.15), 0.0, 1.0);
        long restockSeconds = Math.max(60L, section.getLong("restock-seconds", 1800L));
        int stock = Math.max(0, section.getInt("stock", 64));
        boolean buyable = section.getBoolean("buyable", true);
        boolean sellable = section.getBoolean("sellable", true);

        double[] bounds = sanitizePriceBounds(itemId, basePrice, minPrice, maxPrice);
        minPrice = bounds[0];
        maxPrice = bounds[1];

        shopManager.registerItemDefinition(new ShopItemDefinition(
            itemId, category, material, displayName, customModelData,
            basePrice, minPrice, maxPrice, elasticity, restockSeconds, stock, buyable, sellable
        ));
    }

    /**
     * Guarantees {@code minPrice <= basePrice <= maxPrice} and that every
     * bound is a finite, non-negative number, regardless of what an admin
     * typed in the YAML. This is the last line of defense before a
     * definition reaches {@link ShopManager}: {@code ElasticPricingEngine}
     * trusts these bounds completely, so a broken config value here would
     * otherwise be the one way a price could end up unbounded.
     */
    private double[] sanitizePriceBounds(String itemId, double basePrice, double minPrice, double maxPrice) {
        if (!Double.isFinite(minPrice) || minPrice < 0) {
            logger.warning(() -> "Shop item '" + itemId + "' has invalid min-price, defaulting to 40% of base-price.");
            minPrice = basePrice * 0.4;
        }
        if (!Double.isFinite(maxPrice) || maxPrice <= 0) {
            logger.warning(() -> "Shop item '" + itemId + "' has invalid max-price, defaulting to 250% of base-price.");
            maxPrice = basePrice * 2.5;
        }
        if (maxPrice < minPrice) {
            logger.warning(() -> "Shop item '" + itemId + "' has max-price lower than min-price, swapping them.");
            double temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }
        return new double[] { minPrice, maxPrice };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
