package com.azthera.ecocore.bootstrap;
 
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
 
/**
 * Lightweight, manual dependency container used exclusively during the plugin's
 * composition phase (inside {@code EcoCorePlugin#onEnable}). This is NOT a runtime
 * service locator meant to be called from arbitrary classes — dependencies should
 * always be injected via constructors. This registry only exists to make the
 * wiring phase in the composition root readable and centralized.
 */
public final class ServiceRegistry {
 
    private final Map<Class<?>, Object> services = new LinkedHashMap<>();
 
    public <T> void register(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        if (services.containsKey(type)) {
            throw new IllegalStateException("Service already registered for type: " + type.getName());
        }
        services.put(type, instance);
    }
 
    public <T> T get(Class<T> type) {
        Object instance = services.get(type);
        if (instance == null) {
            throw new IllegalStateException("No service registered for type: " + type.getName());
        }
        return type.cast(instance);
    }
 
    public <T> boolean isRegistered(Class<T> type) {
        return services.containsKey(type);
    }
 
    public void clear() {
        services.clear();
    }
}
