package com.azthera.ecocore.economy;
 
/**
 * Categorizes every kind of balance-affecting event in the economy, used for
 * transaction logging, tax eligibility decisions, and inflation accounting.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER_IN,
    TRANSFER_OUT,
    TAX,
    MONEY_SINK,
    SHOP_BUY,
    SHOP_SELL,
    AUTO_SELL,
    JOB_REWARD,
    QUEST_REWARD,
    MINION_SELL,
    ADMIN_GIVE,
    ADMIN_TAKE,
    STARTING_BALANCE
}
