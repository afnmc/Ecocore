package com.azthera.ecocore.config;
 
import org.bukkit.configuration.file.YamlConfiguration;
 
/**
 * Aggregates the five GUI definition files (shop, sell, jobs, quest, minions)
 * so {@code GuiIconResolver} and each concrete GUI class have a single point
 * of access to their configurable titles, slots, and icon materials.
 */
public final class GuiConfig {
 
    private YamlConfiguration shopGui;
    private YamlConfiguration sellGui;
    private YamlConfiguration jobsGui;
    private YamlConfiguration questGui;
    private YamlConfiguration minionsGui;
 
    public GuiConfig(YamlConfiguration shopGui, YamlConfiguration sellGui, YamlConfiguration jobsGui,
                      YamlConfiguration questGui, YamlConfiguration minionsGui) {
        load(shopGui, sellGui, jobsGui, questGui, minionsGui);
    }
 
    public void load(YamlConfiguration shopGui, YamlConfiguration sellGui, YamlConfiguration jobsGui,
                      YamlConfiguration questGui, YamlConfiguration minionsGui) {
        this.shopGui = shopGui;
        this.sellGui = sellGui;
        this.jobsGui = jobsGui;
        this.questGui = questGui;
        this.minionsGui = minionsGui;
    }
 
    public YamlConfiguration getShopGui() {
        return shopGui;
    }
 
    public YamlConfiguration getSellGui() {
        return sellGui;
    }
 
    public YamlConfiguration getJobsGui() {
        return jobsGui;
    }
 
    public YamlConfiguration getQuestGui() {
        return questGui;
    }
 
    public YamlConfiguration getMinionsGui() {
        return minionsGui;
    }
}
