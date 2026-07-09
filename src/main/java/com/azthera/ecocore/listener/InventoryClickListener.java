package com.azthera.ecocore.listener;

import com.azthera.ecocore.gui.AbstractGui;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.gui.impl.SellGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Arrays;

/**
 * Routes every click/drag inside a custom EcoCore GUI to that GUI's own
 * {@code handleClick} method via {@link GuiSessionManager}, and cleans up
 * the session on close.
 *
 * <p><b>SPECIAL CASE:</b> {@link SellGui} allows item movement in its cart
 * area (slots 10-16, 19-25, 28-34) so players can drag items from their
 * inventory into the cart. All other slots in SellGui are locked buttons.
 * For all other GUIs, clicks are always cancelled to prevent item
 * duplication/theft from what are meant to be non-inventory menus.</p>
 */
public final class InventoryClickListener implements Listener {
    private final GuiSessionManager sessionManager;

    public InventoryClickListener(GuiSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AbstractGui gui)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }

        // SPECIAL CASE: SellGui allows item movement in cart slots
        if (gui instanceof SellGui) {
            int rawSlot = event.getRawSlot();
            int[] cartSlots = SellGui.getCartSlots();
            boolean isCartSlot = Arrays.stream(cartSlots).anyMatch(slot -> slot == rawSlot);

            // If clicking in cart area, allow it (don't cancel)
            if (isCartSlot) {
                return;
            }
            // Otherwise, cancel and route to button handler
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                gui.handleClick(player, rawSlot, event.getClick());
            }
            return;
        }

        // All other GUIs: cancel all clicks, route to button handler
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }
        gui.handleClick(player, event.getRawSlot(), event.getClick());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AbstractGui gui)) {
            return;
        }

        // SPECIAL CASE: SellGui allows drag in cart slots
        if (gui instanceof SellGui) {
            int[] cartSlots = SellGui.getCartSlots();
            boolean allSlotsAreCart = event.getRawSlots().stream()
                .allMatch(slot -> Arrays.stream(cartSlots).anyMatch(cartSlot -> cartSlot == slot));
            if (allSlotsAreCart) {
                return; // Allow drag in cart area
            }
            event.setCancelled(true);
            return;
        }

        // All other GUIs: cancel all drags
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AbstractGui gui)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        gui.onClose(player);
        sessionManager.closeSession(player.getUniqueId());
    }
}