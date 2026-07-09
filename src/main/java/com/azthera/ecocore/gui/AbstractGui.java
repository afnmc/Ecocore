package com.azthera.ecocore.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for every custom inventory menu in EcoCore. Owns the raw
 * {@link Inventory}, a slot-to-{@link GuiButton} map used to dispatch clicks,
 * and registers itself with {@link GuiSessionManager} on open. Subclasses
 * implement {@link #render()} to (re)populate their contents and may
 * override {@link #onClose(Player)} for cleanup.
 *
 * <p><b>Since V3:</b> The {@code inventory} field is {@code protected} (not
 * {@code private}) so subclasses like {@code SellGui} that implement cart-style
 * drag/drop interfaces can directly manipulate inventory slots. External code
 * outside the class hierarchy still cannot access it, preserving encapsulation.</p>
 */
public abstract class AbstractGui implements InventoryHolder {
    // FIX: Changed from 'private' to 'protected' so subclasses like SellGui 
    // can directly manipulate inventory slots for cart-style drag/drop.
    protected final Inventory inventory;
    private final GuiSessionManager sessionManager;
    private final Map<Integer, GuiButton> buttons = new HashMap<>();

    protected AbstractGui(int size, Component title, GuiSessionManager sessionManager) {
        this.inventory = org.bukkit.Bukkit.createInventory(this, size, title);
        this.sessionManager = sessionManager;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * (Re)populates the inventory's contents and the slot->button map.
     * Implementations should call {@link #setButton(int, GuiButton)} for
     * every interactive slot and {@code getInventory().setItem(slot, item)}
     * directly for purely decorative slots (border fillers, etc).
     */
    public abstract void render();

    /**
     * Called by {@code InventoryClickListener} when the inventory this GUI
     * owns is closed by the player, before the session is removed from
     * {@link GuiSessionManager}. Default implementation does nothing.
     */
    public void onClose(Player player) {
    }

    protected void setButton(int slot, GuiButton button) {
        buttons.put(slot, button);
        inventory.setItem(slot, button.getIcon());
    }

    protected void clearButtons() {
        buttons.clear();
    }

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
        if (button != null) {
            button.handleClick(player, clickType);
        }
    }

    public void open(Player player) {
        render();
        player.openInventory(inventory);
        sessionManager.registerOpenSession(player, this);
    }

    protected GuiSessionManager getSessionManager() {
        return sessionManager;
    }
}
