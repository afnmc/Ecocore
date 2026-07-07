package com.azthera.ecocore.gui.impl;
 
import com.azthera.ecocore.gui.AbstractGui;
import com.azthera.ecocore.gui.GuiButton;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.sell.SellService;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.NumberFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;
 
/**
 * Simple three-button GUI for the Sell module: sell the item in hand, sell
 * every sellable item in the inventory, or close. Actual pricing/payout
 * logic lives entirely in {@link SellService}, which itself delegates to
 * the same {@code ShopTransactionService} the Dynamic Shop uses.
 */
public final class SellGui extends AbstractGui {
 
    private final SellService sellService;
    private final GuiIconResolver iconResolver;
    private final MessageService messageService;
    private final NumberFormatter numberFormatter;
    private final YamlConfiguration guiConfig;
 
    public SellGui(SellService sellService, YamlConfiguration guiConfig, GuiIconResolver iconResolver,
                    MessageService messageService, GuiSessionManager sessionManager, NumberFormatter numberFormatter) {
        super(iconResolver.resolveRows(guiConfig, 6) * 9,
            Component.text(iconResolver.resolveTitle(guiConfig, "Sell").replaceAll("<[^>]+>", "")), sessionManager);
        this.sellService = sellService;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.messageService = messageService;
        this.numberFormatter = numberFormatter;
    }
 
    @Override
    public void render() {
        clearButtons();
 
        int sellHandSlot = iconResolver.resolveSlot(guiConfig, "sell-hand-slot", 48);
        int sellAllSlot = iconResolver.resolveSlot(guiConfig, "sell-all-slot", 50);
        int confirmSlot = iconResolver.resolveSlot(guiConfig, "confirm-slot", 53);
 
        setButton(sellHandSlot, GuiButton.of(iconResolver.resolveIcon(guiConfig, "sell-hand"), (player, clickType) -> {
            var result = sellService.sellHand(player);
            if (result.isSuccess()) {
                player.sendMessage(Component.text("Terjual seharga " + numberFormatter.format(result.orElse(0.0)), NamedTextColor.GREEN));
            } else {
                result.onFailure(reason -> player.sendMessage(Component.text(reason, NamedTextColor.RED)));
            }
        }));
 
        setButton(sellAllSlot, GuiButton.of(iconResolver.resolveIcon(guiConfig, "sell-all"), (player, clickType) -> {
            var result = sellService.sellInventory(player);
            if (result.isSuccess()) {
                player.sendMessage(Component.text("Inventory terjual seharga " + numberFormatter.format(result.orElse(0.0)), NamedTextColor.GREEN));
            } else {
                result.onFailure(reason -> player.sendMessage(Component.text(reason, NamedTextColor.RED)));
            }
        }));
 
        setButton(confirmSlot, GuiButton.of(iconResolver.resolveIcon(guiConfig, "confirm"), (player, clickType) ->
            player.closeInventory()));
    }
}
