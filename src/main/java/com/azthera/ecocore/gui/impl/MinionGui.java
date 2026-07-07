package com.azthera.ecocore.gui.impl;
 
import com.azthera.ecocore.data.model.MinionData;
import com.azthera.ecocore.gui.AbstractGui;
import com.azthera.ecocore.gui.GuiButton;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.minions.MinionManager;
import com.azthera.ecocore.minions.MinionType;
import com.azthera.ecocore.minions.MinionUpgradeService;
import com.azthera.ecocore.util.ItemBuilder;
import com.azthera.ecocore.util.NumberFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
 
import java.util.List;
 
/**
 * Shows up to the player's minion limit worth of owned minions as clickable
 * icons. Left-click upgrades Speed, right-click upgrades Capacity,
 * shift-left-click upgrades Fortune, shift-right-click removes the minion.
 * Fixed 5-slot layout matches the default {@code max-per-player} of 5;
 * servers raising that limit significantly beyond the visible row would
 * need a paginated variant, which is a straightforward future extension.
 */
public final class MinionGui extends AbstractGui {
 
    private static final int[] MINION_SLOTS = {10, 12, 14, 16, 18};
    private static final int INFO_SLOT = 22;
 
    private final MinionManager minionManager;
    private final MinionUpgradeService upgradeService;
    private final NumberFormatter numberFormatter;
    private final GuiIconResolver iconResolver;
    private final YamlConfiguration guiConfig;
    private final Player owner;
 
    public MinionGui(MinionManager minionManager, MinionUpgradeService upgradeService, YamlConfiguration guiConfig,
                      GuiIconResolver iconResolver, GuiSessionManager sessionManager, NumberFormatter numberFormatter,
                      Player owner) {
        super(iconResolver.resolveRows(guiConfig, 4) * 9,
            Component.text(iconResolver.resolveTitle(guiConfig, "Minions").replaceAll("<[^>]+>", "")), sessionManager);
        this.minionManager = minionManager;
        this.upgradeService = upgradeService;
        this.numberFormatter = numberFormatter;
        this.iconResolver = iconResolver;
        this.guiConfig = guiConfig;
        this.owner = owner;
    }
 
    @Override
    public void render() {
        clearButtons();
        fillBorder(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).build());
 
        List<MinionData> owned = minionManager.getOwnedMinions(owner.getUniqueId());
 
        for (int i = 0; i < MINION_SLOTS.length; i++) {
            if (i < owned.size()) {
                MinionData minionData = owned.get(i);
                setButton(MINION_SLOTS[i], GuiButton.of(buildMinionIcon(minionData), (player, clickType) ->
                    handleMinionClick(player, minionData, clickType)));
            } else {
                setButton(MINION_SLOTS[i], GuiButton.display(buildEmptySlotIcon()));
            }
        }
 
        setButton(INFO_SLOT, GuiButton.display(buildInfoIcon(owned.size())));
    }
 
    private void handleMinionClick(Player player, MinionData minionData, ClickType clickType) {
        if (clickType == ClickType.SHIFT_RIGHT) {
            var result = minionManager.removeMinion(player, minionData.getMinionId());
            result.onFailure(reason -> player.sendMessage(Component.text(reason, NamedTextColor.RED)));
            render();
            return;
        }
 
        MinionUpgradeService.UpgradeTrack track = switch (clickType) {
            case SHIFT_LEFT -> MinionUpgradeService.UpgradeTrack.FORTUNE;
            case RIGHT -> MinionUpgradeService.UpgradeTrack.CAPACITY;
            default -> MinionUpgradeService.UpgradeTrack.SPEED;
        };
 
        var result = upgradeService.upgrade(player.getUniqueId(), minionData, track);
        if (result.isSuccess()) {
            player.sendMessage(Component.text(track.name() + " upgraded to level " + result.orElse(0), NamedTextColor.GREEN));
        } else {
            result.onFailure(reason -> player.sendMessage(Component.text(reason, NamedTextColor.RED)));
        }
        render();
    }
 
    private ItemStack buildMinionIcon(MinionData minionData) {
        MinionType type = MinionType.valueOf(minionData.getMinionType());
        Material material = iconResolver.resolveMaterialOnly(guiConfig, type.name().toLowerCase(), Material.VILLAGER_SPAWN_EGG);
 
        double nextSpeedCost = upgradeService.computeUpgradeCost(minionData.getSpeedLevel());
        double nextCapacityCost = upgradeService.computeUpgradeCost(minionData.getCapacityLevel());
        double nextFortuneCost = upgradeService.computeUpgradeCost(minionData.getFortuneLevel());
 
        return ItemBuilder.of(material)
            .name(Component.text(type.name() + " Minion", NamedTextColor.GOLD))
            .lore(Component.text("Speed Lv" + minionData.getSpeedLevel() + " (klik kiri, biaya " + numberFormatter.format(nextSpeedCost) + ")", NamedTextColor.YELLOW))
            .lore(Component.text("Capacity Lv" + minionData.getCapacityLevel() + " (klik kanan, biaya " + numberFormatter.format(nextCapacityCost) + ")", NamedTextColor.AQUA))
            .lore(Component.text("Fortune Lv" + minionData.getFortuneLevel() + " (shift-kiri, biaya " + numberFormatter.format(nextFortuneCost) + ")", NamedTextColor.LIGHT_PURPLE))
            .lore(Component.text("Fuel: " + minionData.getFuelRemaining(), NamedTextColor.GRAY))
            .lore(Component.text("Shift-kanan untuk hapus minion.", NamedTextColor.RED))
            .build();
    }
 
    private ItemStack buildEmptySlotIcon() {
        return ItemBuilder.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            .name(Component.text("Slot kosong", NamedTextColor.DARK_GRAY))
            .build();
    }
 
    private ItemStack buildInfoIcon(int currentCount) {
        int maxSlots = minionManager.getLimitPolicy().getMaxMinionsPerPlayer();
        return ItemBuilder.of(Material.BOOK)
            .name(Component.text("Info Minion", NamedTextColor.GOLD))
            .lore(Component.text("Minion terpasang: " + currentCount + " / " + maxSlots, NamedTextColor.YELLOW))
            .build();
    }
}
