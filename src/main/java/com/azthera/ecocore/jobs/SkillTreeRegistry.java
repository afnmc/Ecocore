package com.azthera.ecocore.jobs;
 
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
 
/**
 * Placeholder registry for future per-job skill trees. Currently supports
 * registering simple {@code SkillNode} definitions (unlock level + a
 * descriptive passive bonus id) without wiring any gameplay effect yet —
 * this exists so the jobs module has an extension point ("Skill Tree siap
 * dikembangkan") without over-building a system with no concrete nodes yet.
 */
public final class SkillTreeRegistry {
 
    private final Map<String, Map<String, SkillNode>> skillTreesByJob = new LinkedHashMap<>();
 
    public void registerSkillNode(String jobId, SkillNode node) {
        skillTreesByJob
            .computeIfAbsent(jobId.toUpperCase(), ignored -> new LinkedHashMap<>())
            .put(node.id(), node);
    }
 
    public Map<String, SkillNode> getSkillTree(String jobId) {
        return Map.copyOf(skillTreesByJob.getOrDefault(jobId.toUpperCase(), Map.of()));
    }
 
    public Optional<SkillNode> getNode(String jobId, String nodeId) {
        return Optional.ofNullable(skillTreesByJob.getOrDefault(jobId.toUpperCase(), Map.of()).get(nodeId));
    }
 
    public boolean isUnlocked(String jobId, String nodeId, int currentLevel) {
        return getNode(jobId, nodeId).map(node -> currentLevel >= node.unlockLevel()).orElse(false);
    }
 
    /**
     * @param id unique node id within its job's tree.
     * @param displayName human-readable name shown in the Jobs GUI.
     * @param unlockLevel job level required to unlock this node.
     * @param passiveBonusId opaque identifier consumed by future gameplay code
     * (e.g. "fortune_boost_1") — intentionally uninterpreted here.
     */
    public record SkillNode(String id, String displayName, int unlockLevel, String passiveBonusId) {
    }
}
