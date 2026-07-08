package com.azthera.ecocore.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed view over modules/jobs.yml: the shared XP curve, combo window,
 * daily/weekly bonus multipliers, prestige bounds, per-job base values, and
 * the configurable per-level-up reward table (flat money-per-level plus
 * one-time milestone rewards at specific levels).
 */
public final class JobsConfig {

    private double xpCurveBase;
    private double xpCurveMultiplier;
    private int maxLevel;
    private long comboWindowMillis;
    private double comboMaxMultiplier;
    private double dailyBonusMultiplier;
    private double weeklyBonusMultiplier;
    private long dailyResetHourOfDay;
    private int maxPrestige;
    private double prestigeRewardMultiplierStep;
    private double moneyPerLevel;
    private final Map<String, JobDefinition> jobDefinitions = new HashMap<>();
    private final Map<Integer, LevelMilestone> levelMilestones = new HashMap<>();

    public JobsConfig(YamlConfiguration source) {
        load(source);
    }

    public void load(YamlConfiguration source) {
        this.xpCurveBase = source.getDouble("xp-curve.base", 100.0);
        this.xpCurveMultiplier = source.getDouble("xp-curve.multiplier", 1.12);
        this.maxLevel = source.getInt("max-level", 100);
        this.comboWindowMillis = source.getLong("combo.window-millis", 4000L);
        this.comboMaxMultiplier = source.getDouble("combo.max-multiplier", 2.0);
        this.dailyBonusMultiplier = source.getDouble("bonus.daily-multiplier", 1.25);
        this.weeklyBonusMultiplier = source.getDouble("bonus.weekly-multiplier", 1.5);
        this.dailyResetHourOfDay = source.getLong("bonus.daily-reset-hour", 0L);
        this.maxPrestige = source.getInt("prestige.max", 5);
        this.prestigeRewardMultiplierStep = source.getDouble("prestige.reward-multiplier-step", 0.1);

        jobDefinitions.clear();
        ConfigurationSection jobsSection = source.getConfigurationSection("jobs");
        if (jobsSection != null) {
            for (String jobId : jobsSection.getKeys(false)) {
                ConfigurationSection jobSection = jobsSection.getConfigurationSection(jobId);
                if (jobSection == null) {
                    continue;
                }
                jobDefinitions.put(jobId.toUpperCase(), new JobDefinition(
                    jobId.toUpperCase(),
                    jobSection.getBoolean("enabled", true),
                    jobSection.getDouble("base-xp-per-action", 5.0),
                    jobSection.getDouble("base-reward-per-action", 2.0)
                ));
            }
        }

        loadLevelRewards(source);
    }

    private void loadLevelRewards(YamlConfiguration source) {
        levelMilestones.clear();
        this.moneyPerLevel = 0.0;

        ConfigurationSection levelRewardsSection = source.getConfigurationSection("level-rewards");
        if (levelRewardsSection == null) {
            return;
        }

        this.moneyPerLevel = levelRewardsSection.getDouble("money-per-level", 0.0);

        ConfigurationSection milestonesSection = levelRewardsSection.getConfigurationSection("milestones");
        if (milestonesSection == null) {
            return;
        }

        for (String levelKey : milestonesSection.getKeys(false)) {
            ConfigurationSection milestoneSection = milestonesSection.getConfigurationSection(levelKey);
            if (milestoneSection == null) {
                continue;
            }

            int level;
            try {
                level = Integer.parseInt(levelKey.trim());
            } catch (NumberFormatException exception) {
                continue;
            }

            double milestoneMoney = milestoneSection.getDouble("money", 0.0);
            List<ItemRewardDefinition> items = new ArrayList<>();

            for (Map<?, ?> itemMap : milestoneSection.getMapList("items")) {
                Object materialObj = itemMap.get("material");
                if (materialObj == null) {
                    continue;
                }
                Object amountObj = itemMap.get("amount");
                int amount = (amountObj instanceof Number number) ? number.intValue() : 1;
                items.add(new ItemRewardDefinition(materialObj.toString(), Math.max(1, amount)));
            }

            levelMilestones.put(level, new LevelMilestone(milestoneMoney, List.copyOf(items)));
        }
    }

    public double getXpCurveBase() {
        return xpCurveBase;
    }

    public double getXpCurveMultiplier() {
        return xpCurveMultiplier;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public long getComboWindowMillis() {
        return comboWindowMillis;
    }

    public double getComboMaxMultiplier() {
        return comboMaxMultiplier;
    }

    public double getDailyBonusMultiplier() {
        return dailyBonusMultiplier;
    }

    public double getWeeklyBonusMultiplier() {
        return weeklyBonusMultiplier;
    }

    public long getDailyResetHourOfDay() {
        return dailyResetHourOfDay;
    }

    public int getMaxPrestige() {
        return maxPrestige;
    }

    public double getPrestigeRewardMultiplierStep() {
        return prestigeRewardMultiplierStep;
    }

    public double getMoneyPerLevel() {
        return moneyPerLevel;
    }

    public Map<String, JobDefinition> getJobDefinitions() {
        return Map.copyOf(jobDefinitions);
    }

    public Map<Integer, LevelMilestone> getLevelMilestones() {
        return Map.copyOf(levelMilestones);
    }

    public record JobDefinition(String id, boolean enabled, double baseXpPerAction, double baseRewardPerAction) {
    }

    public record ItemRewardDefinition(String material, int amount) {
    }

    public record LevelMilestone(double money, List<ItemRewardDefinition> items) {
    }
                }
