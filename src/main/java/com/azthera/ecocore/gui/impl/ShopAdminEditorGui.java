package com.azthera.ecocore.gui.impl;

import com.azthera.ecocore.data.model.ShopItemRecord;
import com.azthera.ecocore.data.repository.ShopItemRepository;
import com.azthera.ecocore.gui.AbstractGui;
import com.azthera.ecocore.gui.GuiButton;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.shop.ShopManager;
import com.azthera.ecocore.util.ItemBuilder;
import com.azthera.ecocore.util.NumberFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Admin-only editor for a single shop item's pricing/stock fields. Every
 * click adjusts the underlying {@code ShopItemRecord} directly (through
 * {@code ShopItemRepository}, which persists asynchronously) and
 * immediately re-renders so the admin sees the new value without leaving
 * the GUI. Intended to be opened only from the {@code /eco shop} subcommand,
 * which is responsible for the {@code ecocore.command.eco.shop} permission check.
 *
 * <p>Since V3, this editor also exposes the {@code sellPrice} field, allowing
 * admins to adjust the buy/sell spread per item. The sell price is displayed
 * in green to distinguish it from the buy price (gold).</p>
 */
public final class ShopAdminEditorGui extends AbstractGui {
    private static final int BASE_PRICE_MINUS = 10;
    private static final int BASE_PRICE_PLUS = 11;
    private static final int SELL_PRICE_MINUS = 12;
    private static final int SELL_PRICE_PLUS = 13;
    private static final int MIN_PRICE_MINUS = 19;
    private static final int MIN_PRICE_PLUS = 20;
    private static final int MAX_PRICE_MINUS = 28;
    private static final int MAX_PRICE_PLUS = 29;
    private static final int ELASTICITY_MINUS = 37;
    private static final int ELASTICITY_PLUS = 38;
    private static final int STOCK_MINUS = 15;
    private static final int STOCK_PLUS = 16;
    private static final int RESTOCK_MINUS = 24;
    private static final int RESTOCK_PLUS = 25;
    private static final int INFO_SLOT = 22;
    private static final int CLOSE_SLOT = 49;

    private final ShopManager shopManager;
    private final ShopItemRepository shopItemRepository;
    private final NumberFormatter numberFormatter;
    private final String itemId;

    public ShopAdminEditorGui(ShopManager shopManager, ShopItemRepository shopItemRepository,
                               GuiSessionManager sessionManager, NumberFormatter numberFormatter, String itemId) {
        super(54, Component.text("Admin Editor: " + itemId), sessionManager);
        this.shopManager = shopManager;
        this.shopItemRepository = shopItemRepository;
        this.numberFormatter = numberFormatter;
        this.itemId = itemId;
    }

