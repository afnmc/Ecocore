package com.azthera.ecocore.economy;
 
import com.azthera.ecocore.config.EconomyConfig;
 
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
 
/**
 * Owns the set of currencies known to the economy system: the primary
 * currency (always present, bridged to Vault) and any additional currencies
 * defined under multi-currency support. Rebuilt on every config reload.
 */
public final class CurrencyManager {
 
    private static final String DEFAULT_PRIMARY_DISPLAY_NAME = "Coins";
    private static final String DEFAULT_PRIMARY_SYMBOL = "$";
 
    private final Map<String, Currency> currencies = new LinkedHashMap<>();
    private String primaryCurrencyId;
    private boolean multiCurrencyEnabled;
 
    public CurrencyManager(EconomyConfig economyConfig) {
        reload(economyConfig);
    }
 
    public void reload(EconomyConfig economyConfig) {
        currencies.clear();
        this.primaryCurrencyId = economyConfig.getPrimaryCurrencyId();
        this.multiCurrencyEnabled = economyConfig.isMultiCurrencyEnabled();
 
        currencies.put(primaryCurrencyId, new Currency(
            primaryCurrencyId,
            DEFAULT_PRIMARY_DISPLAY_NAME,
            DEFAULT_PRIMARY_SYMBOL,
            economyConfig.getStartingBalance()
        ));
 
        if (multiCurrencyEnabled) {
            for (EconomyConfig.CurrencyDefinition definition : economyConfig.getCurrencyDefinitions().values()) {
                currencies.put(definition.id(), new Currency(
                    definition.id(),
                    definition.displayName(),
                    definition.symbol(),
                    definition.startingBalance()
                ));
            }
        }
    }
 
    public String getPrimaryCurrencyId() {
        return primaryCurrencyId;
    }
 
    public boolean isMultiCurrencyEnabled() {
        return multiCurrencyEnabled;
    }
 
    public Optional<Currency> getCurrency(String currencyId) {
        return Optional.ofNullable(currencies.get(currencyId));
    }
 
    public Currency getPrimaryCurrency() {
        return currencies.get(primaryCurrencyId);
    }
 
    public Collection<Currency> getAllCurrencies() {
        return currencies.values();
    }
 
    public boolean isKnownCurrency(String currencyId) {
        return currencies.containsKey(currencyId);
    }
}
