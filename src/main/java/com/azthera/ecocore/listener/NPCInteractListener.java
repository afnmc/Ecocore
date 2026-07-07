package com.azthera.ecocore.listener;
 
import com.azthera.ecocore.config.GuiConfig;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.gui.impl.ShopGui;
import com.azthera.ecocore.shop.ShopManager;
import com.azthera.ecocore.shop.ShopNPCManager;
import com.azthera.ecocore.shop.ShopTransactionService;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.NumberFormatter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
 
/**
 * Detects right-click interactions with entities tracked by
 * {@link ShopNPCManager} and opens the {@link ShopGui} filtered to that
 * NPC's assigned category.
 */
public final class NPCInteractListener implements Listener {
 
    private final ShopNPCManager shopNPCManager;
    private final ShopManager shopManager;
    private final ShopTransactionService shopTransactionService;
    private final GuiConfig guiConfig;
    private final GuiIconResolver iconResolver;
    private final MessageService messageService;
    private final GuiSessionManager sessionManager;
    private final NumberFormatter numberFormatter;
 
    public NPCInteractListener(ShopNPCManager shopNPCManager, ShopManager shopManager,
                                ShopTransactionService shopTransactionService, GuiConfig guiConfig,
                                GuiIconResolver iconResolver, MessageService messageService,
                                GuiSessionManager sessionManager, NumberFormatter numberFormatter) {
        this.shopNPCManager = shopNPCManager;
        this.shopManager = shopManager;
        this.shopTransactionService = shopTransactionService;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.messageService = messageService;
        this.sessionManager = sessionManager;
        this.numberFormatter = numberFormatter;
    }
 
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!shopNPCManager.isTrackedNpc(event.getRightClicked().getUniqueId())) {
            return;
        }
 
        event.setCancelled(true);
        Player player = event.getPlayer();
        String categoryId = shopNPCManager.getCategoryForNpc(event.getRightClicked().getUniqueId());
 
        new ShopGui(shopManager, shopTransactionService, guiConfig.getShopGui(), iconResolver, messageService,
            sessionManager, numberFormatter, categoryId).open(player);
    }
}
