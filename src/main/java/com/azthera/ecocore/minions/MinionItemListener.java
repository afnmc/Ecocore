package com.azthera.ecocore.minions;

import com.azthera.ecocore.data.model.MinionData;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.Result;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinionItemListener implements Listener {
    private final JavaPlugin plugin;
    private final MinionManager minionManager;
    private final MessageService messageService;
    private final NamespacedKey minionKey;

    public MinionItemListener(JavaPlugin plugin, MinionManager minionManager, MessageService messageService) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.messageService = messageService;
        this.minionKey = new NamespacedKey(plugin, "minion_type");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType().isAir()) return;
        
        String minionTypeName = extractMinionType(handItem);
        if (minionTypeName == null) return;
        
        event.setCancelled(true);

        MinionType minionType;
        try {
            minionType = MinionType.valueOf(minionTypeName);
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Component.text("Minion spawn item ini rusak (tipe tidak dikenal: " + minionTypeName + ").", NamedTextColor.RED));
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            player.sendMessage(Component.text("Lokasi penempatan tidak valid.", NamedTextColor.RED));
            return;
        }
        BlockFace clickedFace = event.getBlockFace();
        Location spawnLocation = resolveSpawnLocation(clickedBlock, clickedFace);
        if (spawnLocation == null) {
            player.sendMessage(Component.text("Tidak ada ruang cukup untuk menempatkan minion di sini.", NamedTextColor.RED));
            return;
        }

        Result<MinionData> placeResult = minionManager.placeMinion(player, minionType, spawnLocation);
        if (placeResult.isFailure()) {
            placeResult.onFailure(reason -> player.sendMessage(Component.text(reason, NamedTextColor.RED)));
            return;
        }

        int currentAmount = handItem.getAmount();
        if (currentAmount <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            handItem.setAmount(currentAmount - 1);
        }

        spawnPlacementFeedback(spawnLocation);
        player.sendMessage(messageService.render("minions.spawn-success",
            MessageService.placeholder("type", formatMinionTypeName(minionType))));
    }

    private String extractMinionType(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(minionKey, PersistentDataType.STRING)) return null;
        return pdc.get(minionKey, PersistentDataType.STRING);
    }

    private Location resolveSpawnLocation(Block clickedBlock, BlockFace clickedFace) {
        Block targetBlock = clickedBlock.getRelative(clickedFace);
        if (targetBlock == null) return null;
        if (!targetBlock.getType().isAir() && !targetBlock.isPassable()) return null;
        Block floorBlock = targetBlock.getRelative(BlockFace.DOWN);
        if (floorBlock == null || !floorBlock.getType().isSolid()) return null;
        Block ceilingBlock = targetBlock.getRelative(BlockFace.UP);
        if (ceilingBlock != null && !ceilingBlock.isPassable()) return null;
        return targetBlock.getLocation().add(0.5, 0.0, 0.5);
    }

    private void spawnPlacementFeedback(Location location) {
        if (location.getWorld() == null) return;
        location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location.clone().add(0.5, 1.0, 0.5), 20, 0.3, 0.5, 0.3, 0.05);
        location.getWorld().playSound(location, Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.2f);
    }

    private String formatMinionTypeName(MinionType type) {
        String name = type.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}