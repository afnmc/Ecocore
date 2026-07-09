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

/**
 * Routes every click/drag inside a custom EcoCore GUI.
 * 
 * <p><b>SPECIAL CASE:</b> {@link SellGui} allows item movement in its cart
 * area (slots 10-16, 19-25, 28-34) so players can drag items from their
 * inventory into the cart. All other slots in SellGui are locked buttons.
 * For all other GUIs, clicks are always cancelled to prevent item
 * duplication/theft.</p>
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
        
        // SPECIAL CASE: SellGui Cart Area
        if (gui instanceof SellGui && SellGui.isCartSlot(event.getRawSlot())) {
            event.setCancelled(false); // Allow item movement in cart
            return;
        }

        // Default: Cancel all clicks in EcoCore GUIs
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
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

        // SPECIAL CASE: SellGui Cart Area Drag
        if (gui instanceof SellGui) {
            boolean allInCart = event.getRawSlots().stream().allMatch(SellGui::isCartSlot);
            if (allInCart) {
                event.setCancelled(false); // Allow drag in cart
                return;
            }
        }

        // Default: Cancel drags
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
