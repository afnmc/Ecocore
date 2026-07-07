package com.azthera.ecocore.integration;
 
import com.azthera.ecocore.economy.EconomyService;
import com.azthera.ecocore.jobs.JobManager;
import com.azthera.ecocore.jobs.JobType;
import com.azthera.ecocore.minions.MinionManager;
import com.azthera.ecocore.quest.QuestManager;
import com.azthera.ecocore.util.NumberFormatter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
 
/**
 * Exposes EcoCore data to PlaceholderAPI under the {@code %ecocore_...%}
 * namespace, so other plugins (scoreboards, holograms, chat formatters) can
 * display balances, job levels, quest counts, and minion counts without a
 * direct EcoCore dependency. Registered only if PlaceholderAPI is present
 * (soft-depend), and unregistered cleanly on plugin disable.
 *
 * Supported placeholders:
 *  %ecocore_balance%                - primary currency balance
 *  %ecocore_job_<type>_level%       - job level (e.g. %ecocore_job_mining_level%)
 *  %ecocore_job_<type>_prestige%    - job prestige tier
 *  %ecocore_quest_active_count%     - number of active quest instances
 *  %ecocore_minion_count%           - number of placed minions
 */
public final class PlaceholderApiExpansion extends PlaceholderExpansion {
 
    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final JobManager jobManager;
    private final QuestManager questManager;
    private final MinionManager minionManager;
    private final NumberFormatter numberFormatter;
 
    public PlaceholderApiExpansion(JavaPlugin plugin, EconomyService economyService, JobManager jobManager,
                                    QuestManager questManager, MinionManager minionManager,
                                    NumberFormatter numberFormatter) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.jobManager = jobManager;
        this.questManager = questManager;
        this.minionManager = minionManager;
        this.numberFormatter = numberFormatter;
    }
 
    @Override
    public @NotNull String getIdentifier() {
        return "ecocore";
    }
 
    @Override
    public @NotNull String getAuthor() {
        return "Azthera";
    }
 
    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }
 
    @Override
    public boolean persist() {
        return true;
    }
 
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) {
            return "";
        }
 
        if (params.equalsIgnoreCase("balance")) {
            return numberFormatter.format(economyService.getBalance(player.getUniqueId()));
        }
 
        if (params.equalsIgnoreCase("quest_active_count")) {
            return String.valueOf(questManager.getActiveQuests(player.getUniqueId()).size());
        }
 
        if (params.equalsIgnoreCase("minion_count")) {
            return String.valueOf(minionManager.getOwnedMinions(player.getUniqueId()).size());
        }
 
        if (params.startsWith("job_") && params.endsWith("_level")) {
            String jobName = params.substring("job_".length(), params.length() - "_level".length());
            return resolveJobLevel(player, jobName);
        }
 
        if (params.startsWith("job_") && params.endsWith("_prestige")) {
            String jobName = params.substring("job_".length(), params.length() - "_prestige".length());
            return resolveJobPrestige(player, jobName);
        }
 
        return null;
    }
 
    private String resolveJobLevel(OfflinePlayer player, String jobName) {
        try {
            JobType jobType = JobType.valueOf(jobName.toUpperCase());
            return String.valueOf(jobManager.getOrDefault(player.getUniqueId(), jobType).getLevel());
        } catch (IllegalArgumentException exception) {
            return "0";
        }
    }
 
    private String resolveJobPrestige(OfflinePlayer player, String jobName) {
        try {
            JobType jobType = JobType.valueOf(jobName.toUpperCase());
            return String.valueOf(jobManager.getOrDefault(player.getUniqueId(), jobType).getPrestigeTier());
        } catch (IllegalArgumentException exception) {
            return "0";
        }
    }
}
