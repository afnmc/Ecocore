package com.azthera.ecocore.jobs;

import com.azthera.ecocore.config.JobsConfig;
import com.azthera.ecocore.data.model.JobData;
import com.azthera.ecocore.data.repository.JobDataRepository;
import com.azthera.ecocore.economy.EconomyService;
import com.azthera.ecocore.economy.TransactionType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Applies the result of a single job action end-to-end: verifies the player
 * has joined the job (no join = no progress), computes reward via
 * {@code JobRewardCalculator} (factoring in combo, prestige, bonus windows),
 * updates the player's cached {@code JobData} (XP, level, action count),
 * pays out money via {@code EconomyService}, grants configured level-up
 * rewards (flat money per level plus one-time milestone money/items), and
 * reports everything back via {@code ActionResult}. This is the single
 * entry point {@code JobActionListener} calls — it never touches the
 * repository or reward calculator directly.
 */
public final class JobProgressService {

    private final JobDataRepository jobDataRepository;
    private final JobRewardCalculator rewardCalculator;
    private final ComboTracker comboTracker;
    private final PrestigeManager prestigeManager;
    private final BonusScheduler bonusScheduler;
    private final EconomyService economyService;
    private JobsConfig jobsConfig;

    public JobProgressService(JobDataRepository jobDataRepository, JobRewardCalculator rewardCalculator,
                               ComboTracker comboTracker, PrestigeManager prestigeManager,
                               BonusScheduler bonusScheduler, EconomyService economyService, JobsConfig jobsConfig) {
        this.jobDataRepository = jobDataRepository;
        this.rewardCalculator = rewardCalculator;
        this.comboTracker = comboTracker;
        this.prestigeManager = prestigeManager;
        this.bonusScheduler = bonusScheduler;
        this.economyService = economyService;
        this.jobsConfig = jobsConfig;
    }

    public void reload(JobsConfig jobsConfig) {
        this.jobsConfig = jobsConfig;
    }

    /**
     * @return the ActionResult describing what changed. {@code applied} is
     * false if the job type is disabled in config, or the player has not
     * joined this job via the Jobs GUI — in both cases no XP/money is granted.
     */
    public ActionResult processAction(UUID playerId, JobType jobType) {
        JobsConfig.JobDefinition definition = jobsConfig.getJobDefinitions().get(jobType.name());
        if (definition == null || !definition.enabled()) {
            return ActionResult.notApplied();
        }

        JobData jobData = jobDataRepository.getCached(playerId, jobType.name())
            .orElseGet(() -> new JobData(playerId, jobType.name(), 1, 0.0, 0, 0L, 0L, false));

        if (!jobData.isJoined()) {
            return ActionResult.notApplied();
        }

        double comboMultiplier = comboTracker.registerActionAndGetMultiplier(playerId, jobType.name());
        boolean dailyBonus = bonusScheduler.isDailyBonusActive();
        boolean weeklyBonus = bonusScheduler.isWeeklyBonusActive();

        JobRewardCalculator.RewardResult reward = rewardCalculator.computeActionReward(
            definition, comboMultiplier, jobData.getPrestigeTier(), dailyBonus, weeklyBonus
        );

        int previousLevel = jobData.getLevel();
        double newExperience = jobData.getExperience() + reward.experience();
        int newLevel = rewardCalculator.computeLevelFromExperience(newExperience);

        jobData.setExperience(newExperience);
        jobData.setLevel(newLevel);
        jobData.setTotalActionsPerformed(jobData.getTotalActionsPerformed() + 1);
        jobData.setLastActionMillis(System.currentTimeMillis());
        jobDataRepository.save(jobData);

        economyService.deposit(playerId, reward.money(), TransactionType.JOB_REWARD,
            "Job reward: " + jobType.name());

        boolean leveledUp = newLevel > previousLevel;
        double levelUpBonusMoney = 0.0;
        List<ItemStack> levelUpItemRewards = new ArrayList<>();

        if (leveledUp) {
            for (int level = previousLevel + 1; level <= newLevel; level++) {
                levelUpBonusMoney += jobsConfig.getMoneyPerLevel();

                JobsConfig.LevelMilestone milestone = jobsConfig.getLevelMilestones().get(level);
                if (milestone != null) {
                    levelUpBonusMoney += milestone.money();
                    for (JobsConfig.ItemRewardDefinition itemDefinition : milestone.items()) {
                        Material material = Material.matchMaterial(itemDefinition.material());
                        if (material != null) {
                            levelUpItemRewards.add(new ItemStack(material, Math.max(1, itemDefinition.amount())));
                        }
                    }
                }
            }

            if (levelUpBonusMoney > 0) {
                economyService.deposit(playerId, levelUpBonusMoney, TransactionType.JOB_REWARD,
                    "Job level-up bonus: " + jobType.name());
            }
        }

        return new ActionResult(true, newLevel, previousLevel, leveledUp, reward.money(),
            levelUpBonusMoney, levelUpItemRewards);
    }

    public record ActionResult(boolean applied, int newLevel, int previousLevel, boolean leveledUp,
                                double moneyEarned, double levelUpBonusMoney, List<ItemStack> levelUpItemRewards) {

        public static ActionResult notApplied() {
            return new ActionResult(false, 0, 0, false, 0.0, 0.0, List.of());
        }
    }
         }
