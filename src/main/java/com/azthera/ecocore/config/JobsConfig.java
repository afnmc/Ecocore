package com.azthera.ecocore.config;
 
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
 
import java.util.HashMap;
import java.util.Map;
 
/**
 * Typed view over modules/jobs.yml: the shared XP curve, combo window,
 * daily/weekly bonus multipliers, prestige bounds, and per-job base values
 * for the seven job types (Mining, Woodcutting, Fishing, Farming, Hunting,
 * Exploring, Builder).
 */
public final class JobsConfig {
 
    private double xpCurveBase;
    private double xpCurveMultiplier;
    private int maxLevel;
    private long comboWindowMillis;
    private double comboMaxMultiplier;
    private double dailyBonusMultiplier;
    private double weeklyBonusMultiplier;
    private int maxPrestige;
    private double prestigeRewardMultiplierStep;
    private final Map<String, JobDefinition> jobDefinitions = new HashMap<>();
 
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
 
    public int getMaxPrestige() {
        return maxPrestige;
    }
 
    public double getPrestigeRewardMultiplierStep() {
        return prestigeRewardMultiplierStep;
    }
 
    public Map<String, JobDefinition> getJobDefinitions() {
        return Map.copyOf(jobDefinitions);
    }
 
    public record JobDefinition(String id, boolean enabled, double baseXpPerAction, double baseRewardPerAction) {
    }
}
