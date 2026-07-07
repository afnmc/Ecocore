package com.azthera.ecocore.data.model;
 
import java.util.UUID;
 
/**
 * A single player's progress in a single job type: level, XP, prestige tier,
 * and lifetime action count (used for statistics and milestone rewards).
 */
public final class JobData {
 
    private final UUID playerId;
    private final String jobId;
    private int level;
    private double experience;
    private int prestigeTier;
    private long totalActionsPerformed;
    private long lastActionMillis;
 
    public JobData(UUID playerId, String jobId, int level, double experience, int prestigeTier,
                    long totalActionsPerformed, long lastActionMillis) {
        this.playerId = playerId;
        this.jobId = jobId;
        this.level = level;
        this.experience = experience;
        this.prestigeTier = prestigeTier;
        this.totalActionsPerformed = totalActionsPerformed;
        this.lastActionMillis = lastActionMillis;
    }
 
    public UUID getPlayerId() {
        return playerId;
    }
 
    public String getJobId() {
        return jobId;
    }
 
    public int getLevel() {
        return level;
    }
 
    public void setLevel(int level) {
        this.level = level;
    }
 
    public double getExperience() {
        return experience;
    }
 
    public void setExperience(double experience) {
        this.experience = experience;
    }
 
    public int getPrestigeTier() {
        return prestigeTier;
    }
 
    public void setPrestigeTier(int prestigeTier) {
        this.prestigeTier = prestigeTier;
    }
 
    public long getTotalActionsPerformed() {
        return totalActionsPerformed;
    }
 
    public void setTotalActionsPerformed(long totalActionsPerformed) {
        this.totalActionsPerformed = totalActionsPerformed;
    }
 
    public long getLastActionMillis() {
        return lastActionMillis;
    }
 
    public void setLastActionMillis(long lastActionMillis) {
        this.lastActionMillis = lastActionMillis;
    }
}
