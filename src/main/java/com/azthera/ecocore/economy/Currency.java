package com.azthera.ecocore.economy;
 
/**
 * Immutable definition of a single currency known to the economy system.
 * The primary currency (bridged to Vault) always exists; additional
 * currencies are optional and enabled via modules/economy.yml.
 */
public record Currency(String id, String displayName, String symbol, double startingBalance) {
}
