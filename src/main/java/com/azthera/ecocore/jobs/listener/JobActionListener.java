package com.azthera.ecocore.jobs.listener;
 
import com.azthera.ecocore.jobs.JobProgressService;
import com.azthera.ecocore.jobs.JobType;
import com.azthera.ecocore.sell.AutoSellManager;
import com.azthera.ecocore.util.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
 
/**
 * Detects raw player actions (block break, entity kill, fish catch,
 * exploration into new chunks) and maps them to the appropriate
 * {@link JobType}, then delegates all XP/reward logic to
 * {@link JobProgressService}. Also triggers Auto Sell for qualifying drops
 * immediately after a job action completes.
 */
public final class JobActionListener implements Listener {
 
    private final JobProgressService jobProgressService;
    private final AutoSellManager autoSellManager;
    private final MessageService messageService;
 
    public JobActionListener(JobProgressService jobProgressService, AutoSellManager autoSellManager,
                              MessageService messageService) {
        this.jobProgressService = jobProgressService;
        this.autoSellManager = autoSellManager;
        this.messageService = messageService;
    }
 
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();
 
        JobType jobType = classifyBlockJob(material);
        if (jobType == null) {
            return;
        }
 
        JobProgressService.ActionResult result = jobProgressService.processAction(player.getUniqueId(), jobType);
        if (result.applied() && result.leveledUp()) {
            announceLevelUp(player, jobType, result.newLevel());
        }
 
        autoSellManager.tryAutoSell(player, material, 1);
    }
 
    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
 
        JobType jobType = classifyEntityJob(event.getEntity().getType());
        if (jobType == null) {
            return;
        }
 
        JobProgressService.ActionResult result = jobProgressService.processAction(killer.getUniqueId(), jobType);
        if (result.applied() && result.leveledUp()) {
            announceLevelUp(killer, jobType, result.newLevel());
        }
    }
 
    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
 
        JobProgressService.ActionResult result = jobProgressService.processAction(player.getUniqueId(), JobType.FISHING);
        if (result.applied() && result.leveledUp()) {
            announceLevelUp(player, JobType.FISHING, result.newLevel());
        }
    }
 
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        Player player = event.getPlayer();
 
        JobProgressService.ActionResult result = jobProgressService.processAction(player.getUniqueId(), JobType.EXPLORING);
        if (result.applied() && result.leveledUp()) {
            announceLevelUp(player, JobType.EXPLORING, result.newLevel());
        }
    }
 
    private JobType classifyBlockJob(Material material) {
        String name = material.name();
        if (name.contains("ORE") || name.contains("STONE") || name.contains("DEEPSLATE")) {
            return JobType.MINING;
        }
        if (name.contains("LOG") || name.contains("WOOD")) {
            return JobType.WOODCUTTING;
        }
        if (name.contains("CROP") || material == Material.WHEAT || material == Material.CARROTS
            || material == Material.POTATOES || material == Material.BEETROOTS) {
            return JobType.FARMING;
        }
        if (name.contains("PLANKS") || name.contains("BRICK") || name.contains("CONCRETE")) {
            return JobType.BUILDER;
        }
        return null;
    }
 
    private JobType classifyEntityJob(EntityType entityType) {
        return switch (entityType) {
            case ZOMBIE, SKELETON, SPIDER, CREEPER, ENDERMAN, WITCH -> JobType.HUNTING;
            default -> null;
        };
    }
 
    private void announceLevelUp(Player player, JobType jobType, int newLevel) {
        Component message = messageService.render("jobs.level-up",
            MessageService.placeholder("job", jobType.name()),
            MessageService.placeholder("level", String.valueOf(newLevel)));
        player.sendMessage(message);
    }
}
