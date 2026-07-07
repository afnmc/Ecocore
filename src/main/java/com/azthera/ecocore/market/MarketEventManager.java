package com.azthera.ecocore.market;
 
import com.azthera.ecocore.config.MarketConfig;
import com.azthera.ecocore.data.model.MarketEventRecord;
import com.azthera.ecocore.data.repository.MarketEventRepository;
import com.azthera.ecocore.shop.ShopManager;
 
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;
 
/**
 * Decides when to start and end market-wide events (Boom, Crash, Festival,
 * Mining/Farming/Fishing Week, Inflation, Deflation), applying each event's
 * configured price/demand modifiers on top of every shop item's own
 * elasticity-driven price via {@code PricingEngine.applyExternalModifier}.
 * Only one instance of a given event type may be active at a time.
 */
public final class MarketEventManager {
 
    private final MarketEventRepository marketEventRepository;
    private final ShopManager shopManager;
    private final com.azthera.ecocore.shop.PricingEngine pricingEngine;
    private final com.azthera.ecocore.data.repository.ShopItemRepository shopItemRepository;
    private final ServerNewsBroadcaster newsBroadcaster;
    private final Logger logger;
    private final Random random = new Random();
    private MarketConfig marketConfig;
 
    public MarketEventManager(MarketEventRepository marketEventRepository, ShopManager shopManager,
                               com.azthera.ecocore.shop.PricingEngine pricingEngine,
                               com.azthera.ecocore.data.repository.ShopItemRepository shopItemRepository,
                               ServerNewsBroadcaster newsBroadcaster, MarketConfig marketConfig, Logger logger) {
        this.marketEventRepository = marketEventRepository;
        this.shopManager = shopManager;
        this.pricingEngine = pricingEngine;
        this.shopItemRepository = shopItemRepository;
        this.newsBroadcaster = newsBroadcaster;
        this.marketConfig = marketConfig;
        this.logger = logger;
    }
 
    public void reload(MarketConfig marketConfig) {
        this.marketConfig = marketConfig;
    }
 
    public void initialize() {
        marketEventRepository.loadActiveIntoCacheAsync();
    }
 
    /**
     * Rolls whether a new random event should start, weighted by each
     * configured event's weight, skipping event types that are already active.
     */
    public void maybeStartRandomEvent() {
        List<MarketConfig.MarketEventDefinition> eligible = marketConfig.getEventDefinitions().values().stream()
            .filter(MarketConfig.MarketEventDefinition::enabled)
            .filter(def -> marketEventRepository.getCachedActive(def.id()).isEmpty())
            .toList();
 
        if (eligible.isEmpty()) {
            return;
        }
 
        double totalWeight = eligible.stream().mapToDouble(MarketConfig.MarketEventDefinition::weight).sum();
        if (totalWeight <= 0) {
            return;
        }
 
        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (MarketConfig.MarketEventDefinition definition : eligible) {
            cumulative += definition.weight();
            if (roll <= cumulative) {
                startEvent(definition);
                return;
            }
        }
    }
 
    private void startEvent(MarketConfig.MarketEventDefinition definition) {
        long now = System.currentTimeMillis();
        long durationMillis = definition.minDurationSeconds() * 1000L
            + (long) (random.nextDouble() * (definition.maxDurationSeconds() - definition.minDurationSeconds()) * 1000L);
 
        MarketEventRecord record = new MarketEventRecord(
            0L, definition.id(), now, now + durationMillis,
            definition.priceModifier(), definition.demandModifier(), true
        );
 
        marketEventRepository.startEvent(record);
 
        MarketEventType eventType = parseEventType(definition.id());
        if (eventType != null) {
            newsBroadcaster.announceEventStarted(eventType);
        }
        logger.info(() -> "Market event started: " + definition.id());
    }
 
    /**
     * Checks all currently active events for expiry and deactivates them,
     * applying no further modifier once ended (prices settle back toward
     * their own elasticity-driven equilibrium).
     */
    public void tickExpirations() {
        long now = System.currentTimeMillis();
        for (MarketEventRecord record : List.copyOf(marketEventRepository.getAllCachedActive())) {
            if (record.getEndsAtMillis() <= now) {
                marketEventRepository.endEvent(record);
                MarketEventType eventType = parseEventType(record.getEventType());
                if (eventType != null) {
                    newsBroadcaster.announceEventEnded(eventType);
                }
                logger.info(() -> "Market event ended: " + record.getEventType());
            }
        }
        marketEventRepository.deactivateExpiredAsync(now);
    }
 
    /**
     * Applies the aggregate price modifier of all currently active events to
     * every cached shop item. Called once per simulation tick, after the AI
     * market's own buy/sell simulation has run.
     */
    public void applyActiveEventModifiers() {
        List<MarketEventRecord> activeEvents = List.copyOf(marketEventRepository.getAllCachedActive());
        if (activeEvents.isEmpty()) {
            return;
        }
 
        double combinedPriceModifier = 1.0;
        for (MarketEventRecord event : activeEvents) {
            combinedPriceModifier *= event.getPriceModifier();
        }
 
        for (var record : shopItemRepository.getAllCached()) {
            double adjustedPrice = pricingEngine.applyExternalModifier(record, combinedPriceModifier);
            record.setCurrentPrice(adjustedPrice);
            shopItemRepository.save(record);
        }
    }
 
    public Optional<MarketEventRecord> getActiveEvent(MarketEventType type) {
        return marketEventRepository.getCachedActive(type.name().toLowerCase());
    }
 
    private MarketEventType parseEventType(String id) {
        try {
            return MarketEventType.valueOf(id.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
