package com.azthera.ecocore.bootstrap;
 
/**
 * Represents a lifecycle-managed feature module within EcoCore.
 * Each module is independent and can be enabled, disabled, or reloaded
 * without requiring a server restart.
 */
public interface Module {
 
    /**
     * @return unique, human-readable name of this module (used for logging and lookup).
     */
    String getName();
 
    /**
     * Called once during plugin startup, or when the module is toggled on via config reload.
     */
    void enable();
 
    /**
     * Called during plugin shutdown, or when the module is toggled off via config reload.
     */
    void disable();
 
    /**
     * Called when {@code /eco reload} is executed. Implementations should refresh
     * their configuration state without tearing down runtime data.
     */
    void reload();
 
    /**
     * @return whether this module is currently active. Modules that are disabled
     * via configuration should return {@code false} here.
     */
    default boolean isEnabled() {
        return true;
    }
}
