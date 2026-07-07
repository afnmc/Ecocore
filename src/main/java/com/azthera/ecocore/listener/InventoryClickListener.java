package com.azthera.ecocore.listener;
 
import com.azthera.ecocore.gui.AbstractGui;
import com.azthera.ecocore.gui.GuiSessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
 
/**
 * Routes every click/drag inside a custom EcoCore GUI to that GUI's own
 * {@code handleClick} method via {@link GuiSessionManager}, and cleans up
 * the session on close. Clicks are always cancelled for EcoCore GUIs to
 * prevent item duplication/theft from what are meant to be non-inventory
 * menus, since every button's action is what actually performs any state
 * change, not manual item movement.
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
        if (holder instanceof AbstractGui) {
            event.setCancelled(true);
        }
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
