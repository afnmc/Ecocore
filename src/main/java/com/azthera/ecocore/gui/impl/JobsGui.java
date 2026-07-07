package com.azthera.ecocore.gui.impl;
 
import com.azthera.ecocore.data.model.JobData;
import com.azthera.ecocore.gui.AbstractGui;
import com.azthera.ecocore.gui.GuiButton;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.jobs.JobManager;
import com.azthera.ecocore.jobs.JobRewardCalculator;
import com.azthera.ecocore.jobs.JobType;
import com.azthera.ecocore.jobs.PrestigeManager;
import com.azthera.ecocore.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
 
import java.util.EnumMap;
import java.util.Map;
 
/**
 * Displays every enabled job's current level, XP progress, and prestige
 * tier for the specific player who owns this GUI instance. One instance is
 * created per open (mirroring {@code ShopGui}'s pattern), so the owning
 * player never needs to be threaded through method parameters.
 * Shift-right-clicking a job attempts to prestige it via {@link PrestigeManager}.
 */
public final class JobsGui extends AbstractGui {
 
    private static final Map<JobType, Integer> JOB_SLOTS = new EnumMap<>(JobType.class);
 
    static {
        JOB_SLOTS.put(JobType.MINING, 10);
        JOB_SLOTS.put(JobType.WOODCUTTING, 12);
        JOB_SLOTS.put(JobType.FISHING, 14);
        JOB_SLOTS.put(JobType.FARMING, 16);
        JOB_SLOTS.put(JobType.HUNTING, 28);
        JOB_SLOTS.put(JobType.EXPLORING, 30);
        JOB_SLOTS.put(JobType.BUILDER, 32);
    }
 
    private final JobManager jobManager;
    private final PrestigeManager prestigeManager;
    private final JobRewardCalculator rewardCalculator;
    private final GuiIconResolver iconResolver;
    private final YamlConfiguration guiConfig;
    private final Player owner;
 
    public JobsGui(JobManager jobManager, PrestigeManager prestigeManager, JobRewardCalculator rewardCalculator,
                   YamlConfiguration guiConfig, GuiIconResolver iconResolver, GuiSessionManager sessionManager,
                   Player owner) {
        super(iconResolver.resolveRows(guiConfig, 5) * 9,
            Component.text(iconResolver.resolveTitle(guiConfig, "Jobs").replaceAll("<[^>]+>", "")), sessionManager);
        this.jobManager = jobManager;
        this.prestigeManager = prestigeManager;
        this.rewardCalculator = rewardCalculator;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.owner = owner;
    }
 
    @Override
    public void render() {
        clearButtons();
        fillBorder(iconResolver.resolveIcon(guiConfig, "border-filler"));
 
        for (JobType jobType : jobManager.getEnabledJobTypes()) {
            Integer slot = JOB_SLOTS.get(jobType);
            if (slot == null) {
                continue;
            }
            setButton(slot, GuiButton.of(buildJobIcon(jobType), (player, clickType) ->
                handleJobClick(player, jobType, clickType)));
        }
    }
 
    private void handleJobClick(Player player, JobType jobType, ClickType clickType) {
        if (clickType == ClickType.SHIFT_RIGHT) {
            JobData jobData = jobManager.getOrDefault(player.getUniqueId(), jobType);
            var result = prestigeManager.prestige(jobData);
            if (result.isSuccess()) {
                player.sendMessage(Component.text("Berhasil prestige " + jobType.name() + " ke tingkat " + result.orElse(0), NamedTextColor.LIGHT_PURPLE));
            } else {
                result.onFailure(reason -> player.sendMessage(Component.text(reason, NamedTextColor.RED)));
            }
        }
        render();
    }
 
    private ItemStack buildJobIcon(JobType jobType) {
        JobData data = jobManager.getOrDefault(owner.getUniqueId(), jobType);
        Material material = iconResolver.resolveMaterialOnly(guiConfig, jobType.name().toLowerCase(), Material.PAPER);
 
        double xpForCurrentLevel = rewardCalculator.computeXpForLevel(data.getLevel());
        boolean canPrestige = prestigeManager.canPrestige(data);
 
        ItemBuilder builder = ItemBuilder.of(material)
            .name(Component.text(jobType.name(), NamedTextColor.AQUA))
            .lore(Component.text("Level: " + data.getLevel(), NamedTextColor.YELLOW))
            .lore(Component.text("XP: " + String.format("%.0f", data.getExperience()) + " / " + String.format("%.0f", xpForCurrentLevel), NamedTextColor.GRAY))
            .lore(Component.text("Prestige: " + data.getPrestigeTier(), NamedTextColor.LIGHT_PURPLE));
 
        if (canPrestige) {
            builder.lore(Component.text("Shift-klik kanan untuk Prestige!", NamedTextColor.GREEN));
        }
 
        return builder.build();
    }
}
