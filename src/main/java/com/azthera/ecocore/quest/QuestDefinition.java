package com.azthera.ecocore.quest;
 
import java.util.List;
 
/**
 * Static, config/registry-defined template for a quest: its type, objective
 * (what action and how much progress is required), reward, and presentation
 * details (title, sound, whether it shows a BossBar while active). Player
 * runtime progress against a definition is tracked separately in
 * {@code QuestData}.
 */
public final class QuestDefinition {
 
    private final String id;
    private final QuestType type;
    private final String title;
    private final String description;
    private final String objectiveKey;
    private final double requiredProgress;
    private final double moneyReward;
    private final double experienceReward;
    private final String soundKey;
    private final boolean showBossBar;
    private final String nextInChainId;
    private final List<String> prerequisiteIds;
 
    public QuestDefinition(String id, QuestType type, String title, String description, String objectiveKey,
                            double requiredProgress, double moneyReward, double experienceReward, String soundKey,
                            boolean showBossBar, String nextInChainId, List<String> prerequisiteIds) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.objectiveKey = objectiveKey;
        this.requiredProgress = requiredProgress;
        this.moneyReward = moneyReward;
        this.experienceReward = experienceReward;
        this.soundKey = soundKey;
        this.showBossBar = showBossBar;
        this.nextInChainId = nextInChainId;
        this.prerequisiteIds = prerequisiteIds;
    }
 
    public String getId() {
        return id;
    }
 
    public QuestType getType() {
        return type;
    }
 
    public String getTitle() {
        return title;
    }
 
    public String getDescription() {
        return description;
    }
 
    public String getObjectiveKey() {
        return objectiveKey;
    }
 
    public double getRequiredProgress() {
        return requiredProgress;
    }
 
    public double getMoneyReward() {
        return moneyReward;
    }
 
    public double getExperienceReward() {
        return experienceReward;
    }
 
    public String getSoundKey() {
        return soundKey;
    }
 
    public boolean isShowBossBar() {
        return showBossBar;
    }
 
    public String getNextInChainId() {
        return nextInChainId;
    }
 
    public List<String> getPrerequisiteIds() {
        return prerequisiteIds;
    }
 
    public boolean isPartOfChain() {
        return nextInChainId != null && !nextInChainId.isBlank();
    }
}
