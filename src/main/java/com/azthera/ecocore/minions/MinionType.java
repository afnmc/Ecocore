package com.azthera.ecocore.minions;
 
/**
 * The five minion types supported when the Minions module is enabled.
 * Each corresponds to a section under modules/minions.yml#types and to a
 * distinct action performed by {@code MinionTaskScheduler} on tick.
 */
public enum MinionType {
    MINER,
    FARMER,
    LUMBERJACK,
    FISHING,
    COLLECTOR
}
