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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * {@code /ecocore shop} — opens the Dynamic Shop GUI directly for the
 * sender (all categories), without needing to find and click a shop NPC.
 * GUI construction/render is wrapped in a try/catch: any exception is
 * logged in FULL (with stack trace) to the console and reported to the
 * player as a short error line, instead of the command silently doing
 * nothing — this makes future failures diagnosable instead of feeling like
 * the command "just doesn't work".
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

        if (shopManager.getAllDefinitions().isEmpty()) {
            player.sendMessage(Component.text(
                "Shop belum siap (katalog belum termuat). Coba lagi sebentar atau hubungi admin.",
                NamedTextColor.RED));
            return;
        }

        try {
            new ShopGui(shopManager, shopTransactionService, guiConfig.getShopGui(), iconResolver, messageService,
                sessionManager, numberFormatter, null).open(player);
        } catch (Exception exception) {
            player.sendMessage(Component.text(
                "Gagal membuka shop: " + exception.getClass().getSimpleName()
                    + ". Detail sudah dicatat di console server, tolong laporkan ke admin.",
                NamedTextColor.RED));
            JavaPlugin.getProvidingPlugin(ShopOpenSubCommand.class).getLogger().log(
                Level.SEVERE, "Failed to open Shop GUI for " + player.getName(), exception
            );
        }
    }
                }
