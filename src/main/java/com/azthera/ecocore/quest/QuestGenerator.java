package com.azthera.ecocore.quest;
 
import com.azthera.ecocore.config.QuestConfig;
import com.azthera.ecocore.data.model.QuestData;
import com.azthera.ecocore.data.repository.QuestDataRepository;
 
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
 
/**
 * Generates new quest instances for a player during a rotation reset
 * (daily/weekly/monthly) or on-demand for random quests. Picks definitions
 * from {@code QuestRegistry} by type, respecting the configured counts per
 * rotation and the random-quest weight, and creates fresh {@code QuestData}
 * instances with an expiry appropriate to the quest's type.
 */
public final class QuestGenerator {
 
    private final QuestRegistry questRegistry;
    private final QuestDataRepository questDataRepository;
    private QuestConfig questConfig;
    private final Random random = new Random();
    private final ZoneId zoneId = ZoneId.systemDefault();
 
    public QuestGenerator(QuestRegistry questRegistry, QuestDataRepository questDataRepository, QuestConfig questConfig) {
        this.questRegistry = questRegistry;
        this.questDataRepository = questDataRepository;
        this.questConfig = questConfig;
    }
 
    public void reload(QuestConfig questConfig) {
        this.questConfig = questConfig;
    }
 
    public void generateDailyQuests(UUID playerId) {
        generateFromPool(playerId, QuestType.DAILY, questConfig.getDailyQuestCount(), computeDailyExpiry());
    }
 
    public void generateWeeklyQuests(UUID playerId) {
        generateFromPool(playerId, QuestType.WEEKLY, questConfig.getWeeklyQuestCount(), computeWeeklyExpiry());
    }
 
    public void generateMonthlyQuests(UUID playerId) {
        generateFromPool(playerId, QuestType.MONTHLY, questConfig.getMonthlyQuestCount(), computeMonthlyExpiry());
    }
 
    public void maybeGenerateRandomQuest(UUID playerId) {
        if (!questConfig.isRandomQuestsEnabled()) {
            return;
        }
        if (random.nextDouble() > questConfig.getRandomQuestWeight()) {
            return;
        }
 
        List<QuestDefinition> pool = List.copyOf(questRegistry.getDefinitionsByType(QuestType.RANDOM));
        if (pool.isEmpty()) {
            return;
        }
 
        QuestDefinition chosen = pool.get(random.nextInt(pool.size()));
        createInstance(playerId, chosen, computeDailyExpiry());
    }
 
    private void generateFromPool(UUID playerId, QuestType type, int count, long expiresAtMillis) {
        List<QuestDefinition> pool = List.copyOf(questRegistry.getDefinitionsByType(type));
        if (pool.isEmpty()) {
            return;
        }
 
        List<QuestDefinition> shuffled = new java.util.ArrayList<>(pool);
        java.util.Collections.shuffle(shuffled, random);
 
        int actualCount = Math.min(count, shuffled.size());
        for (int i = 0; i < actualCount; i++) {
            createInstance(playerId, shuffled.get(i), expiresAtMillis);
        }
    }
 
    private void createInstance(UUID playerId, QuestDefinition definition, long expiresAtMillis) {
        String instanceId = definition.getId() + ":" + UUID.randomUUID();
        QuestData questData = new QuestData(
            playerId, instanceId, definition.getId(), 0.0, definition.getRequiredProgress(),
            false, false, System.currentTimeMillis(), expiresAtMillis
        );
        questDataRepository.save(questData);
    }
 
    private long computeDailyExpiry() {
        return ZonedDateTime.now(zoneId).plusDays(1).toInstant().toEpochMilli();
    }
 
    private long computeWeeklyExpiry() {
        return ZonedDateTime.now(zoneId).plusWeeks(1).toInstant().toEpochMilli();
    }
 
    private long computeMonthlyExpiry() {
        return ZonedDateTime.now(zoneId).plusMonths(1).toInstant().toEpochMilli();
    }
}
