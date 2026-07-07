package com.azthera.ecocore.api;
 
/**
 * Static access point for {@link EcoCoreAPI}, following the same pattern as
 * Vault's {@code Economy} provider and LuckPerms' {@code LuckPermsProvider}.
 * EcoCore itself calls {@link #register(EcoCoreAPI)} once during
 * {@code onEnable} after every service is fully constructed, and
 * {@link #unregister()} during {@code onDisable}. This is the one
 * intentional static holder in the entire plugin — a deliberate exception
 * to "no static abuse" because it exists specifically to serve as a stable,
 * conventional cross-plugin integration point, not as an internal service locator.
 */
public final class EcoCoreProvider {
 
    private static EcoCoreAPI instance;
 
    private EcoCoreProvider() {
    }
 
    public static void register(EcoCoreAPI api) {
        instance = api;
    }
 
    public static void unregister() {
        instance = null;
    }
 
    /**
     * @return the active {@link EcoCoreAPI} instance.
     * @throws IllegalStateException if EcoCore has not finished enabling yet,
     * or has already been disabled.
     */
    public static EcoCoreAPI get() {
        if (instance == null) {
            throw new IllegalStateException("EcoCoreAPI is not available. Is EcoCore installed and enabled?");
        }
        return instance;
    }
 
    public static boolean isAvailable() {
        return instance != null;
    }
}
