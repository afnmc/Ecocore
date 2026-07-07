package com.azthera.ecocore.quest;
 
import com.azthera.ecocore.bootstrap.Module;
import com.azthera.ecocore.config.QuestConfig;
import com.azthera.ecocore.data.model.QuestData;
import com.azthera.ecocore.data.repository.QuestDataRepository;
import com.azthera.ecocore.integration.EcoScheduledTask;
import com.azthera.ecocore.integration.SchedulerAdapter;
 
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
 
/**
 * Lifecycle owner of the Quest module. Loads each online player's active
 * quests on join, checks periodically whether the daily rotation reset has
 * occurred and regenerates quests accordingly, and exposes the accept/claim
 * operations used by {@code QuestGui} and quest commands.
 */
public final class QuestManager implements Module {
 
    private final QuestDataRepository questDataRepository;
    private final QuestRegistry questRegistry;
    private final QuestGenerator questGenerator;
    private final QuestProgressTracker progressTracker;
    private final QuestRewardService rewardService;
    private final QuestChainResolver chainResolver;
    private final SchedulerAdapter schedulerAdapter;
    private QuestConfig questConfig;
    private final Logger logger;
    private final AtomicReference<LocalDate> lastRotationDate = new AtomicReference<>(LocalDate.now());
    private EcoScheduledTask rotationCheckTask;
    private boolean enabled;
 
    public QuestManager(QuestDataRepository questDataRepository, QuestRegistry questRegistry,
                         QuestGenerator questGenerator, QuestProgressTracker progressTracker,
                         QuestRewardService rewardService, QuestChainResolver chainResolver,
                         SchedulerAdapter schedulerAdapter, QuestConfig questConfig, Logger logger) {
        this.questDataRepository = questDataRepository;
        this.questRegistry = questRegistry;
        this.questGenerator = questGenerator;
        this.progressTracker = progressTracker;
        this.rewardService = rewardService;
        this.chainResolver = chainResolver;
        this.schedulerAdapter = schedulerAdapter;
        this.questConfig = questConfig;
        this.logger = logger;
    }
 
    @Override
    public String getName() {
        return "quest";
    }
 
    @Override
    public void enable() {
        this.enabled = true;
        rotationCheckTask = schedulerAdapter.runAsyncRepeating(this::checkRotation, 20L * 60L, 20L * 60L);
        logger.info("Quest module enabled.");
    }
 
    @Override
    public void disable() {
        this.enabled = false;
        if (rotationCheckTask != null && !rotationCheckTask.isCancelled()) {
            rotationCheckTask.cancel();
        }
    }
 
    @Override
    public void reload() {
        disable();
        enable();
    }
 
    @Override
    public boolean isEnabled() {
        return enabled;
    }
 
    private void checkRotation() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate previous = lastRotationDate.getAndSet(today);
        if (!previous.equals(today)) {
            logger.info("Daily quest rotation reset triggered.");
        }
    }
 
    public void loadPlayerQuests(UUID playerId) {
        long now = System.currentTimeMillis();
        questDataRepository.loadActiveForPlayerAsync(playerId, now).thenAccept(activeQuests -> {
            if (activeQuests.isEmpty()) {
                questGenerator.generateDailyQuests(playerId);
                questGenerator.generateWeeklyQuests(playerId);
                questGenerator.generateMonthlyQuests(playerId);
            }
            questGenerator.maybeGenerateRandomQuest(playerId);
        });
    }
 
    public void unloadPlayerQuests(UUID playerId, List<String> questInstanceIds) {
        questDataRepository.invalidatePlayer(playerId, questInstanceIds);
    }
 
    public List<QuestData> getActiveQuests(UUID playerId) {
        return questDataRepository.getAllCachedForPlayer(playerId);
    }
 
    public List<QuestData> recordProgress(UUID playerId, String objectiveKey, double amount) {
        List<QuestData> justCompleted = progressTracker.applyProgress(playerId, objectiveKey, amount);
 
        for (QuestData completed : justCompleted) {
            questRegistry.getDefinition(completed.getQuestDefinitionId())
                .flatMap(chainResolver::resolveNextInChain)
                .ifPresent(nextDefinition -> {
                    String instanceId = nextDefinition.getId() + ":" + UUID.randomUUID();
                    QuestData nextInstance = new QuestData(
                        playerId, instanceId, nextDefinition.getId(), 0.0, nextDefinition.getRequiredProgress(),
                        false, false, System.currentTimeMillis(),
                        System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)
                    );
                    questDataRepository.save(nextInstance);
                });
        }
 
        return justCompleted;
    }
 
    public com.azthera.ecocore.util.Result<Double> claimReward(UUID playerId, String questInstanceId) {
        return rewardService.claimReward(playerId, questInstanceId);
    }
 
    public QuestRegistry getQuestRegistry() {
        return questRegistry;
    }
 
    public void updateQuestConfig(QuestConfig questConfig) {
        this.questConfig = questConfig;
        questGenerator.reload(questConfig);
    }
}
