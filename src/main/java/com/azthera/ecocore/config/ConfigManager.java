package com.azthera.ecocore.config;
 
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
 
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
 
/**
 * Central owner of every YAML configuration file used by EcoCore. Responsible for
 * ensuring default resources exist on disk, running migration before load, parsing
 * each file into its typed wrapper class, and re-running that process on {@code /eco reload}.
 */
public final class ConfigManager {
 
    private final Plugin plugin;
    private final Logger logger;
    private final ConfigMigrator migrator;
 
    private YamlConfiguration mainConfig;
    private MessagesConfig messagesConfig;
    private EconomyConfig economyConfig;
    private ShopConfig shopConfig;
    private MarketConfig marketConfig;
    private InflationConfig inflationConfig;
    private JobsConfig jobsConfig;
    private QuestConfig questConfig;
    private MinionsConfig minionsConfig;
    private GuiConfig guiConfig;
    private YamlConfiguration sellConfig;
    private YamlConfiguration shopItemsConfig;
 
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.migrator = new ConfigMigrator(plugin);
    }
 
    public void loadAll() {
        saveDefaultResources();
        migrator.migrateAll();
 
        this.mainConfig = loadYaml("config.yml");
        this.messagesConfig = new MessagesConfig(loadYaml("messages.yml"));
        this.economyConfig = new EconomyConfig(loadYaml("modules/economy.yml"));
        this.shopConfig = new ShopConfig(loadYaml("modules/shop.yml"));
        this.shopItemsConfig = loadYaml("modules/shop-items.yml");
        this.marketConfig = new MarketConfig(loadYaml("modules/market.yml"));
        this.inflationConfig = new InflationConfig(loadYaml("modules/inflation.yml"));
        this.jobsConfig = new JobsConfig(loadYaml("modules/jobs.yml"));
        this.questConfig = new QuestConfig(loadYaml("modules/quest.yml"));
        this.minionsConfig = new MinionsConfig(loadYaml("modules/minions.yml"));
        this.sellConfig = loadYaml("modules/sell.yml");
        this.guiConfig = new GuiConfig(
            loadYaml("gui/shop.yml"),
            loadYaml("gui/sell.yml"),
            loadYaml("gui/jobs.yml"),
            loadYaml("gui/quest.yml"),
            loadYaml("gui/minions.yml")
        );
    }
 
    public void reloadAll() {
        loadAll();
        logger.info("EcoCore configuration reloaded.");
    }
 
    private void saveDefaultResources() {
        saveResourceIfMissing("config.yml");
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("modules/economy.yml");
        saveResourceIfMissing("modules/shop.yml");
        saveResourceIfMissing("modules/shop-items.yml");
        saveResourceIfMissing("modules/market.yml");
        saveResourceIfMissing("modules/inflation.yml");
        saveResourceIfMissing("modules/jobs.yml");
        saveResourceIfMissing("modules/quest.yml");
        saveResourceIfMissing("modules/minions.yml");
        saveResourceIfMissing("modules/sell.yml");
        saveResourceIfMissing("gui/shop.yml");
        saveResourceIfMissing("gui/sell.yml");
        saveResourceIfMissing("gui/jobs.yml");
        saveResourceIfMissing("gui/quest.yml");
        saveResourceIfMissing("gui/minions.yml");
    }
 
    private void saveResourceIfMissing(String relativePath) {
        File target = new File(plugin.getDataFolder(), relativePath);
        if (!target.exists()) {
            plugin.saveResource(relativePath, false);
        }
    }
 
    private YamlConfiguration loadYaml(String relativePath) {
        File file = new File(plugin.getDataFolder(), relativePath);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
 
        try (InputStream defaultStream = plugin.getResource(relativePath)) {
            if (defaultStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
                );
                configuration.setDefaults(defaults);
            }
        } catch (IOException exception) {
            logger.warning("Failed to load defaults for " + relativePath + ": " + exception.getMessage());
        }
 
        return configuration;
    }
 
    public YamlConfiguration getMainConfig() {
        return mainConfig;
    }
 
    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }
 
    public EconomyConfig getEconomyConfig() {
        return economyConfig;
    }
 
    public ShopConfig getShopConfig() {
        return shopConfig;
    }
 
    public MarketConfig getMarketConfig() {
        return marketConfig;
    }
 
    public InflationConfig getInflationConfig() {
        return inflationConfig;
    }
 
    public JobsConfig getJobsConfig() {
        return jobsConfig;
    }
 
    public QuestConfig getQuestConfig() {
        return questConfig;
    }
 
    public MinionsConfig getMinionsConfig() {
        return minionsConfig;
    }
 
    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    public YamlConfiguration getSellConfig() {
        return sellConfig;
    }

    public YamlConfiguration getShopItemsConfig() {
        return shopItemsConfig;
    }
 
    public boolean isDebugMode() {
        return mainConfig.getBoolean("debug", false);
    }
 
    public String getMessagePrefix() {
        return mainConfig.getString("prefix", "<gray>[<gold>EcoCore</gold>]</gray> ");
    }
 
    public boolean isModuleEnabledInConfig(String moduleKey) {
        return mainConfig.getBoolean("modules." + moduleKey, true);
    }
}
