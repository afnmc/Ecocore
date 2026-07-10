package com.azthera.ecocore.command.sub;

import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.config.GuiConfig;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.gui.impl.MinionGui;
import com.azthera.ecocore.minions.MinionFuelManager;
import com.azthera.ecocore.minions.MinionManager;
import com.azthera.ecocore.minions.MinionUpgradeService;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.NumberFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MinionsOpenSubCommand implements SubCommand {
    private final MinionManager minionManager;
    private final MinionUpgradeService upgradeService;
    private final MinionFuelManager fuelManager;
    private final GuiConfig guiConfig;
    private final GuiIconResolver iconResolver;
    private final GuiSessionManager sessionManager;
    private final NumberFormatter numberFormatter;
    private final MessageService messageService;

    public MinionsOpenSubCommand(MinionManager minionManager, MinionUpgradeService upgradeService,
                                  MinionFuelManager fuelManager, GuiConfig guiConfig,
                                  GuiIconResolver iconResolver, GuiSessionManager sessionManager,
                                  NumberFormatter numberFormatter, MessageService messageService) {
        this.minionManager = minionManager;
        this.upgradeService = upgradeService;
        this.fuelManager = fuelManager;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.sessionManager = sessionManager;
        this.numberFormatter = numberFormatter;
        this.messageService = messageService;
    }

    @Override
    public String getName() { return "minions"; }

    @Override
    public String getPermission() { return "ecocore.minions.use"; }

    @Override
    public String getUsage() { return "/ecocore minions"; }

    @Override
    public String getDescription() { return "Buka menu Minions (upgrade/hapus minion milikmu)."; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.render("general.player-only"));
            return;
        }
        
        if (!minionManager.isEnabled()) {
            player.sendMessage(Component.text("Modul Minions sedang tidak aktif.", NamedTextColor.RED));
            return;
        }

        try {
            new MinionGui(
                minionManager, 
                upgradeService, 
                fuelManager, 
                guiConfig.getMinionsGui(), 
                iconResolver, 
                sessionManager, 
                numberFormatter, 
                player
            ).open(player);
        } catch (Exception exception) {
            player.sendMessage(Component.text(
                "Gagal membuka menu Minions: " + exception.getClass().getSimpleName() 
                + ". Detail sudah dicatat di console server, tolong laporkan ke admin.",
                NamedTextColor.RED));
            com.azthera.ecocore.EcoCorePlugin.getPlugin(com.azthera.ecocore.EcoCorePlugin.class).getLogger()
                .severe("Failed to open Minions GUI for " + player.getName() + ": " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}