package com.azthera.ecocore.jobs;
 
import com.azthera.ecocore.config.JobsConfig;
 
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
 
/**
 * Computes the final XP and monetary reward for a single job action,
 * layering the job's base values, the player's combo multiplier, prestige
 * reward bonus, and any active daily/weekly bonus window on top of each other.
 * Pure calculation — does not touch any repository or apply anything itself.
 */
public final class JobRewardCalculator {
 
    private JobsConfig jobsConfig;
 
    public JobRewardCalculator(JobsConfig jobsConfig) {
        this.jobsConfig = jobsConfig;
    }
 
    public void reload(JobsConfig jobsConfig) {
        this.jobsConfig = jobsConfig;
    }
 
    public double computeXpForLevel(int level) {
        return jobsConfig.getXpCurveBase() * Math.pow(jobsConfig.getXpCurveMultiplier(), level - 1);
    }
 
    public int computeLevelFromExperience(double totalExperience) {
        int level = 1;
        double xpNeeded = computeXpForLevel(level);
        double remaining = totalExperience;
 
        while (remaining >= xpNeeded && level < jobsConfig.getMaxLevel()) {
            remaining -= xpNeeded;
            level++;
            xpNeeded = computeXpForLevel(level);
        }
        return level;
    }
 
    public RewardResult computeActionReward(JobsConfig.JobDefinition jobDefinition, double comboMultiplier,
                                              int prestigeTier, boolean isDailyBonusActive, boolean isWeeklyBonusActive) {
        double xp = jobDefinition.baseXpPerAction() * comboMultiplier;
        double money = jobDefinition.baseRewardPerAction() * comboMultiplier;
 
        double prestigeMultiplier = 1.0 + (prestigeTier * jobsConfig.getPrestigeRewardMultiplierStep());
        xp *= prestigeMultiplier;
        money *= prestigeMultiplier;
 
        if (isDailyBonusActive) {
            xp *= jobsConfig.getDailyBonusMultiplier();
            money *= jobsConfig.getDailyBonusMultiplier();
        }
        if (isWeeklyBonusActive) {
            xp *= jobsConfig.getWeeklyBonusMultiplier();
            money *= jobsConfig.getWeeklyBonusMultiplier();
        }
 
        return new RewardResult(xp, money);
    }
 
    public boolean isWeeklyBonusWindow(ZonedDateTime now) {
        return now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
 
    public record RewardResult(double experience, double money) {
    }
}
