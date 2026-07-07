package com.azthera.ecocore.jobs;
 
/**
 * The seven job types available in EcoCore. Each corresponds to a section
 * under modules/jobs.yml#jobs and to a distinct action-detection listener
 * (block break for Mining/Woodcutting/Farming, entity kill for
 * Hunting/Exploring milestones, fish catch for Fishing, structure completion
 * heuristics for Builder).
 */
public enum JobType {
    MINING,
    WOODCUTTING,
    FISHING,
    FARMING,
    HUNTING,
    EXPLORING,
    BUILDER
}
