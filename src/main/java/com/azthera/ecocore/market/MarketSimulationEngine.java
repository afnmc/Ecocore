package com.azthera.ecocore.market;
 
import com.azthera.ecocore.config.MarketConfig;
import com.azthera.ecocore.data.model.ShopItemRecord;
import com.azthera.ecocore.data.repository.ShopItemRepository;
import com.azthera.ecocore.shop.PricingEngine;
 
import java.util.logging.Logger;
 
/**
 * Runs one full AI Market simulation pass: selects weighted actions via
 * {@code WeightedEconomyModel}, applies them through the same
 * {@code PricingEngine} used for real player transactions (so AI actions
 * behave identically to player buy/sell in terms of price impact), then
 * delegates to {@code MarketEventManager} for event lifecycle and modifier
 * application. Does not touch player balances — the AI market simulates
 * price pressure only, it does not spend or receive real currency.
 */
public final class MarketSimulationEngine {
 
    private final ShopItemRepository shopItemRepository;
    private final PricingEngine pricingEngine;
    private final WeightedEconomyModel weightedEconomyModel;
    private final MarketEventManager marketEventManager;
    private final Logger logger;
    private MarketConfig marketConfig;
 
    public MarketSimulationEngine(ShopItemRepository shopItemRepository, PricingEngine pricingEngine,
                                   WeightedEconomyModel weightedEconomyModel, MarketEventManager marketEventManager,
                                   MarketConfig marketConfig, Logger logger) {
        this.shopItemRepository = shopItemRepository;
        this.pricingEngine = pricingEngine;
        this.weightedEconomyModel = weightedEconomyModel;
        this.marketEventManager = marketEventManager;
        this.marketConfig = marketConfig;
        this.logger = logger;
    }
 
    public void reload(MarketConfig marketConfig) {
        this.marketConfig = marketConfig;
    }
 
    public void runSimulationTick() {
        marketEventManager.tickExpirations();
        marketEventManager.maybeStartRandomEvent();
 
        var candidates = shopItemRepository.getAllCached();
        if (candidates.isEmpty()) {
            return;
        }
 
        int actionCount = Math.max(1, (int) Math.round(marketConfig.getAiActionsPerTick()));
        var actions = weightedEconomyModel.selectActions(candidates, actionCount);
 
        for (WeightedEconomyModel.SimulatedAction action : actions) {
            shopItemRepository.getCached(action.itemId()).ifPresent(record -> applySimulatedAction(record, action));
        }
 
        marketEventManager.applyActiveEventModifiers();
        logger.fine(() -> "AI market simulation tick applied " + actions.size() + " simulated actions.");
    }
 
    private void applySimulatedAction(ShopItemRecord record, WeightedEconomyModel.SimulatedAction action) {
        double newPrice = action.isBuy()
            ? pricingEngine.onBuy(record, action.quantity())
            : pricingEngine.onSell(record, action.quantity());
 
        record.setCurrentPrice(newPrice);
        shopItemRepository.save(record);
    }
}
