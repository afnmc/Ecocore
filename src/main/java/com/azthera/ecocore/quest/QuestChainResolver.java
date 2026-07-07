package com.azthera.ecocore.quest;
 
import java.util.List;
import java.util.Optional;
 
/**
 * Resolves chain-quest sequencing: given a completed quest definition,
 * determines the next quest in its chain (if any) and whether a player has
 * satisfied all prerequisites to accept a given quest. Chain quests use
 * {@code QuestDefinition#getNextInChainId} to link steps; prerequisite-gated
 * quests (including non-chain ones) use {@code QuestDefinition#getPrerequisiteIds}.
 */
public final class QuestChainResolver {
 
    private final QuestRegistry questRegistry;
 
    public QuestChainResolver(QuestRegistry questRegistry) {
        this.questRegistry = questRegistry;
    }
 
    public Optional<QuestDefinition> resolveNextInChain(QuestDefinition completedDefinition) {
        if (!completedDefinition.isPartOfChain()) {
            return Optional.empty();
        }
        return questRegistry.getDefinition(completedDefinition.getNextInChainId());
    }
 
    public boolean arePrerequisitesMet(QuestDefinition definition, List<String> completedQuestDefinitionIds) {
        if (definition.getPrerequisiteIds().isEmpty()) {
            return true;
        }
        return completedQuestDefinitionIds.containsAll(definition.getPrerequisiteIds());
    }
}
