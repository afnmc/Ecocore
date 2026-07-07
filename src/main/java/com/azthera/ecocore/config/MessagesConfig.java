package com.azthera.ecocore.config;
 
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
 
import java.util.HashMap;
import java.util.Map;
 
/**
 * Flattens the nested messages.yml structure (e.g. "economy.balance") into a
 * single-level map keyed by dotted path, ready to be handed to {@code MessageService}.
 */
public final class MessagesConfig {
 
    private final Map<String, String> flattenedMessages = new HashMap<>();
 
    public MessagesConfig(YamlConfiguration source) {
        load(source);
    }
 
    public void load(YamlConfiguration source) {
        flattenedMessages.clear();
        flatten(source, "");
    }
 
    private void flatten(ConfigurationSection section, String path) {
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            String fullPath = path.isEmpty() ? key : path + "." + key;
 
            if (value instanceof ConfigurationSection nested) {
                flatten(nested, fullPath);
            } else if (value != null) {
                flattenedMessages.put(fullPath, String.valueOf(value));
            }
        }
    }
 
    public Map<String, String> asFlatMap() {
        return Map.copyOf(flattenedMessages);
    }
 
    public String get(String key, String fallback) {
        return flattenedMessages.getOrDefault(key, fallback);
    }
}
