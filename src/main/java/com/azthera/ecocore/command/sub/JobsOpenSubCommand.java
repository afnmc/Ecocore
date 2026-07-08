package com.azthera.ecocore.command.sub;

import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.config.GuiConfig;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.gui.impl.JobsGui;
import com.azthera.ecocore.jobs.JobManager;
import com.azthera.ecocore.jobs.JobRewardCalculator;
import com.azthera.ecocore.jobs.PrestigeManager;
import com.azthera.ecocore.util.MessageService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /ecocore jobs} — opens the Jobs GUI for the sender, where they can
 * see every enabled job's level/XP/prestige, join or leave individual jobs
 * (left-click), and prestige a maxed job (shift-right-click). Player-facing
 * only; the admin lookup command lives at {@code /ecocore jobsadmin}.
 */
public final class JobsOpenSubCommand implements SubCommand {

    private final JobManager jobManager;
    private final PrestigeManager prestigeManager;
    private final JobRewardCalculator jobRewardCalculator;
    private final GuiConfig guiConfig;
    private final GuiIconResolver iconResolver;
    private final GuiSessionManager sessionManager;
    private final MessageService messageService;

    public JobsOpenSubCommand(JobManager jobManager, PrestigeManager prestigeManager,
                               JobRewardCalculator jobRewardCalculator, GuiConfig guiConfig,
                               GuiIconResolver iconResolver, GuiSessionManager sessionManager,
                               MessageService messageService) {
        this.jobManager = jobManager;
        this.prestigeManager = prestigeManager;
        this.jobRewardCalculator = jobRewardCalculator;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.sessionManager = sessionManager;
        this.messageService = messageService;
    }

    @Override
    public String getName() {
        return "jobs";
    }

    @Override
    public String getPermission() {
        return "ecocore.jobs.use";
    }

    @Override
    public String getUsage() {
        return "/ecocore jobs";
    }

    @Override
    public String getDescription() {
        return "Buka menu Jobs (join/leave, lihat progress, prestige).";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageService.render("general.player-only"));
            return;
        }

        new JobsGui(jobManager, prestigeManager, jobRewardCalculator, guiConfig.getJobsGui(), iconResolver,
            messageService, sessionManager, player).open(player);
    }
}
