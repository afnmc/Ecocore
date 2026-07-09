package com.azthera.ecocore.gui.impl;

import com.azthera.ecocore.gui.AbstractGui;
import com.azthera.ecocore.gui.GuiButton;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.sell.SellService;
import com.azthera.ecocore.util.ItemBuilder;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.NumberFormatter;
import com.azthera.ecocore.util.Result;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Cart-style Sell GUI: players drag items from their inventory into a
 * 27-slot storage area (3 rows), then click "Sell All" to sell everything
 * in the cart. If the player closes the GUI without selling, all items
 * remaining in the cart are returned to their inventory.
 */
public final class SellGui extends AbstractGui {
    // Cart area: rows 1-3 (slots 10-16, 19-25, 28-34)
    private static final int[] CART_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    private static final int SELL_ALL_SLOT = 38;
    private static final int RETURN_ALL_SLOT = 40;
    private static final int CLOSE_SLOT = 42;
    private static final int INFO_SLOT = 4;

    private final SellService sellService;
    private final GuiIconResolver iconResolver;
    private final MessageService messageService;
    private final NumberFormatter numberFormatter;
    private final YamlConfiguration guiConfig;

    public SellGui(SellService sellService, YamlConfiguration guiConfig, GuiIconResolver iconResolver,
                    MessageService messageService, GuiSessionManager sessionManager, NumberFormatter numberFormatter) {
        super(54, Component.text(stripLegacy(iconResolver.resolveTitle(guiConfig, "Sell"))), sessionManager);
        this.sellService = sellService;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.messageService = messageService;
        this.numberFormatter = numberFormatter;
    }

    private static String stripLegacy(String raw) {
        return raw == null ? "Sell" : raw.replaceAll("<[^>]+>", "");
    }

    @Override
    public void render() {
        clearButtons();
        ItemStack filler = iconResolver.resolveIcon(guiConfig, "border-filler");
        fillBorder(filler);

        // Header row (row 0)
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        // Cart area (rows 1-3) — leave empty so players can drag items in
        for (int slot : CART_SLOTS) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, null);
            }
        }

        // Action bar (row 4)
        setButton(SELL_ALL_SLOT, GuiButton.of(
            ItemBuilder.of(Material.EMERALD_BLOCK)
                .name(Component.text("Jual Semua di Keranjang", NamedTextColor.GREEN))
                .lore(Component.text("Klik untuk menjual semua item di keranjang.", NamedTextColor.GRAY))
                .build(),
            (player, clickType) -> handleSellAll(player)
        ));
        setButton(RETURN_ALL_SLOT, GuiButton.of(
            ItemBuilder.of(Material.HOPPER)
                .name(Component.text("Kembalikan ke Inventory", NamedTextColor.YELLOW))
                .lore(Component.text("Klik untuk mengembalikan semua item ke inventory.", NamedTextColor.GRAY))
                .build(),
            (player, clickType) -> handleReturnAll(player)
        ));
        setButton(CLOSE_SLOT, GuiButton.of(
            ItemBuilder.of(Material.BARRIER)
                .name(Component.text("Tutup", NamedTextColor.RED))
                .build(),
            (player, clickType) -> player.closeInventory()
        ));

        // Info slot (header center)
        setButton(INFO_SLOT, GuiButton.display(
            ItemBuilder.of(Material.BOOK)
                .name(Component.text("Info Keranjang", NamedTextColor.GOLD))
                .lore(Component.text("Drag item dari inventory ke keranjang.", NamedTextColor.GRAY))
                .lore(Component.text("Klik 'Jual Semua' untuk menjual.", NamedTextColor.GRAY))
                .lore(Component.text("Item yang tidak dijual akan dikembalikan saat tutup.", NamedTextColor.GRAY))
                .build()
        ));

        // Footer row (row 5)
        for (int i = 45; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private void handleSellAll(Player player) {
        List<ItemStack> cartItems = collectCartItems();
        if (cartItems.isEmpty()) {
            player.sendMessage(Component.text("Keranjang kosong. Drag item dari inventory ke keranjang terlebih dahulu.", NamedTextColor.RED));
            return;
        }
        Result<Double> result = sellService.sellCartItems(player, cartItems);
        if (result.isSuccess()) {
            double payout = result.orElse(0.0);
            player.sendMessage(messageService.render("sell.cart-sell-success",
                MessageService.placeholder("price", numberFormatter.format(payout))));
            clearCart();
            render();
        } else {
            result.onFailure(reason -> player.sendMessage(Component.text(reason, NamedTextColor.RED)));
        }
    }

    private void handleReturnAll(Player player) {
        List<ItemStack> cartItems = collectCartItems();
        if (cartItems.isEmpty()) {
            player.sendMessage(Component.text("Keranjang sudah kosong.", NamedTextColor.YELLOW));
            return;
        }
        returnItemsToPlayer(player, cartItems);
        clearCart();
        player.sendMessage(Component.text("Item dikembalikan ke inventory.", NamedTextColor.GREEN));
        render();
    }

    private List<ItemStack> collectCartItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int slot : CART_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                items.add(item.clone());
            }
        }
        return items;
    }

    private void clearCart() {
        for (int slot : CART_SLOTS) {
            inventory.setItem(slot, null);
        }
    }

    private void returnItemsToPlayer(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            var leftover = player.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    @Override
    public void onClose(Player player) {
        List<ItemStack> remainingItems = collectCartItems();
        if (!remainingItems.isEmpty()) {
            returnItemsToPlayer(player, remainingItems);
            player.sendMessage(Component.text("Item yang tidak dijual dikembalikan ke inventory.", NamedTextColor.YELLOW));
        }
    }

    /**
     * @return the array of slot indices that make up the cart storage area.
     * Used by {@code InventoryClickListener} to allow drag/drop only in these slots.
     */
    public static int[] getCartSlots() {
        return CART_SLOTS.clone();
    }
                      }