    @Override
    public void render() {
        clearButtons();
        var recordOpt = shopManager.getRecord(itemId);
        if (recordOpt.isEmpty()) {
            setButton(INFO_SLOT, GuiButton.display(
                ItemBuilder.of(Material.BARRIER).name(Component.text("Item tidak ditemukan", NamedTextColor.RED)).build()));
            return;
        }
        ShopItemRecord record = recordOpt.get();
        setButton(BASE_PRICE_MINUS, adjustButton(Material.RED_DYE, "Base Price -5%", () -> adjust(record, r -> r.setBasePrice(r.getBasePrice() * 0.95))));
        setButton(BASE_PRICE_PLUS, adjustButton(Material.LIME_DYE, "Base Price +5%", () -> adjust(record, r -> r.setBasePrice(r.getBasePrice() * 1.05))));
        setButton(SELL_PRICE_MINUS, adjustButton(Material.RED_DYE, "Sell Price -5%", () -> adjust(record, r -> r.setSellPrice(Math.max(0.01, r.getSellPrice() * 0.95)))));
        setButton(SELL_PRICE_PLUS, adjustButton(Material.LIME_DYE, "Sell Price +5%", () -> adjust(record, r -> r.setSellPrice(r.getSellPrice() * 1.05))));
        setButton(MIN_PRICE_MINUS, adjustButton(Material.RED_DYE, "Min Price -5%", () -> adjust(record, r -> r.setMinPrice(r.getMinPrice() * 0.95))));
        setButton(MIN_PRICE_PLUS, adjustButton(Material.LIME_DYE, "Min Price +5%", () -> adjust(record, r -> r.setMinPrice(r.getMinPrice() * 1.05))));
        setButton(MAX_PRICE_MINUS, adjustButton(Material.RED_DYE, "Max Price -5%", () -> adjust(record, r -> r.setMaxPrice(r.getMaxPrice() * 0.95))));
        setButton(MAX_PRICE_PLUS, adjustButton(Material.LIME_DYE, "Max Price +5%", () -> adjust(record, r -> r.setMaxPrice(r.getMaxPrice() * 1.05))));
        setButton(ELASTICITY_MINUS, adjustButton(Material.RED_DYE, "Elasticity -0.01", () -> adjust(record, r -> r.setElasticity(Math.max(0.01, r.getElasticity() - 0.01)))));
        setButton(ELASTICITY_PLUS, adjustButton(Material.LIME_DYE, "Elasticity +0.01", () -> adjust(record, r -> r.setElasticity(r.getElasticity() + 0.01))));
        setButton(STOCK_MINUS, adjustButton(Material.RED_DYE, "Stock -10", () -> adjust(record, r -> r.setStock(Math.max(0, r.getStock() - 10)))));
        setButton(STOCK_PLUS, adjustButton(Material.LIME_DYE, "Stock +10", () -> adjust(record, r -> r.setStock(r.getStock() + 10))));
        setButton(RESTOCK_MINUS, adjustButton(Material.RED_DYE, "Restock -60s", () -> adjust(record, r -> r.setRestockIntervalSeconds(Math.max(60L, r.getRestockIntervalSeconds() - 60L)))));
        setButton(RESTOCK_PLUS, adjustButton(Material.LIME_DYE, "Restock +60s", () -> adjust(record, r -> r.setRestockIntervalSeconds(r.getRestockIntervalSeconds() + 60L))));
        setButton(INFO_SLOT, GuiButton.display(buildInfoIcon(record)));
        setButton(CLOSE_SLOT, GuiButton.of(
            ItemBuilder.of(Material.BARRIER).name(Component.text("Tutup", NamedTextColor.RED)).build(),
            (player, clickType) -> player.closeInventory()));
    }

    private GuiButton adjustButton(Material material, String label, Runnable action) {
        return GuiButton.of(
            ItemBuilder.of(material).name(Component.text(label, NamedTextColor.YELLOW)).build(),
            (player, clickType) -> action.run()
        );
    }

    private void adjust(ShopItemRecord record, java.util.function.Consumer<ShopItemRecord> mutation) {
        mutation.accept(record);
        shopItemRepository.save(record);
        render();
    }

    private ItemStack buildInfoIcon(ShopItemRecord record) {
        return ItemBuilder.of(Material.BOOK)
            .name(Component.text(itemId, NamedTextColor.GOLD))
            .lore(Component.text("Base Price: " + numberFormatter.format(record.getBasePrice()), NamedTextColor.GOLD))
            .lore(Component.text("Buy Price: " + numberFormatter.format(record.getCurrentPrice()), NamedTextColor.GOLD))
            .lore(Component.text("Sell Price: " + numberFormatter.format(record.getSellPrice()), NamedTextColor.GREEN))
            .lore(Component.text("Min/Max: " + numberFormatter.format(record.getMinPrice()) + " / " + numberFormatter.format(record.getMaxPrice()), NamedTextColor.GRAY))
            .lore(Component.text("Elasticity: " + record.getElasticity(), NamedTextColor.GRAY))
            .lore(Component.text("Stock: " + record.getStock(), NamedTextColor.GRAY))
            .lore(Component.text("Restock: " + record.getRestockIntervalSeconds() + "s", NamedTextColor.GRAY))
            .build();
    }
}