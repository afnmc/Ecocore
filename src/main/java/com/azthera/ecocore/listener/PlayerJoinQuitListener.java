package com.azthera.ecocore.listener;
 
import com.azthera.ecocore.economy.EconomyService;
import com.azthera.ecocore.jobs.JobManager;
import com.azthera.ecocore.jobs.JobType;
import com.azthera.ecocore.minions.MinionManager;
import com.azthera.ecocore.quest.QuestManager;
import com.azthera.ecocore.sell.AutoSellManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
 
import java.util.List;
 
/**
 * Warms up every module's cache for a player on join (economy account,
 * job data, active quests, owned minions) and cleans up per-player cached
 * state on quit to avoid unbounded memory growth at 300-player scale.
 * Auto Sell state is also cleared on quit since it is a session-only toggle,
 * not persisted.
 */
public final class PlayerJoinQuitListener implements Listener {
 
    private final EconomyService economyService;
    private final JobManager jobManager;
    private final QuestManager questManager;
    private final MinionManager minionManager;
    private final AutoSellManager autoSellManager;
 
    public PlayerJoinQuitListener(EconomyService economyService, JobManager jobManager, QuestManager questManager,
                                   MinionManager minionManager, AutoSellManager autoSellManager) {
        this.economyService = economyService;
        this.jobManager = jobManager;
        this.questManager = questManager;
        this.minionManager = minionManager;
        this.autoSellManager = autoSellManager;
    }
 
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        economyService.ensureAccountLoaded(player.getUniqueId());
        jobManager.loadPlayerJobs(player.getUniqueId());
        questManager.loadPlayerQuests(player.getUniqueId());
        minionManager.loadPlayerMinions(player.getUniqueId());
    }
 
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
 
        List<String> jobIds = List.of(
            JobType.MINING.name(), JobType.WOODCUTTING.name(), JobType.FISHING.name(),
            JobType.FARMING.name(), JobType.HUNTING.name(), JobType.EXPLORING.name(), JobType.BUILDER.name()
        );
        jobManager.unloadPlayerJobs(player.getUniqueId());
 
        List<String> questInstanceIds = questManager.getActiveQuests(player.getUniqueId()).stream()
            .map(quest -> quest.getQuestInstanceId())
            .toList();
        questManager.unloadPlayerQuests(player.getUniqueId(), questInstanceIds);
 
        autoSellManager.disableAutoSell(player.getUniqueId());
    }
}
