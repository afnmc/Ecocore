package com.azthera.ecocore.permission;
 
/**
 * Central registry of every permission node string used across EcoCore,
 * mirroring the nodes declared in plugin.yml. Referencing constants from
 * here (instead of hardcoding string literals in commands/listeners)
 * prevents typos from silently creating unregistered, always-false
 * permission checks.
 */
public final class Permissions {
 
    private Permissions() {
    }
 
    public static final String COMMAND_ECO = "ecocore.command.eco";
    public static final String COMMAND_RELOAD = "ecocore.command.eco.reload";
    public static final String COMMAND_GIVE = "ecocore.command.eco.give";
    public static final String COMMAND_DEBUG = "ecocore.command.eco.debug";
    public static final String COMMAND_MARKET = "ecocore.command.eco.market";
    public static final String COMMAND_INFLATION = "ecocore.command.eco.inflation";
    public static final String COMMAND_JOBS = "ecocore.command.eco.jobs";
    public static final String COMMAND_QUEST = "ecocore.command.eco.quest";
    public static final String COMMAND_SHOP = "ecocore.command.eco.shop";
    public static final String COMMAND_ECONOMY = "ecocore.command.eco.economy";
 
    public static final String SHOP_USE = "ecocore.shop.use";
    public static final String SELL_USE = "ecocore.sell.use";
    public static final String SELL_AUTOSELL = "ecocore.sell.autosell";
    public static final String SELL_BONUS_TIER1 = "ecocore.sell.bonus.tier1";
    public static final String SELL_BONUS_TIER2 = "ecocore.sell.bonus.tier2";
    public static final String SELL_BONUS_TIER3 = "ecocore.sell.bonus.tier3";
 
    public static final String JOBS_USE = "ecocore.jobs.use";
    public static final String JOBS_PRESTIGE = "ecocore.jobs.prestige";
 
    public static final String QUEST_USE = "ecocore.quest.use";
 
    public static final String MINIONS_USE = "ecocore.minions.use";
    public static final String MINIONS_BYPASS_LIMIT = "ecocore.minions.bypasslimit";
 
    public static final String WILDCARD = "ecocore.*";
}
