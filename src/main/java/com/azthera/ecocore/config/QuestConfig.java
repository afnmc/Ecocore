package com.azthera.ecocore.config;
 
import org.bukkit.configuration.file.YamlConfiguration;
 
/**
 * Typed view over modules/quest.yml: how many daily/weekly/monthly quests are
 * rotated in, when the daily rotation resets, and whether chain/random quests
 * are enabled.
 */
public final class QuestConfig {
 
    private int dailyQuestCount;
    private int weeklyQuestCount;
    private int monthlyQuestCount;
    private long dailyResetHourOfDay;
    private boolean chainQuestsEnabled;
    private boolean randomQuestsEnabled;
    private double randomQuestWeight;
 
    public QuestConfig(YamlConfiguration source) {
        load(source);
    }
 
    public void load(YamlConfiguration source) {
        this.dailyQuestCount = source.getInt("rotation.daily-count", 3);
        this.weeklyQuestCount = source.getInt("rotation.weekly-count", 3);
        this.monthlyQuestCount = source.getInt("rotation.monthly-count", 1);
        this.dailyResetHourOfDay = source.getLong("rotation.daily-reset-hour", 0L);
        this.chainQuestsEnabled = source.getBoolean("chain.enabled", true);
        this.randomQuestsEnabled = source.getBoolean("random.enabled", true);
        this.randomQuestWeight = source.getDouble("random.weight", 0.3);
    }
 
    public int getDailyQuestCount() {
        return dailyQuestCount;
    }
 
    public int getWeeklyQuestCount() {
        return weeklyQuestCount;
    }
 
    public int getMonthlyQuestCount() {
        return monthlyQuestCount;
    }
 
    public long getDailyResetHourOfDay() {
        return dailyResetHourOfDay;
    }
 
    public boolean isChainQuestsEnabled() {
        return chainQuestsEnabled;
    }
 
    public boolean isRandomQuestsEnabled() {
        return randomQuestsEnabled;
    }
 
    public double getRandomQuestWeight() {
        return randomQuestWeight;
    }
}
