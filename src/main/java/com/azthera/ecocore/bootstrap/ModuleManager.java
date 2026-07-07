package com.azthera.ecocore.bootstrap;
 
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
 
/**
 * Orchestrates the lifecycle of all registered {@link Module} instances.
 * Modules are enabled/disabled/reloaded in registration order, which allows
 * dependent modules to be registered after their dependencies.
 */
public final class ModuleManager {
 
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final Logger logger;
 
    public ModuleManager(Logger logger) {
        this.logger = logger;
    }
 
    public void registerModule(Module module) {
        modules.put(module.getName().toLowerCase(), module);
    }
 
    public void enableAll() {
        for (Module module : modules.values()) {
            try {
                module.enable();
                logger.info(() -> "Module enabled: " + module.getName());
            } catch (Exception exception) {
                logger.severe("Failed to enable module '" + module.getName() + "': " + exception.getMessage());
                throw new RuntimeException(exception);
            }
        }
    }
 
    public void disableAll() {
        for (Module module : modules.values()) {
            try {
                module.disable();
                logger.info(() -> "Module disabled: " + module.getName());
            } catch (Exception exception) {
                logger.severe("Failed to disable module '" + module.getName() + "': " + exception.getMessage());
            }
        }
    }
 
    public void reloadAll() {
        for (Module module : modules.values()) {
            try {
                module.reload();
                logger.info(() -> "Module reloaded: " + module.getName());
            } catch (Exception exception) {
                logger.severe("Failed to reload module '" + module.getName() + "': " + exception.getMessage());
            }
        }
    }
 
    public Module getModule(String name) {
        return modules.get(name.toLowerCase());
    }
 
    public boolean isModuleEnabled(String name) {
        Module module = getModule(name);
        return module != null && module.isEnabled();
    }
}
