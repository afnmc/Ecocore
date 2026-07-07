package com.azthera.ecocore.minions;
 
import com.azthera.ecocore.bootstrap.Module;
import com.azthera.ecocore.config.MinionsConfig;
import com.azthera.ecocore.data.model.MinionData;
import com.azthera.ecocore.data.repository.MinionRepository;
import com.azthera.ecocore.util.Result;
import org.bukkit.Location;
import org.bukkit.entity.Player;
 
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
 
/**
 * Lifecycle owner of the Minions module. Can be entirely disabled via
 * {@code MinionsConfig#isModuleEnabled} without breaking any other module —
 * no other package references classes here directly, only through this
 * manager, satisfying the requirement that a third-party minion plugin could
 * be used instead without side effects.
 */
public final class MinionManager implements Module {
 
    private final MinionRepository minionRepository;
    private final MinionLimitPolicy limitPolicy;
    private final MinionStorageManager storageManager;
    private final MinionTaskScheduler taskScheduler;
    private MinionsConfig minionsConfig;
    private final Logger logger;
    private boolean enabled;
 
    public MinionManager(MinionRepository minionRepository, MinionLimitPolicy limitPolicy,
                          MinionStorageManager storageManager, MinionTaskScheduler taskScheduler,
                          MinionsConfig minionsConfig, Logger logger) {
        this.minionRepository = minionRepository;
        this.limitPolicy = limitPolicy;
        this.storageManager = storageManager;
        this.taskScheduler = taskScheduler;
        this.minionsConfig = minionsConfig;
        this.logger = logger;
    }
 
    @Override
    public String getName() {
        return "minions";
    }
 
    @Override
    public void enable() {
        this.enabled = minionsConfig.isModuleEnabled();
        if (!enabled) {
            logger.info("Minions module is disabled in config; skipping startup.");
            return;
        }
        taskScheduler.start();
        logger.info("Minions module enabled.");
    }
 
    @Override
    public void disable() {
        this.enabled = false;
        taskScheduler.stop();
    }
 
    @Override
    public void reload() {
        disable();
        enable();
    }
 
    @Override
    public boolean isEnabled() {
        return enabled;
    }
 
    public void loadPlayerMinions(UUID playerId) {
        if (!enabled) {
            return;
        }
        minionRepository.loadForOwnerAsync(playerId);
    }
 
    public Result<MinionData> placeMinion(Player player, MinionType type, Location location) {
        if (!enabled) {
            return Result.failure("Modul minion sedang tidak aktif.");
        }
        if (!limitPolicy.canPlaceAnother(player)) {
            return Result.failure("Kamu sudah mencapai batas maksimum minion.");
        }
 
        MinionsConfig.MinionTypeDefinition typeDefinition = minionsConfig.getMinionTypeDefinitions().get(type.name());
        if (typeDefinition == null || !typeDefinition.enabled()) {
            return Result.failure("Tipe minion ini tidak diaktifkan.");
        }
 
        var emptyStorage = storageManager.createEmptyStorage(type);
        byte[] serializedEmpty = storageManager.serializeStorage(emptyStorage);
 
        MinionData minionData = new MinionData(
            UUID.randomUUID(), player.getUniqueId(), type.name(), location.getWorld().getName(),
            location.getX(), location.getY(), location.getZ(),
            0, 0, 0, minionsConfig.getBaseFuelCapacity(), serializedEmpty
        );
 
        minionRepository.insert(minionData);
        return Result.success(minionData);
    }
 
    public Result<Void> removeMinion(Player player, UUID minionId) {
        if (!enabled) {
            return Result.failure("Modul minion sedang tidak aktif.");
        }
 
        Optional<MinionData> dataOpt = minionRepository.getCached(minionId);
        if (dataOpt.isEmpty()) {
            return Result.failure("Minion tidak ditemukan.");
        }
        if (!dataOpt.get().getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("ecocore.minions.bypasslimit")) {
            return Result.failure("Kamu bukan pemilik minion ini.");
        }
 
        minionRepository.delete(minionId);
        return Result.success(null);
    }
 
    public List<MinionData> getOwnedMinions(UUID ownerId) {
        return minionRepository.getAllCachedForOwner(ownerId);
    }
 
    public MinionLimitPolicy getLimitPolicy() {
        return limitPolicy;
    }
 
    public void updateMinionsConfig(MinionsConfig minionsConfig) {
        this.minionsConfig = minionsConfig;
        limitPolicy.reload(minionsConfig);
        storageManager.reload(minionsConfig);
    }
}
