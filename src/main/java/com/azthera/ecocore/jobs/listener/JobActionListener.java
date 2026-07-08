package com.azthera.ecocore.jobs.listener;

import com.azthera.ecocore.jobs.JobProgressService;
import com.azthera.ecocore.jobs.JobType;
import com.azthera.ecocore.quest.QuestManager;
import com.azthera.ecocore.sell.AutoSellManager;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.NumberFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Detects raw player actions (block break, entity kill, fish catch,
 * exploration into new chunks) and:
 *  1) maps them to the appropriate {@link JobType} and delegates XP/reward
 *     logic to {@link JobProgressService} (which itself gates on job join
 *     state and grants level-up rewards);
 *  2) independently reports the same actions to {@code QuestManager} as
 *     objective progress (e.g. "mine:stone", "kill:zombie", "fish:catch",
 *     "explore:chunk"), regardless of job join state, since quest progress
 *     is a separate reward track from jobs and must always tick forward.
 * Also triggers Auto Sell for qualifying drops immediately after a job
 * action completes, and hands the player any level-up item rewards.
 */
public final class JobActionListener implements Listener {

    private final JobProgressService jobProgressService;
    private final AutoSellManager autoSellManager;
    private final MessageService messageService;
    private final QuestManager questManager;
    private final NumberFormatter numberFormatter;

    public JobActionListener(JobProgressService jobProgressService, AutoSellManager autoSellManager,
                              MessageService messageService, QuestManager questManager,
                              NumberFormatter numberFormatter) {
        this.jobProgressService = jobProgressService;
        this.autoSellManager = autoSellManager;
        this.messageService = messageService;
        this.questManager = questManager;
        this.numberFormatter = numberFormatter;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();

        JobType jobType = classifyBlockJob(material);
        if (jobType != null) {
            String prefix = objectivePrefixForJob(jobType);
            String materialKey = material.name().toLowerCase(Locale.ROOT);
            questManager.recordProgress(player.getUniqueId(), prefix + ":" + materialKey, 1.0);
            questManager.recordProgress(player.getUniqueId(), prefix + ":any", 1.0);

            JobProgressService.ActionResult result = jobProgressService.processAction(player.getUniqueId(), jobType);
            if (result.applied() && result.leveledUp()) {
                announceLevelUp(player, jobType, result);
            }

            autoSellManager.tryAutoSell(player, material, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        String entityKey = event.getEntity().getType().name().toLowerCase(Locale.ROOT);
        questManager.recordProgress(killer.getUniqueId(), "kill:" + entityKey, 1.0);
        questManager.recordProgress(killer.getUniqueId(), "kill:any", 1.0);

        JobType jobType = classifyEntityJob(event.getEntity().getType());
        if (jobType == null) {
            return;
        }

        JobProgressService.ActionResult result = jobProgressService.processAction(killer.getUniqueId(), jobType);
        if (result.applied() && result.leveledUp()) {
            announceLevelUp(killer, jobType, result);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();

        questManager.recordProgress(player.getUniqueId(), "fish:catch", 1.0);

        JobProgressService.ActionResult result = jobProgressService.processAction(player.getUniqueId(), JobType.FISHING);
        if (result.applied() && result.leveledUp()) {
            announceLevelUp(player, JobType.FISHING, result);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        Player player = event.getPlayer();

        questManager.recordProgress(player.getUniqueId(), "explore:chunk", 1.0);

        JobProgressService.ActionResult result = jobProgressService.processAction(player.getUniqueId(), JobType.EXPLORING);
        if (result.applied() && result.leveledUp()) {
            announceLevelUp(player, JobType.EXPLORING, result);
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

    private String objectivePrefixForJob(JobType jobType) {
        return switch (jobType) {
            case MINING -> "mine";
            case WOODCUTTING -> "woodcut";
            case FARMING -> "farm";
            case BUILDER -> "build";
            default -> jobType.name().toLowerCase(Locale.ROOT);
        };
    }

    private void announceLevelUp(Player player, JobType jobType, JobProgressService.ActionResult result) {
        Component message = messageService.render("jobs.level-up",
            MessageService.placeholder("job", jobType.name()),
            MessageService.placeholder("level", String.valueOf(result.newLevel())));
        player.sendMessage(message);

        if (result.levelUpBonusMoney() > 0) {
            Component bonusMessage = messageService.render("jobs.level-up-reward",
                MessageService.placeholder("job", jobType.name()),
                MessageService.placeholder("amount", numberFormatter.format(result.levelUpBonusMoney())));
            player.sendMessage(bonusMessage);
        }

        if (!result.levelUpItemRewards().isEmpty()) {
            Map<Material, Integer> summary = new HashMap<>();
            for (ItemStack item : result.levelUpItemRewards()) {
                var leftover = player.getInventory().addItem(item);
                for (ItemStack overflow : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflow);
                }
                summary.merge(item.getType(), item.getAmount(), Integer::sum);
            }

            StringBuilder itemSummary = new StringBuilder();
            for (var entry : summary.entrySet()) {
                if (!itemSummary.isEmpty()) {
                    itemSummary.append(", ");
                }
                itemSummary.append(entry.getValue()).append("x ").append(entry.getKey().name());
            }
            player.sendMessage(Component.text("Item reward level up: " + itemSummary, NamedTextColor.GOLD));
        }
    }
 }
