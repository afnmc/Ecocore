package com.azthera.ecocore.command.sub;

import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.config.GuiConfig;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.gui.impl.MinionGui;
import com.azthera.ecocore.minions.MinionManager;
import com.azthera.ecocore.minions.MinionUpgradeService;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.NumberFormatter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /ecocore minions} — opens the Minions GUI for the sender, showing
 * their owned minions and letting them upgrade or remove each one.
 */
public final class MinionsOpenSubCommand implements SubCommand {

    private final MinionManager minionManager;
    private final MinionUpgradeService minionUpgradeService;
    private final GuiConfig guiConfig;
    private final GuiIconResolver iconResolver;
    private final GuiSessionManager sessionManager;
    private final NumberFormatter numberFormatter;
    private final MessageService messageService;

    public MinionsOpenSubCommand(MinionManager minionManager, MinionUpgradeService minionUpgradeService,
                                  GuiConfig guiConfig, GuiIconResolver iconResolver, GuiSessionManager sessionManager,
                                  NumberFormatter numberFormatter, MessageService messageService) {
        this.minionManager = minionManager;
        this.minionUpgradeService = minionUpgradeService;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.sessionManager = sessionManager;
        this.numberFormatter = numberFormatter;
        this.messageService = messageService;
    }

    @Override
    public String getName() {
        return "minions";
    }

    @Override
    public String getPermission() {
        return "ecocore.minions.use";
    }

    @Override
    public String getUsage() {
        return "/ecocore minions";
    }

    @Override
    public String getDescription() {
        return "Buka menu Minions (upgrade/hapus minion milikmu).";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.render("general.player-only"));
            return;
        }

        new MinionGui(minionManager, minionUpgradeService, guiConfig.getMinionsGui(), iconResolver, sessionManager,
            numberFormatter, player).open(player);
    }
}
