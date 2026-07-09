package com.azthera.ecocore.shop;

import com.azthera.ecocore.bootstrap.Module;
import com.azthera.ecocore.config.ShopConfig;
import com.azthera.ecocore.data.model.ShopItemRecord;
import com.azthera.ecocore.data.repository.ShopItemRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Lifecycle owner of the Dynamic Shop module. Loads {@code ShopItemDefinition}s
 * and {@code ShopCategory}s from config, ensures a corresponding
 * {@code ShopItemRecord} exists (creating one with initial pricing values on
 * first run) for every defined item, and exposes lookups used by the shop GUI.
 *
 * <p>Since V3, the initial record creation also sets {@code sellPrice} from
 * the definition's {@code initialSellPrice} field (typically 70% of basePrice),
 * so new items immediately have a distinct buy/sell spread without needing
 * a separate adjustment step.</p>
 */
public final class ShopManager implements Module {
    private final ShopItemRepository shopItemRepository;
    private final RestockScheduler restockScheduler;
    private ShopConfig shopConfig;
    private final Logger logger;
    private final Map<String, ShopItemDefinition> itemDefinitions = new LinkedHashMap<>();
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();
    private boolean enabled;

    public ShopManager(ShopItemRepository shopItemRepository, RestockScheduler restockScheduler,
                        ShopConfig shopConfig, Logger logger) {
        this.shopItemRepository = shopItemRepository;
        this.restockScheduler = restockScheduler;
        this.shopConfig = shopConfig;
        this.logger = logger;
    }

    @Override
    public String getName() {
        return "shop";
    }

    @Override
    public void enable() {
        this.enabled = true;
        shopItemRepository.loadAllIntoCacheAsync().thenRun(this::ensureAllDefinitionsHaveRecords);
        restockScheduler.start();
    }

    @Override
    public void disable() {
        this.enabled = false;
        restockScheduler.stop();
    }

    @Override
    public void reload() {
        disable();
        enable();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void registerItemDefinition(ShopItemDefinition definition) {
        itemDefinitions.put(definition.getItemId(), definition);
    }

    public void registerCategory(ShopCategory category) {
        categories.put(category.getId(), category);
    }

    /**
     * Ensures every registered definition has a corresponding {@code ShopItemRecord}
     * in the database (and cache). For new items, creates a record with the
     * definition's initial pricing values (including the new {@code initialSellPrice}).
     * Existing records are NOT overwritten — their dynamic pricing state is preserved.
     */
    private void ensureAllDefinitionsHaveRecords() {
        long now = System.currentTimeMillis();
        for (ShopItemDefinition definition : itemDefinitions.values()) {
            if (shopItemRepository.getCached(definition.getItemId()).isEmpty()) {
                ShopItemRecord record = new ShopItemRecord(
                    definition.getItemId(),
                    definition.getInitialBasePrice(),
                    definition.getInitialBasePrice(),
                    definition.getInitialSellPrice(),
                    definition.getInitialMinPrice(),
                    definition.getInitialMaxPrice(),
                    definition.getInitialElasticity(),
                    0.0,
                    0.0,
                    definition.getInitialRestockSeconds(),
                    definition.getInitialStock(),
                    now
                );
                shopItemRepository.save(record);
                logger.info(() -> "Initialized new shop item: " + definition.getItemId()
                    + " (buy=" + definition.getInitialBasePrice() + ", sell=" + definition.getInitialSellPrice() + ")");
            }
        }
    }

    public Optional<ShopItemDefinition> getDefinition(String itemId) {
        return Optional.ofNullable(itemDefinitions.get(itemId));
    }

    public Optional<ShopItemRecord> getRecord(String itemId) {
        return shopItemRepository.getCached(itemId);
    }

    public Collection<ShopItemDefinition> getAllDefinitions() {
        return itemDefinitions.values();
    }

    public Collection<ShopItemDefinition> getDefinitionsForCategory(String categoryId) {
        return itemDefinitions.values().stream()
            .filter(def -> def.getCategoryId().equals(categoryId))
            .toList();
    }

    public Collection<ShopCategory> getAllCategories() {
        return categories.values();
    }

    public void updateShopConfig(ShopConfig shopConfig) {
        this.shopConfig = shopConfig;
    }

    public ShopConfig getShopConfig() {
        return shopConfig;
    }
}