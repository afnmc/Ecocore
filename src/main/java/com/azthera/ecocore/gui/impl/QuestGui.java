package com.azthera.ecocore.gui.impl;
 
import com.azthera.ecocore.data.model.QuestData;
import com.azthera.ecocore.gui.GuiButton;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.gui.PaginatedGui;
import com.azthera.ecocore.quest.QuestDefinition;
import com.azthera.ecocore.quest.QuestManager;
import com.azthera.ecocore.util.ItemBuilder;
import com.azthera.ecocore.util.NumberFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
 
import java.util.ArrayList;
import java.util.List;
 
/**
 * Lists the owning player's active/completed quest instances, paginated.
 * Each quest shows a text-based progress indicator and a claim hint once
 * completed; clicking a completed, unclaimed quest claims its reward via
 * {@link QuestManager#claimReward}.
 */
public final class QuestGui extends PaginatedGui {
 
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
 
    private final QuestManager questManager;
    private final GuiIconResolver iconResolver;
    private final NumberFormatter numberFormatter;
    private final YamlConfiguration guiConfig;
    private final Player owner;
    private final List<Integer> contentSlots;
 
    public QuestGui(QuestManager questManager, YamlConfiguration guiConfig, GuiIconResolver iconResolver,
                     GuiSessionManager sessionManager, NumberFormatter numberFormatter, Player owner) {
        super(iconResolver.resolveRows(guiConfig, 5) * 9,
            Component.text(iconResolver.resolveTitle(guiConfig, "Quests").replaceAll("<[^>]+>", "")),
            sessionManager, computeContentSlots(iconResolver.resolveRows(guiConfig, 5)).size(),
            PREVIOUS_PAGE_SLOT, NEXT_PAGE_SLOT);
        this.questManager = questManager;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.numberFormatter = numberFormatter;
        this.owner = owner;
        this.contentSlots = computeContentSlots(iconResolver.resolveRows(guiConfig, 5));
    }
 
    private static List<Integer> computeContentSlots(int rows) {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= rows - 2; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots;
    }
 
    @Override
    protected void renderStaticElements() {
        fillBorder(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).build());
    }
 
    @Override
    protected List<GuiButton> getAllContentButtons() {
        List<QuestData> activeQuests = questManager.getActiveQuests(owner.getUniqueId());
        List<GuiButton> buttons = new ArrayList<>();
 
        for (QuestData questData : activeQuests) {
            questManager.getQuestRegistry().getDefinition(questData.getQuestDefinitionId())
                .ifPresent(definition -> buttons.add(buildQuestButton(questData, definition)));
        }
        return buttons;
    }
 
    @Override
    protected List<Integer> getContentSlots() {
        return contentSlots;
    }
 
    private GuiButton buildQuestButton(QuestData questData, QuestDefinition definition) {
        return GuiButton.of(buildQuestIcon(questData, definition), (player, clickType) -> {
            if (questData.isCompleted() && !questData.isRewardClaimed()) {
                var result = questManager.claimReward(player.getUniqueId(), questData.getQuestInstanceId());
                if (result.isSuccess()) {
                    player.sendMessage(Component.text("Reward diklaim: " + numberFormatter.format(result.orElse(0.0)), NamedTextColor.GREEN));
                } else {
                    result.onFailure(reason -> player.sendMessage(Component.text(reason, NamedTextColor.RED)));
                }
            }
            render();
        });
    }
 
    private ItemStack buildQuestIcon(QuestData questData, QuestDefinition definition) {
        Material material = iconResolver.resolveMaterialOnly(guiConfig, definition.getType().name().toLowerCase(), Material.PAPER);
        if (questData.isCompleted() && !questData.isRewardClaimed()) {
            material = iconResolver.resolveMaterialOnly(guiConfig, "claim-reward", Material.GOLD_INGOT);
        }
 
        double progressFraction = questData.getRequiredProgress() > 0
            ? Math.min(1.0, questData.getProgress() / questData.getRequiredProgress())
            : 0.0;
        String progressBar = buildProgressBar(progressFraction);
 
        ItemBuilder builder = ItemBuilder.of(material)
            .name(Component.text(definition.getTitle(), NamedTextColor.LIGHT_PURPLE))
            .lore(Component.text(definition.getDescription(), NamedTextColor.GRAY))
            .lore(Component.text(progressBar, NamedTextColor.GREEN))
            .lore(Component.text(String.format("%.0f / %.0f", questData.getProgress(), questData.getRequiredProgress()), NamedTextColor.YELLOW));
 
        if (questData.isCompleted() && !questData.isRewardClaimed()) {
            builder.lore(Component.text("Klik untuk klaim reward!", NamedTextColor.GOLD));
        } else if (questData.isRewardClaimed()) {
            builder.lore(Component.text("Reward sudah diklaim.", NamedTextColor.DARK_GRAY));
        }
 
        return builder.build();
    }
 
    private String buildProgressBar(double fraction) {
        int totalBars = 20;
        int filledBars = (int) Math.round(fraction * totalBars);
        return "[" + "|".repeat(Math.max(0, filledBars)) + " ".repeat(Math.max(0, totalBars - filledBars)) + "]";
    }
 
    @Override
    protected GuiButton buildPreviousPageButton() {
        return GuiButton.display(ItemBuilder.of(Material.ARROW).name(Component.text("Halaman Sebelumnya")).build());
    }
 
    @Override
    protected GuiButton buildNextPageButton() {
        return GuiButton.display(ItemBuilder.of(Material.ARROW).name(Component.text("Halaman Berikutnya")).build());
    }
}
