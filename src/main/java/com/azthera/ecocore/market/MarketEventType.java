package com.azthera.ecocore.market;
 
/**
 * The set of market-wide events the AI Market simulation can trigger.
 * Each corresponds to a configurable entry under modules/market.yml#events.
 */
public enum MarketEventType {
    BOOM_MARKET,
    CRASH,
    INFLATION,
    DEFLATION,
    FESTIVAL,
    MINING_WEEK,
    FARMING_WEEK,
    FISHING_WEEK
}
