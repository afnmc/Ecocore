package com.azthera.ecocore.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGui implements InventoryHolder {
    // FIX: Diubah ke protected agar SellGui bisa akses langsung untuk fitur cart drag/drop
    protected final Inventory inventory;
    private final GuiSessionManager sessionManager;
    private final Map<Integer, GuiButton> buttons = new HashMap<>();

    protected AbstractGui(int size, Component title, GuiSessionManager sessionManager) {
        this.inventory = org.bukkit.Bukkit.createInventory(this, size, title);
        this.sessionManager = sessionManager;
    }

    @Override
    public Inventory getInventory() { return inventory; }
    public abstract void render();
    public void onClose(Player player) { }

    protected void setButton(int slot, GuiButton button) {
        buttons.put(slot, button);
        inventory.setItem(slot, button.getIcon());
    }

    protected void clearButtons() { buttons.clear(); }

    protected void fillBorder(ItemStack fillerItem) {
        int size = inventory.getSize();
        int rows = size / 9;
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            boolean isBorder = row == 0 || row == rows - 1 || col == 0 || col == 8;
            if (isBorder && inventory.getItem(slot) == null) {
                inventory.setItem(slot, fillerItem);
            }
        }
    }

    public void handleClick(Player player, int slot, ClickType clickType) {
        GuiButton button = buttons.get(slot);
        if (button != null) button.handleClick(player, clickType);
    }

    public void open(Player player) {
        render();
        player.openInventory(inventory);
        sessionManager.registerOpenSession(player, this);
    }

    protected GuiSessionManager getSessionManager() { return sessionManager; }
}