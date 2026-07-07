package com.azthera.ecocore.minions;
 
import com.azthera.ecocore.config.MinionsConfig;
import com.azthera.ecocore.data.model.MinionData;
import com.azthera.ecocore.data.repository.MinionRepository;
import com.azthera.ecocore.economy.EconomyService;
import com.azthera.ecocore.economy.TransactionType;
import com.azthera.ecocore.util.Result;
 
import java.util.UUID;
 
/**
 * Handles Speed/Capacity/Fortune upgrade purchases for a minion, charging
 * the owner via {@code EconomyService} and enforcing each upgrade track's
 * configured max level. Upgrade cost scales geometrically per level so
 * later upgrades cost meaningfully more, discouraging trivial max-upgrading
 * without making it pay-to-win (cost is paid in the same survival currency
 * everyone earns, not a premium currency).
 */
public final class MinionUpgradeService {
 
    private static final double COST_GROWTH_PER_LEVEL = 1.6;
    private static final double BASE_UPGRADE_COST = 250.0;
 
    private final MinionRepository minionRepository;
    private final EconomyService economyService;
    private MinionsConfig minionsConfig;
 
    public MinionUpgradeService(MinionRepository minionRepository, EconomyService economyService,
                                 MinionsConfig minionsConfig) {
        this.minionRepository = minionRepository;
        this.economyService = economyService;
        this.minionsConfig = minionsConfig;
    }
 
    public void reload(MinionsConfig minionsConfig) {
        this.minionsConfig = minionsConfig;
    }
 
    public enum UpgradeTrack {
        SPEED, CAPACITY, FORTUNE
    }
 
    public double computeUpgradeCost(int currentLevel) {
        return BASE_UPGRADE_COST * Math.pow(COST_GROWTH_PER_LEVEL, currentLevel);
    }
 
    public Result<Integer> upgrade(UUID ownerId, MinionData minionData, UpgradeTrack track) {
        if (!minionData.getOwnerId().equals(ownerId)) {
            return Result.failure("Kamu bukan pemilik minion ini.");
        }
 
        int currentLevel = resolveCurrentLevel(minionData, track);
        int maxLevel = resolveMaxLevel(minionData, track);
 
        if (currentLevel >= maxLevel) {
            return Result.failure("Upgrade ini sudah mencapai level maksimum.");
        }
 
        double cost = computeUpgradeCost(currentLevel);
        if (!economyService.has(ownerId, cost)) {
            return Result.failure("Saldo kamu tidak cukup untuk upgrade ini.");
        }
 
        Result<Double> paymentResult = economyService.withdraw(ownerId, cost, TransactionType.ADMIN_TAKE,
            "Minion upgrade: " + track.name());
        if (paymentResult.isFailure()) {
            return Result.failure("Pembayaran gagal.");
        }
 
        int newLevel = currentLevel + 1;
        applyNewLevel(minionData, track, newLevel);
        minionRepository.save(minionData);
 
        return Result.success(newLevel);
    }
 
    private int resolveCurrentLevel(MinionData minionData, UpgradeTrack track) {
        return switch (track) {
            case SPEED -> minionData.getSpeedLevel();
            case CAPACITY -> minionData.getCapacityLevel();
            case FORTUNE -> minionData.getFortuneLevel();
        };
    }
 
    private int resolveMaxLevel(MinionData minionData, UpgradeTrack track) {
        MinionsConfig.MinionTypeDefinition definition = minionsConfig.getMinionTypeDefinitions()
            .get(minionData.getMinionType());
        if (definition == null) {
            return 0;
        }
        return switch (track) {
            case SPEED -> definition.maxUpgradeSpeedLevel();
            case CAPACITY -> definition.maxUpgradeCapacityLevel();
            case FORTUNE -> definition.maxUpgradeFortuneLevel();
        };
    }
 
    private void applyNewLevel(MinionData minionData, UpgradeTrack track, int newLevel) {
        switch (track) {
            case SPEED -> minionData.setSpeedLevel(newLevel);
            case CAPACITY -> minionData.setCapacityLevel(newLevel);
            case FORTUNE -> minionData.setFortuneLevel(newLevel);
        }
    }
 
    public double getActionIntervalSeconds(MinionData minionData) {
        MinionsConfig.MinionTypeDefinition definition = minionsConfig.getMinionTypeDefinitions()
            .get(minionData.getMinionType());
        double base = definition != null ? definition.baseActionIntervalSeconds() : 5.0;
        double speedReduction = minionData.getSpeedLevel() * 0.4;
        return Math.max(1.0, base - speedReduction);
    }
}
