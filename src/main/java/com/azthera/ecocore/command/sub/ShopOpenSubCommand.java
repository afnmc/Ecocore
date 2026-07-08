package com.azthera.ecocore.command.sub;

import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.config.GuiConfig;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.gui.impl.ShopGui;
import com.azthera.ecocore.shop.ShopManager;
import com.azthera.ecocore.shop.ShopTransactionService;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.NumberFormatter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /ecocore shop} — opens the Dynamic Shop GUI directly for the
 * sender (all categories), without needing to find and click a shop NPC.
 * NPC interaction ({@code NPCInteractListener}) still works and opens the
 * same GUI filtered to that NPC's assigned category; this command is a
 * always-available fallback/shortcut using the same
 * {@code ecocore.shop.open} permission (default true).
 */
public final class ShopOpenSubCommand implements SubCommand {

    private final ShopManager shopManager;
    private final ShopTransactionService shopTransactionService;
    private final GuiConfig guiConfig;
    private final GuiIconResolver iconResolver;
    private final MessageService messageService;
    private final GuiSessionManager sessionManager;
    private final NumberFormatter numberFormatter;

    public ShopOpenSubCommand(ShopManager shopManager, ShopTransactionService shopTransactionService,
                               GuiConfig guiConfig, GuiIconResolver iconResolver, MessageService messageService,
                               GuiSessionManager sessionManager, NumberFormatter numberFormatter) {
        this.shopManager = shopManager;
        this.shopTransactionService = shopTransactionService;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.messageService = messageService;
        this.sessionManager = sessionManager;
        this.numberFormatter = numberFormatter;
    }

    @Override
    public String getName() {
        return "shop";
    }

    @Override
    public String getPermission() {
        return "ecocore.shop.open";
    }

    @Override
    public String getUsage() {
        return "/ecocore shop";
    }

    @Override
    public String getDescription() {
        return "Buka menu Dynamic Shop (semua kategori).";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.render("general.player-only"));
            return;
        }

        new ShopGui(shopManager, shopTransactionService, guiConfig.getShopGui(), iconResolver, messageService,
            sessionManager, numberFormatter, null).open(player);
    }
}
