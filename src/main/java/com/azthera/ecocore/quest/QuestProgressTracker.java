package com.azthera.ecocore.quest;
 
import com.azthera.ecocore.data.model.QuestData;
import com.azthera.ecocore.data.repository.QuestDataRepository;
 
import java.util.List;
import java.util.UUID;
 
/**
 * Applies incremental progress to a player's active quest instances whose
 * objective key matches a given action (e.g. "mine:stone", "kill:zombie",
 * "sell:any"). Marks a quest as completed once required progress is met,
 * but does not grant rewards itself — that is {@code QuestRewardService}'s
 * responsibility, kept separate so progress tracking stays a pure state update.
 */
public final class QuestProgressTracker {
 
    private final QuestDataRepository questDataRepository;
    private final QuestRegistry questRegistry;
 
    public QuestProgressTracker(QuestDataRepository questDataRepository, QuestRegistry questRegistry) {
        this.questDataRepository = questDataRepository;
        this.questRegistry = questRegistry;
    }
 
    /**
     * @return the list of quest instances that just became completed as a
     * result of this progress update (empty if none did).
     */
    public List<QuestData> applyProgress(UUID playerId, String objectiveKey, double amount) {
        List<QuestData> activeQuests = questDataRepository.getAllCachedForPlayer(playerId);
        List<QuestData> justCompleted = new java.util.ArrayList<>();
 
        for (QuestData questData : activeQuests) {
            if (questData.isCompleted()) {
                continue;
            }
 
            var definitionOpt = questRegistry.getDefinition(questData.getQuestDefinitionId());
            if (definitionOpt.isEmpty() || !definitionOpt.get().getObjectiveKey().equals(objectiveKey)) {
                continue;
            }
 
            double newProgress = Math.min(questData.getRequiredProgress(), questData.getProgress() + amount);
            questData.setProgress(newProgress);
 
            if (newProgress >= questData.getRequiredProgress()) {
                questData.setCompleted(true);
                justCompleted.add(questData);
            }
 
            questDataRepository.save(questData);
        }
 
        return justCompleted;
    }
}
