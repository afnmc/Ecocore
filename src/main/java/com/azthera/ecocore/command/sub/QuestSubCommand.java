package com.azthera.ecocore.command.sub;

import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.quest.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /ecocore questadmin <player>} — admin lookup listing a player's
 * currently active quest instances and their progress. The player-facing
 * GUI opener lives at {@code /ecocore quest}.
 */
public final class QuestSubCommand implements SubCommand {

    private final QuestManager questManager;

    public QuestSubCommand(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public String getName() {
        return "questadmin";
    }

    @Override
    public String getPermission() {
        return "ecocore.command.eco.quest";
    }

    @Override
    public String getUsage() {
        return "/ecocore questadmin <player>";
    }

    @Override
    public String getDescription() {
        return "Lihat quest aktif pemain (admin).";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: " + getUsage(), NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Pemain tidak online.", NamedTextColor.RED));
            return;
        }

        var activeQuests = questManager.getActiveQuests(target.getUniqueId());
        if (activeQuests.isEmpty()) {
            sender.sendMessage(Component.text(target.getName() + " tidak memiliki quest aktif.", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text("=== Quest aktif " + target.getName() + " ===", NamedTextColor.LIGHT_PURPLE));
        for (var questData : activeQuests) {
            String status = questData.isCompleted() ? (questData.isRewardClaimed() ? "CLAIMED" : "COMPLETED") : "IN PROGRESS";
            sender.sendMessage(Component.text(questData.getQuestDefinitionId() + ": "
                + questData.getProgress() + "/" + questData.getRequiredProgress() + " [" + status + "]", NamedTextColor.YELLOW));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
