package com.azthera.ecocore.command.sub;
 
import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.jobs.JobManager;
import com.azthera.ecocore.jobs.JobType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
 
import java.util.Arrays;
import java.util.List;
 
/**
 * {@code /eco jobs <player> <jobType>} — admin lookup of a player's job
 * level/XP/prestige, useful for support tickets without opening their GUI.
 */
public final class JobsSubCommand implements SubCommand {
 
    private final JobManager jobManager;
 
    public JobsSubCommand(JobManager jobManager) {
        this.jobManager = jobManager;
    }
 
    @Override
    public String getName() {
        return "jobs";
    }
 
    @Override
    public String getPermission() {
        return "ecocore.command.eco.jobs";
    }
 
    @Override
    public String getUsage() {
        return "/eco jobs <player> <jobType>";
    }
 
    @Override
    public String getDescription() {
        return "Lihat progress job pemain.";
    }
 
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: " + getUsage(), NamedTextColor.RED));
            return;
        }
 
        Player target = org.bukkit.Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Pemain tidak online.", NamedTextColor.RED));
            return;
        }
 
        JobType jobType;
        try {
            jobType = JobType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text("Job type tidak dikenal.", NamedTextColor.RED));
            return;
        }
 
        var data = jobManager.getOrDefault(target.getUniqueId(), jobType);
        sender.sendMessage(Component.text(target.getName() + " - " + jobType.name() + ": Level "
            + data.getLevel() + ", XP " + String.format("%.0f", data.getExperience())
            + ", Prestige " + data.getPrestigeTier(), NamedTextColor.YELLOW));
    }
 
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return org.bukkit.Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2) {
            return Arrays.stream(JobType.values()).map(Enum::name).toList();
        }
        return List.of();
    }
}
