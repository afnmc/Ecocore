package com.azthera.ecocore.command.sub;

import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.config.GuiConfig;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.gui.impl.SellGui;
import com.azthera.ecocore.sell.SellService;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.NumberFormatter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /ecocore sell} — opens the Sell GUI for the sender (sell item in
 * hand, sell whole inventory).
 */
public final class SellOpenSubCommand implements SubCommand {

    private final SellService sellService;
    private final GuiConfig guiConfig;
    private final GuiIconResolver iconResolver;
    private final MessageService messageService;
    private final GuiSessionManager sessionManager;
    private final NumberFormatter numberFormatter;

    public SellOpenSubCommand(SellService sellService, GuiConfig guiConfig, GuiIconResolver iconResolver,
                               MessageService messageService, GuiSessionManager sessionManager,
                               NumberFormatter numberFormatter) {
        this.sellService = sellService;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.messageService = messageService;
        this.sessionManager = sessionManager;
        this.numberFormatter = numberFormatter;
    }

    @Override
    public String getName() {
        return "sell";
    }

    @Override
    public String getPermission() {
        return "ecocore.sell.use";
    }

    @Override
    public String getUsage() {
        return "/ecocore sell";
    }

    @Override
    public String getDescription() {
        return "Buka menu Sell (jual item di tangan atau seluruh inventory).";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.render("general.player-only"));
            return;
        }

        new SellGui(sellService, guiConfig.getSellGui(), iconResolver, messageService, sessionManager, numberFormatter)
            .open(player);
    }
}
