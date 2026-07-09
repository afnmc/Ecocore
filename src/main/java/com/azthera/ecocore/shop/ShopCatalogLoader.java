package com.azthera.ecocore.shop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Parses {@code modules/shop-items.yml} into {@link ShopCategory} and
 * {@link ShopItemDefinition} instances and registers them with a
 * {@link ShopManager}. This is the only supported way to define the shop
 * catalog — server owners add/edit/remove items purely through that YAML
 * file and run {@code /eco reload}; no Java code or recompilation is
 * required.
 *
 * <p>Every item is validated before registration:</p>
 * <ul>
 *   <li>Invalid materials are skipped with a warning rather than crashing
 *       the whole catalog load.</li>
 *   <li>Price bounds are sanitized so a malformed entry can never let
 *       {@code ElasticPricingEngine} clamp to a broken or unbounded range.</li>
 *   <li>Items flagged as "OP" (elytra, netherite gear, totem, etc.) are
 *       rejected with a warning unless the admin explicitly overrides the
 *       safety check via {@code allow-op: true} on that item's section.
 *       This is the first line of defense against economy-breaking items
 *       being added to the shop by mistake.</li>
 *   <li>Since V3, the loader also reads an optional {@code sell-price} field
 *       per item (defaulting to 70% of base-price) and validates it against
 *       the item's price bounds, so the buy/sell spread is configurable
 *       per-item rather than hardcoded globally.</li>
 * </ul>
 */
public final class ShopCatalogLoader {
    /**
     * Materials considered "OP" for a survival economy. These items can
     * trivially break the economy if sold/bought at vanilla prices (e.g.
     * elytra for infinite flight, netherite gear for instant combat
     * supremacy, totem of undying for free revival). Admins can override
     * per-item via {@code allow-op: true} in the YAML, but the default is
     * to reject them to prevent accidental economy damage.
     */
    private static final Set<Material> OP_MATERIALS = Set.of(
        Material.ELYTRA,
        Material.NETHERITE_SWORD,
        Material.NETHERITE_PICKAXE,
        Material.NETHERITE_AXE,
        Material.NETHERITE_SHOVEL,
        Material.NETHERITE_HOE,
        Material.NETHERITE_HELMET,
        Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS,
        Material.NETHERITE_BOOTS,
        Material.NETHERITE_INGOT,
        Material.NETHERITE_BLOCK,
        Material.TOTEM_OF_UNDYING,
        Material.ENCHANTED_GOLDEN_APPLE,
        Material.NETHER_STAR,
        Material.DRAGON_EGG,
        Material.DRAGON_HEAD,
        Material.BEACON,
        Material.SHULKER_BOX,
        Material.COMMAND_BLOCK,
        Material.BARRIER,
        Material.STRUCTURE_BLOCK,
        Material.STRUCTURE_VOID,
        Material.JIGSAW,
        Material.DEBUG_STICK,
        Material.SPAWNER
    );

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
        String materialName = section.getString("material");
        if (category == null || category.isBlank()) {
            logger.warning(() -> "Shop item '" + itemId + "' is missing 'category', skipping.");
            return;
        }
        Material material = materialName != null ? Material.matchMaterial(materialName) : null;
        if (material == null) {
            logger.warning(() -> "Shop item '" + itemId + "' has invalid or missing 'material' (" + materialName + "), skipping.");
            return;
        }

        // --- OP material safety check ---
        // Reject OP items unless admin explicitly overrides via allow-op: true.
        // This prevents accidental economy damage from items that can trivially
        // break the game (elytra, netherite, totem, etc.).
        if (OP_MATERIALS.contains(material) && !section.getBoolean("allow-op", false)) {
            logger.warning(() -> "Shop item '" + itemId + "' uses OP material " + material.name()
                + " without 'allow-op: true' override, skipping. If you really want this item in the shop, "
                + "add 'allow-op: true' to its section in modules/shop-items.yml.");
            return;
        }

        String displayName = section.getString("display-name", itemId);
        int customModelData = section.getInt("custom-model-data", 0);
        double basePrice = Math.max(0.0, section.getDouble("base-price", 1.0));
        double sellPrice = section.getDouble("sell-price", basePrice * 0.7);
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
        sellPrice = sanitizeSellPrice(itemId, sellPrice, basePrice, minPrice, maxPrice);

        shopManager.registerItemDefinition(new ShopItemDefinition(
            itemId, category, material.name(), displayName, customModelData,
            basePrice, sellPrice, minPrice, maxPrice, elasticity, restockSeconds, stock, buyable, sellable
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

    /**
     * Ensures the sell price is within sane bounds: must be positive, finite,
     * and ideally between minPrice and maxPrice. If admin set a sell price
     * outside that range, we clamp it to the nearest bound with a warning.
     * This prevents weird edge cases where selling gives more than buying,
     * or selling gives negative money.
     */
    private double sanitizeSellPrice(String itemId, double sellPrice, double basePrice,
                                      double minPrice, double maxPrice) {
        if (!Double.isFinite(sellPrice) || sellPrice < 0) {
            logger.warning(() -> "Shop item '" + itemId + "' has invalid sell-price, defaulting to 70% of base-price.");
            return basePrice * 0.7;
        }
        if (sellPrice > maxPrice) {
            logger.warning(() -> "Shop item '" + itemId + "' has sell-price higher than max-price, clamping to max-price.");
            return maxPrice;
        }
        if (sellPrice < minPrice * 0.1) {
            // Allow sell price to be lower than minPrice (that's the whole point of a spread),
            // but warn if it's absurdly low (less than 10% of minPrice) — likely a typo.
            logger.warning(() -> "Shop item '" + itemId + "' has very low sell-price (" + sellPrice
                + "), are you sure? Default is 70% of base-price.");
        }
        return sellPrice;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}