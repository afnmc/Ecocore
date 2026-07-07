package com.azthera.ecocore.config;
 
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
 
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;
 
/**
 * Ensures every managed YAML file on disk contains all keys present in the
 * bundled default resource, without ever overwriting values the server owner
 * has already customized. This is what allows plugin updates to add new config
 * options without forcing the owner to delete and regenerate their configs.
 */
public final class ConfigMigrator {
 
    private static final String VERSION_KEY = "config-version";
 
    private static final List<String> MANAGED_FILES = List.of(
        "config.yml",
        "messages.yml",
        "modules/economy.yml",
        "modules/shop.yml",
        "modules/shop-items.yml",
        "modules/market.yml",
        "modules/inflation.yml",
        "modules/jobs.yml",
        "modules/quest.yml",
        "modules/minions.yml",
        "modules/sell.yml",
        "gui/shop.yml",
        "gui/sell.yml",
        "gui/jobs.yml",
        "gui/quest.yml",
        "gui/minions.yml"
    );
 
    private final Plugin plugin;
    private final Logger logger;
 
    public ConfigMigrator(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
 
    public void migrateAll() {
        for (String relativePath : MANAGED_FILES) {
            migrateFile(relativePath);
        }
    }
 
    private void migrateFile(String relativePath) {
        File file = new File(plugin.getDataFolder(), relativePath);
        if (!file.exists()) {
            return;
        }
 
        YamlConfiguration onDisk = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration defaults = loadDefaultResource(relativePath);
        if (defaults == null) {
            return;
        }
 
        boolean changed = mergeMissingKeys(defaults, onDisk);
 
        int defaultVersion = defaults.getInt(VERSION_KEY, 1);
        int diskVersion = onDisk.getInt(VERSION_KEY, 0);
        if (diskVersion < defaultVersion) {
            onDisk.set(VERSION_KEY, defaultVersion);
            changed = true;
        }
 
        if (changed) {
            try {
                onDisk.save(file);
                logger.info(() -> "Migrated configuration file: " + relativePath);
            } catch (IOException exception) {
                logger.warning("Failed to save migrated config " + relativePath + ": " + exception.getMessage());
            }
        }
    }
 
    private boolean mergeMissingKeys(ConfigurationSection defaults, ConfigurationSection target) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            Object defaultValue = defaults.get(key);
 
            if (!target.contains(key)) {
                target.set(key, defaultValue);
                changed = true;
                continue;
            }
 
            if (defaultValue instanceof ConfigurationSection defaultSection) {
                ConfigurationSection targetSection = target.getConfigurationSection(key);
                if (targetSection == null) {
                    target.set(key, defaultValue);
                    changed = true;
                } else if (mergeMissingKeys(defaultSection, targetSection)) {
                    changed = true;
                }
            }
        }
        return changed;
    }
 
    private YamlConfiguration loadDefaultResource(String relativePath) {
        try (InputStream stream = plugin.getResource(relativePath)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            logger.warning("Failed to read default resource " + relativePath + ": " + exception.getMessage());
            return null;
        }
    }
}
