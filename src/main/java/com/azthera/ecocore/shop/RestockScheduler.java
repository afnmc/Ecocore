package com.azthera.ecocore.shop;
 
import com.azthera.ecocore.config.ShopConfig;
import com.azthera.ecocore.data.model.ShopItemRecord;
import com.azthera.ecocore.data.repository.ShopItemRepository;
import com.azthera.ecocore.integration.EcoScheduledTask;
import com.azthera.ecocore.integration.SchedulerAdapter;
 
import java.util.logging.Logger;
 
/**
 * Periodically checks every cached shop item and restocks any that have
 * passed their configured restock interval. Runs on the global/async
 * scheduler since it only touches cached, thread-safe repository state.
 */
public final class RestockScheduler {
 
    private static final long CHECK_INTERVAL_TICKS = 20L * 60L;
 
    private final ShopItemRepository shopItemRepository;
    private final StockManager stockManager;
    private final ShopConfig shopConfig;
    private final SchedulerAdapter schedulerAdapter;
    private final Logger logger;
    private EcoScheduledTask task;
 
    public RestockScheduler(ShopItemRepository shopItemRepository, StockManager stockManager, ShopConfig shopConfig,
                             SchedulerAdapter schedulerAdapter, Logger logger) {
        this.shopItemRepository = shopItemRepository;
        this.stockManager = stockManager;
        this.shopConfig = shopConfig;
        this.schedulerAdapter = schedulerAdapter;
        this.logger = logger;
    }
 
    public void start() {
        stop();
        task = schedulerAdapter.runAsyncRepeating(this::checkAndRestock, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
        logger.info("Restock scheduler started.");
    }
 
    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
 
    private void checkAndRestock() {
        long now = System.currentTimeMillis();
        for (ShopItemRecord record : shopItemRepository.getAllCached()) {
            if (stockManager.isDueForRestock(record, now)) {
                int fullStock = shopConfig.getDefaultStock();
                stockManager.restockToFull(record, fullStock, now);
                logger.fine(() -> "Restocked shop item: " + record.getItemId());
            }
        }
    }
}
