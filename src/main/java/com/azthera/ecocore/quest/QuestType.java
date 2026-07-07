package com.azthera.ecocore.quest;
 
/**
 * Rotation category a quest belongs to. Daily/Weekly/Monthly quests rotate
 * on a schedule; Story quests are fixed, one-time, sequential; Repeatable
 * quests have no expiry and can be re-accepted immediately after completion;
 * Random quests are drawn ad-hoc rather than from a fixed rotation pool.
 */
public enum QuestType {
    DAILY,
    WEEKLY,
    MONTHLY,
    STORY,
    REPEATABLE,
    RANDOM,
    CHAIN
}
