package com.azthera.ecocore.jobs;
 
import com.azthera.ecocore.config.JobsConfig;
 
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
 
/**
 * Tracks each player's consecutive-action combo per job, used to scale up
 * XP/reward the longer a player keeps performing the same job action
 * without a gap longer than the configured combo window. Combo resets to 1
 * if the player pauses longer than {@code comboWindowMillis} between actions.
 */
public final class ComboTracker {
 
    private record ComboState(int comboCount, long lastActionMillis) {
    }
 
    private final Map<String, ComboState> comboStates = new ConcurrentHashMap<>();
    private JobsConfig jobsConfig;
 
    public ComboTracker(JobsConfig jobsConfig) {
        this.jobsConfig = jobsConfig;
    }
 
    public void reload(JobsConfig jobsConfig) {
        this.jobsConfig = jobsConfig;
    }
 
    private String key(UUID playerId, String jobId) {
        return playerId + ":" + jobId;
    }
 
    /**
     * Registers a new action for the given player/job and returns the
     * resulting combo multiplier to apply to this action's reward/XP.
     */
    public double registerActionAndGetMultiplier(UUID playerId, String jobId) {
        long now = System.currentTimeMillis();
        String key = key(playerId, jobId);
        ComboState previous = comboStates.get(key);
 
        int newComboCount;
        if (previous == null || (now - previous.lastActionMillis()) > jobsConfig.getComboWindowMillis()) {
            newComboCount = 1;
        } else {
            newComboCount = previous.comboCount() + 1;
        }
 
        comboStates.put(key, new ComboState(newComboCount, now));
        return computeMultiplier(newComboCount);
    }
 
    private double computeMultiplier(int comboCount) {
        double maxMultiplier = jobsConfig.getComboMaxMultiplier();
        double growthPerAction = 0.02;
        double multiplier = 1.0 + Math.min(maxMultiplier - 1.0, (comboCount - 1) * growthPerAction);
        return multiplier;
    }
 
    public void resetCombo(UUID playerId, String jobId) {
        comboStates.remove(key(playerId, jobId));
    }
 
    public int getCurrentCombo(UUID playerId, String jobId) {
        ComboState state = comboStates.get(key(playerId, jobId));
        return state != null ? state.comboCount() : 0;
    }
 
    public void clearPlayer(UUID playerId) {
        comboStates.keySet().removeIf(key -> key.startsWith(playerId + ":"));
    }
}
