package com.azthera.ecocore.data.model;
 
import java.util.UUID;
 
/**
 * A single player's progress on a single accepted quest instance.
 * {@code questDefinitionId} refers to the static quest template id defined
 * in the quest module (not persisted here), while this record tracks the
 * player-specific runtime progress toward completing it.
 */
public final class QuestData {
 
    private final UUID playerId;
    private final String questInstanceId;
    private final String questDefinitionId;
    private double progress;
    private double requiredProgress;
    private boolean completed;
    private boolean rewardClaimed;
    private long acceptedAtMillis;
    private long expiresAtMillis;
 
    public QuestData(UUID playerId, String questInstanceId, String questDefinitionId, double progress,
                      double requiredProgress, boolean completed, boolean rewardClaimed,
                      long acceptedAtMillis, long expiresAtMillis) {
        this.playerId = playerId;
        this.questInstanceId = questInstanceId;
        this.questDefinitionId = questDefinitionId;
        this.progress = progress;
        this.requiredProgress = requiredProgress;
        this.completed = completed;
        this.rewardClaimed = rewardClaimed;
        this.acceptedAtMillis = acceptedAtMillis;
        this.expiresAtMillis = expiresAtMillis;
    }
 
    public UUID getPlayerId() {
        return playerId;
    }
 
    public String getQuestInstanceId() {
        return questInstanceId;
    }
 
    public String getQuestDefinitionId() {
        return questDefinitionId;
    }
 
    public double getProgress() {
        return progress;
    }
 
    public void setProgress(double progress) {
        this.progress = progress;
    }
 
    public double getRequiredProgress() {
        return requiredProgress;
    }
 
    public void setRequiredProgress(double requiredProgress) {
        this.requiredProgress = requiredProgress;
    }
 
    public boolean isCompleted() {
        return completed;
    }
 
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
 
    public boolean isRewardClaimed() {
        return rewardClaimed;
    }
 
    public void setRewardClaimed(boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }
 
    public long getAcceptedAtMillis() {
        return acceptedAtMillis;
    }
 
    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }
 
    public void setExpiresAtMillis(long expiresAtMillis) {
        this.expiresAtMillis = expiresAtMillis;
    }
}
