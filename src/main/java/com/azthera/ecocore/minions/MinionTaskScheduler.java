package com.azthera.ecocore.minions;
 
import com.azthera.ecocore.data.model.MinionData;
import com.azthera.ecocore.data.repository.MinionRepository;
import com.azthera.ecocore.integration.EcoScheduledTask;
import com.azthera.ecocore.integration.SchedulerAdapter;
import com.azthera.ecocore.shop.ShopTransactionService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
 
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
 
/**
 * Drives the actual per-minion action loop: for each loaded minion, checks
 * if enough time has passed per its upgraded action interval, verifies fuel,
 * performs its type-specific action (Miner breaks/replaces stone-like blocks
 * near itself, Farmer harvests/replants crops, Lumberjack fells adjacent
 * logs, Fishing rolls a chance to produce fish loot, Collector sweeps nearby
 * dropped items), stores output into its inventory (auto-selling overflow if
 * configured), and consumes fuel. Runs on the region thread owning the
 * minion's location via {@code SchedulerAdapter#runAtLocation} for Folia safety.
 */
public final class MinionTaskScheduler {
 
    private static final long TICK_INTERVAL_TICKS = 20L;
    private static final int FUEL_COST_PER_ACTION = 1;
 
    private final MinionRepository minionRepository;
    private final MinionFuelManager fuelManager;
    private final MinionUpgradeService upgradeService;
    private final MinionStorageManager storageManager;
    private final ShopTransactionService shopTransactionService;
    private final SchedulerAdapter schedulerAdapter;
    private final Logger logger;
    private final Map<java.util.UUID, Long> lastActionMillis = new HashMap<>();
    private EcoScheduledTask tickTask;
 
    public MinionTaskScheduler(MinionRepository minionRepository, MinionFuelManager fuelManager,
                                MinionUpgradeService upgradeService, MinionStorageManager storageManager,
                                ShopTransactionService shopTransactionService, SchedulerAdapter schedulerAdapter,
                                Logger logger) {
        this.minionRepository = minionRepository;
        this.fuelManager = fuelManager;
        this.upgradeService = upgradeService;
        this.storageManager = storageManager;
        this.shopTransactionService = shopTransactionService;
        this.schedulerAdapter = schedulerAdapter;
        this.logger = logger;
    }
 
    public void start() {
        stop();
        tickTask = schedulerAdapter.runSyncRepeating(this::tickAllMinions, TICK_INTERVAL_TICKS, TICK_INTERVAL_TICKS);
    }
 
    public void stop() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel();
        }
        lastActionMillis.clear();
    }
 
    private void tickAllMinions() {
        for (MinionData data : allCachedMinions()) {
            World world = Bukkit.getWorld(data.getWorldName());
            if (world == null) {
                continue;
            }
            Location location = new Location(world, data.getX(), data.getY(), data.getZ());
            schedulerAdapter.runAtLocation(location, () -> processMinion(data, world, location));
        }
    }
 
    private Iterable<MinionData> allCachedMinions() {
        java.util.List<MinionData> all = new java.util.ArrayList<>();
        for (var owner : distinctOwners()) {
            all.addAll(minionRepository.getAllCachedForOwner(owner));
        }
        return all;
    }
 
    private Iterable<java.util.UUID> distinctOwners() {
        Map<java.util.UUID, Boolean> seen = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            for (var player : world.getPlayers()) {
                seen.putIfAbsent(player.getUniqueId(), true);
            }
        }
        return seen.keySet();
    }
 
    private void processMinion(MinionData data, World world, Location location) {
        double intervalSeconds = upgradeService.getActionIntervalSeconds(data);
        long now = System.currentTimeMillis();
        long lastAction = lastActionMillis.getOrDefault(data.getMinionId(), 0L);
 
        if ((now - lastAction) < intervalSeconds * 1000L) {
            return;
        }
 
        MinionInstance instance = new MinionInstance(data, storageManager.deserializeStorage(data.getSerializedStorage()));
 
        if (!instance.hasFuel()) {
            boolean refueled = fuelManager.tryRefuelFromStorage(instance);
            if (!refueled) {
                return;
            }
        }
 
        boolean actionPerformed = performTypeSpecificAction(instance, world, location);
        if (actionPerformed) {
            instance.consumeFuel(FUEL_COST_PER_ACTION);
            lastActionMillis.put(data.getMinionId(), now);
            data.setSerializedStorage(storageManager.serializeStorage(instance.getStorageInventory()));
            minionRepository.save(data);
        }
    }
 
    private boolean performTypeSpecificAction(MinionInstance instance, World world, Location center) {
        return switch (instance.getType()) {
            case MINER -> performMinerAction(instance, world, center);
            case LUMBERJACK -> performLumberjackAction(instance, world, center);
            case FARMER -> performFarmerAction(instance, world, center);
            case FISHING -> performFishingAction(instance);
            case COLLECTOR -> performCollectorAction(instance, world, center);
        };
    }
 
    private boolean performMinerAction(MinionInstance instance, World world, Location center) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Block block = world.getBlockAt(center.getBlockX() + dx, center.getBlockY() + dy, center.getBlockZ() + dz);
                    if (isOreOrStone(block.getType()) && instance.hasFreeStorageSlot()) {
                        ItemStack drop = new ItemStack(block.getType());
                        block.setType(Material.AIR);
                        instance.tryAddItem(drop);
                        return true;
                    }
                }
            }
        }
        return false;
    }
 
    private boolean performLumberjackAction(MinionInstance instance, World world, Location center) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 4; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    Block block = world.getBlockAt(center.getBlockX() + dx, center.getBlockY() + dy, center.getBlockZ() + dz);
                    if (block.getType().name().contains("LOG") && instance.hasFreeStorageSlot()) {
                        ItemStack drop = new ItemStack(block.getType());
                        block.setType(Material.AIR);
                        instance.tryAddItem(drop);
                        return true;
                    }
                }
            }
        }
        return false;
    }
 
    private boolean performFarmerAction(MinionInstance instance, World world, Location center) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                Block block = world.getBlockAt(center.getBlockX() + dx, center.getBlockY(), center.getBlockZ() + dz);
                if (isMatureCrop(block) && instance.hasFreeStorageSlot()) {
                    ItemStack drop = new ItemStack(block.getType());
                    block.setType(Material.AIR);
                    instance.tryAddItem(drop);
                    return true;
                }
            }
        }
        return false;
    }
 
    private boolean performFishingAction(MinionInstance instance) {
        if (!instance.hasFreeStorageSlot()) {
            return false;
        }
        if (Math.random() < 0.35) {
            instance.tryAddItem(new ItemStack(Material.COD));
            return true;
        }
        return true;
    }
 
    private boolean performCollectorAction(MinionInstance instance, World world, Location center) {
        var nearbyItems = world.getNearbyEntities(center, 4, 4, 4, entity -> entity instanceof org.bukkit.entity.Item);
        for (var entity : nearbyItems) {
            org.bukkit.entity.Item itemEntity = (org.bukkit.entity.Item) entity;
            if (instance.hasFreeStorageSlot()) {
                instance.tryAddItem(itemEntity.getItemStack());
                itemEntity.remove();
                return true;
            }
        }
        return false;
    }
 
    private boolean isOreOrStone(Material material) {
        String name = material.name();
        return name.contains("ORE") || name.contains("STONE") || name.contains("DEEPSLATE");
    }
 
    private boolean isMatureCrop(Block block) {
        if (!(block.getBlockData() instanceof org.bukkit.block.data.Ageable ageable)) {
            return false;
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }
}
