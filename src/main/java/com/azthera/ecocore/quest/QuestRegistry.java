package com.azthera.ecocore.quest;
 
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
 
/**
 * In-memory registry of all known {@link QuestDefinition}s, populated on
 * plugin startup/reload from the quest module's config-defined templates.
 * This is intentionally a small standalone class (rather than folded into
 * {@code QuestManager}) so {@code QuestChainResolver} and
 * {@code QuestGenerator} can depend on lookups without depending on the
 * full manager lifecycle.
 */
public final class QuestRegistry {
 
    private final Map<String, QuestDefinition> definitions = new LinkedHashMap<>();
 
    public void register(QuestDefinition definition) {
        definitions.put(definition.getId(), definition);
    }
 
    public void clear() {
        definitions.clear();
    }
 
    public Optional<QuestDefinition> getDefinition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }
 
    public Collection<QuestDefinition> getAllDefinitions() {
        return definitions.values();
    }
 
    public Collection<QuestDefinition> getDefinitionsByType(QuestType type) {
        return definitions.values().stream()
            .filter(definition -> definition.getType() == type)
            .toList();
    }
}
