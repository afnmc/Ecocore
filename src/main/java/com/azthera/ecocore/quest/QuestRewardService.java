package com.azthera.ecocore.quest;
 
import com.azthera.ecocore.data.model.QuestData;
import com.azthera.ecocore.data.repository.QuestDataRepository;
import com.azthera.ecocore.economy.EconomyService;
import com.azthera.ecocore.economy.TransactionType;
import com.azthera.ecocore.util.Result;
 
import java.util.Optional;
import java.util.UUID;
 
/**
 * Pays out the reward for a completed-but-unclaimed quest instance and marks
 * it as claimed. Split from {@code QuestProgressTracker} so that completing
 * a quest's objective and claiming its reward remain distinct steps — this
 * matches the GUI flow where a completed quest shows a "claim" button rather
 * than auto-granting the reward the instant progress hits 100%.
 */
public final class QuestRewardService {
 
    private final QuestDataRepository questDataRepository;
    private final QuestRegistry questRegistry;
    private final EconomyService economyService;
 
    public QuestRewardService(QuestDataRepository questDataRepository, QuestRegistry questRegistry,
                               EconomyService economyService) {
        this.questDataRepository = questDataRepository;
        this.questRegistry = questRegistry;
        this.economyService = economyService;
    }
 
    public Result<Double> claimReward(UUID playerId, String questInstanceId) {
        Optional<QuestData> questDataOpt = questDataRepository.getCached(playerId, questInstanceId);
        if (questDataOpt.isEmpty()) {
            return Result.failure("Quest tidak ditemukan.");
        }
 
        QuestData questData = questDataOpt.get();
        if (!questData.isCompleted()) {
            return Result.failure("Quest belum selesai.");
        }
        if (questData.isRewardClaimed()) {
            return Result.failure("Reward quest ini sudah diklaim.");
        }
 
        Optional<QuestDefinition> definitionOpt = questRegistry.getDefinition(questData.getQuestDefinitionId());
        if (definitionOpt.isEmpty()) {
            return Result.failure("Definisi quest tidak ditemukan.");
        }
 
        QuestDefinition definition = definitionOpt.get();
        double moneyReward = definition.getMoneyReward();
 
        if (moneyReward > 0) {
            economyService.deposit(playerId, moneyReward, TransactionType.QUEST_REWARD,
                "Quest reward: " + definition.getTitle());
        }
 
        questData.setRewardClaimed(true);
        questDataRepository.save(questData);
 
        return Result.success(moneyReward);
    }
}
