package com.azthera.ecocore.config;
 
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
 
import java.util.HashMap;
import java.util.Map;
 
/**
 * Typed view over modules/economy.yml: starting balance, tax, money sink,
 * statistics toggle, and optional multi-currency definitions.
 */
public final class EconomyConfig {
 
    private boolean multiCurrencyEnabled;
    private String primaryCurrencyId;
    private double startingBalance;
    private boolean taxEnabled;
    private double taxRate;
    private double moneySinkThreshold;
    private boolean statisticsEnabled;
    private final Map<String, CurrencyDefinition> currencyDefinitions = new HashMap<>();
 
    public EconomyConfig(YamlConfiguration source) {
        load(source);
    }
 
    public void load(YamlConfiguration source) {
        this.multiCurrencyEnabled = source.getBoolean("multi-currency.enabled", false);
        this.primaryCurrencyId = source.getString("multi-currency.primary", "vault");
        this.startingBalance = source.getDouble("starting-balance", 500.0);
        this.taxEnabled = source.getBoolean("tax.enabled", true);
        this.taxRate = source.getDouble("tax.rate", 0.02);
        this.moneySinkThreshold = source.getDouble("money-sink.threshold", 1_000_000.0);
        this.statisticsEnabled = source.getBoolean("statistics.enabled", true);
 
        currencyDefinitions.clear();
        ConfigurationSection currenciesSection = source.getConfigurationSection("multi-currency.currencies");
        if (currenciesSection != null) {
            for (String currencyId : currenciesSection.getKeys(false)) {
                ConfigurationSection currencySection = currenciesSection.getConfigurationSection(currencyId);
                if (currencySection == null) {
                    continue;
                }
                currencyDefinitions.put(currencyId, new CurrencyDefinition(
                    currencyId,
                    currencySection.getString("display-name", currencyId),
                    currencySection.getString("symbol", ""),
                    currencySection.getDouble("starting-balance", 0.0)
                ));
            }
        }
    }
 
    public boolean isMultiCurrencyEnabled() {
        return multiCurrencyEnabled;
    }
 
    public String getPrimaryCurrencyId() {
        return primaryCurrencyId;
    }
 
    public double getStartingBalance() {
        return startingBalance;
    }
 
    public boolean isTaxEnabled() {
        return taxEnabled;
    }
 
    public double getTaxRate() {
        return taxRate;
    }
 
    public double getMoneySinkThreshold() {
        return moneySinkThreshold;
    }
 
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }
 
    public Map<String, CurrencyDefinition> getCurrencyDefinitions() {
        return Map.copyOf(currencyDefinitions);
    }
 
    public record CurrencyDefinition(String id, String displayName, String symbol, double startingBalance) {
    }
}
