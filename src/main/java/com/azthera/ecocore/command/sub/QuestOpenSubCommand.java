package com.azthera.ecocore.command.sub;

import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.config.GuiConfig;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.gui.impl.QuestGui;
import com.azthera.ecocore.quest.QuestManager;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.NumberFormatter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /ecocore quest} — opens the Quest GUI for the sender, listing their
 * currently active/completed quest instances and letting them claim
 * finished rewards. Player-facing only; the admin lookup command lives at
 * {@code /ecocore questadmin}.
 */
public final class QuestOpenSubCommand implements SubCommand {

    private final QuestManager questManager;
    private final GuiConfig guiConfig;
    private final GuiIconResolver iconResolver;
    private final GuiSessionManager sessionManager;
    private final NumberFormatter numberFormatter;
    private final MessageService messageService;

    public QuestOpenSubCommand(QuestManager questManager, GuiConfig guiConfig, GuiIconResolver iconResolver,
                                GuiSessionManager sessionManager, NumberFormatter numberFormatter,
                                MessageService messageService) {
        this.questManager = questManager;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.sessionManager = sessionManager;
        this.numberFormatter = numberFormatter;
        this.messageService = messageService;
    }

    @Override
    public String getName() {
        return "quest";
    }

    @Override
    public String getPermission() {
        return "ecocore.quest.use";
    }

    @Override
    public String getUsage() {
        return "/ecocore quest";
    }

    @Override
    public String getDescription() {
        return "Buka menu Quest (lihat progress, klaim reward).";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.render("general.player-only"));
            return;
        }

        new QuestGui(questManager, guiConfig.getQuestGui(), iconResolver, sessionManager, numberFormatter, player)
            .open(player);
    }
}
