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

public final class InventoryClickListener implements Listener {
    private final GuiSessionManager sessionManager;

    public InventoryClickListener(GuiSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AbstractGui gui)) return;

        // SPECIAL CASE: SellGui Cart Area - allow drag/drop
        if (gui instanceof SellGui && SellGui.isCartSlot(event.getRawSlot())) {
            event.setCancelled(false);
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) return;
        gui.handleClick(player, event.getRawSlot(), event.getClick());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AbstractGui gui)) return;

        // SPECIAL CASE: SellGui Cart Area Drag - allow drag
        if (gui instanceof SellGui) {
            boolean allInCart = event.getRawSlots().stream().allMatch(SellGui::isCartSlot);
            if (allInCart) {
                event.setCancelled(false);
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AbstractGui gui)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        gui.onClose(player);
        sessionManager.closeSession(player.getUniqueId());
    }
}