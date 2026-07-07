package com.azthera.ecocore.market;
 
import com.azthera.ecocore.bootstrap.Module;
import com.azthera.ecocore.config.MarketConfig;
import com.azthera.ecocore.integration.EcoScheduledTask;
import com.azthera.ecocore.integration.SchedulerAdapter;
 
import java.util.logging.Logger;
 
/**
 * Module wrapper that schedules {@code MarketSimulationEngine#runSimulationTick}
 * to run repeatedly on the async scheduler at the interval configured in
 * modules/market.yml. This is the sole entry point that drives the AI Market
 * over time; nothing else calls the simulation engine directly.
 */
public final class MarketTickTask implements Module {
 
    private final MarketSimulationEngine simulationEngine;
    private final MarketEventManager marketEventManager;
    private final SchedulerAdapter schedulerAdapter;
    private final Logger logger;
    private MarketConfig marketConfig;
    private EcoScheduledTask scheduledTask;
    private boolean enabled;
 
    public MarketTickTask(MarketSimulationEngine simulationEngine, MarketEventManager marketEventManager,
                           SchedulerAdapter schedulerAdapter, MarketConfig marketConfig, Logger logger) {
        this.simulationEngine = simulationEngine;
        this.marketEventManager = marketEventManager;
        this.schedulerAdapter = schedulerAdapter;
        this.marketConfig = marketConfig;
        this.logger = logger;
    }
 
    @Override
    public String getName() {
        return "market";
    }
 
    @Override
    public void enable() {
        this.enabled = true;
        marketEventManager.initialize();
 
        long intervalTicks = marketConfig.getTickIntervalSeconds() * 20L;
        scheduledTask = schedulerAdapter.runAsyncRepeating(
            simulationEngine::runSimulationTick, intervalTicks, intervalTicks
        );
        logger.info(() -> "Market simulation scheduled every " + marketConfig.getTickIntervalSeconds() + " seconds.");
    }
 
    @Override
    public void disable() {
        this.enabled = false;
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel();
        }
    }
 
    @Override
    public void reload() {
        disable();
        enable();
    }
 
    @Override
    public boolean isEnabled() {
        return enabled;
    }
 
    public void updateMarketConfig(MarketConfig marketConfig) {
        this.marketConfig = marketConfig;
        simulationEngine.reload(marketConfig);
        marketEventManager.reload(marketConfig);
    }
}
