package com.azthera.ecocore.inflation;

import com.azthera.ecocore.config.InflationConfig;
import com.azthera.ecocore.data.model.ShopItemRecord;
import com.azthera.ecocore.data.repository.ShopItemRepository;
import com.azthera.ecocore.economy.EconomyStatisticsService;
import com.azthera.ecocore.economy.TaxManager;
import com.azthera.ecocore.integration.SchedulerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import java.util.logging.Logger;

/**
 * Consumes the classified {@link InflationCalculator.InflationState} and
 * gently nudges NPC shop prices, (future) upgrade costs, and tax rate in
 * response — using small configured step sizes rather than sharp jumps, and
 * always clamped within configured bounds, matching the "harga NPC naik
 * sedikit... semua configurable... perlahan turun" requirement.
 *
 * <p><b>Since V3:</b> Added {@link SchedulerAdapter} for broadcasting inflation
 * notifications to all online players with color-coded symbols (▴ red for up,
 * ▾ green for down). Also tracks {@code lastEffectiveRate} and {@code lastState}
 * so the {@code /inflation status} command can display current inflation state
 * without needing to recalculate.</p>
 */
public final class InflationAdjustmentService {
    private final ShopItemRepository shopItemRepository;
    private final MoneySupplyTracker moneySupplyTracker;
    private final InflationCalculator inflationCalculator;
    private final TaxManager taxManager;
    private final EconomyStatisticsService statisticsService;
    private final SchedulerAdapter schedulerAdapter;
    private final Logger logger;
    private InflationConfig inflationConfig;
    private double currentTaxRateModifier = 0.0;
    
    // Track for /inflation status command
    private double lastEffectiveRate = 0.0;
    private InflationCalculator.InflationState lastState = InflationCalculator.InflationState.STABLE;

    public InflationAdjustmentService(ShopItemRepository shopItemRepository, MoneySupplyTracker moneySupplyTracker,
                                       InflationCalculator inflationCalculator, TaxManager taxManager,
                                       EconomyStatisticsService statisticsService, InflationConfig inflationConfig,
                                       Logger logger, SchedulerAdapter schedulerAdapter) {
        this.shopItemRepository = shopItemRepository;
        this.moneySupplyTracker = moneySupplyTracker;
        this.inflationCalculator = inflationCalculator;
        this.taxManager = taxManager;
        this.statisticsService = statisticsService;
        this.inflationConfig = inflationConfig;
        this.logger = logger;
        this.schedulerAdapter = schedulerAdapter;
    }

    public void reload(InflationConfig inflationConfig) {
        this.inflationConfig = inflationConfig;
    }

    /**
     * Runs one full inflation calculation + adjustment cycle: refreshes the
     * money supply snapshot, classifies the current state, then applies a
     * small proportional nudge to every cached shop item's basePrice
     * (bounded by configured min/max NPC price multipliers) and to the tax rate.
     */
    public void runAdjustmentCycle(String currencyId, long windowStartMillis) {
        moneySupplyTracker.refreshSnapshot(currencyId, windowStartMillis).thenAccept(snapshot -> {
            statisticsService.getRecentTransactions(500).thenAccept(recentTransactions -> {
                double transactionVolume = recentTransactions.stream()
                    .mapToDouble(entry -> Math.abs(entry.getAmount()))
                    .sum();
                double effectiveRate = inflationCalculator.computeEffectiveInflationRate(snapshot, transactionVolume);
                InflationCalculator.InflationState state = inflationCalculator.classify(effectiveRate);
                
                // Track for /inflation status command
                this.lastEffectiveRate = effectiveRate;
                this.lastState = state;
                
                applyAdjustment(state);
                broadcastInflationNotification(effectiveRate, state);
                
                logger.info(() -> "Inflation cycle: rate=" + String.format("%.4f", effectiveRate) + " state=" + state);
            });
        });
    }

    private void applyAdjustment(InflationCalculator.InflationState state) {
        double npcStep = inflationConfig.getNpcPriceAdjustmentStep();
        double taxStep = inflationConfig.getTaxAdjustmentStep();
        double npcDirection = switch (state) {
            case HIGH_INFLATION -> 1.0;
            case DEFLATION -> -1.0;
            case STABLE -> 0.0;
        };
        if (npcDirection != 0.0) {
            adjustAllShopBasePrices(npcDirection * npcStep);
            adjustTaxRateModifier(npcDirection * taxStep);
        }
    }

    private void adjustAllShopBasePrices(double fractionalChange) {
        double minMultiplier = inflationConfig.getMinNpcPriceMultiplier();
        double maxMultiplier = inflationConfig.getMaxNpcPriceMultiplier();
        for (ShopItemRecord record : shopItemRepository.getAllCached()) {
            double newBasePrice = record.getBasePrice() * (1.0 + fractionalChange);
            double lowerBound = record.getMinPrice() * minMultiplier / maxMultiplier;
            double upperBound = record.getMaxPrice() * maxMultiplier;
            newBasePrice = Math.max(lowerBound, Math.min(upperBound, newBasePrice));
            record.setBasePrice(newBasePrice);
            shopItemRepository.save(record);
        }
    }

    private void adjustTaxRateModifier(double delta) {
        currentTaxRateModifier = Math.max(-0.02, Math.min(0.02, currentTaxRateModifier + delta));
    }

    /**
     * @return the cumulative tax-rate adjustment produced by inflation cycles
     * so far, to be added on top of the base configured tax rate. Kept
     * separate from {@code TaxManager}'s own rate to avoid inflation
     * permanently baking itself into the base config value.
     */
    public double getCurrentTaxRateModifier() {
        return currentTaxRateModifier;
    }

    /**
     * @return the last calculated effective inflation rate, for display in
     * the /inflation status command without needing to recalculate.
     */
    public double getLastEffectiveRate() {
        return lastEffectiveRate;
    }

    /**
     * @return the last classified inflation state (HIGH_INFLATION, STABLE, or DEFLATION),
     * for display in the /inflation status command.
     */
    public InflationCalculator.InflationState getLastState() {
        return lastState;
    }

    /**
     * Broadcasts a color-coded inflation notification to all online players.
     * Only broadcasts if the rate change is significant (> 1%) to avoid spam.
     * 
     * <p>Format:</p>
     * <ul>
     *   <li>Naik: <span style="color:red">▴ Inflasi: Naik X.XX%</span></li>
     *   <li>Turun: <span style="color:green">▾ Inflasi: Turun X.XX%</span></li>
     * </ul>
     */
    private void broadcastInflationNotification(double effectiveRate, InflationCalculator.InflationState state) {
        // Only broadcast if rate change is significant (> 1%)
        if (Math.abs(effectiveRate) < 0.01) return;
        
        Component message;
        if (effectiveRate > 0) {
            message = Component.text("▴ Inflasi: Naik " + String.format("%.2f%%", effectiveRate * 100), NamedTextColor.RED);
        } else {
            message = Component.text("▾ Inflasi: Turun " + String.format("%.2f%%", Math.abs(effectiveRate) * 100), NamedTextColor.GREEN);
        }
        
        schedulerAdapter.runSync(() -> Bukkit.getServer().sendMessage(message));
    }
    }
