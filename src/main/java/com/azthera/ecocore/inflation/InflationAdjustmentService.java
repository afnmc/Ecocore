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

public final class InflationAdjustmentService {
    private final ShopItemRepository shopItemRepository;
    private final MoneySupplyTracker moneySupplyTracker;
    private final InflationCalculator inflationCalculator;
    private final TaxManager taxManager;
    private final EconomyStatisticsService statisticsService;
    private final SchedulerAdapter schedulerAdapter; // Tambahan
    private final Logger logger;
    private InflationConfig inflationConfig;
    private double currentTaxRateModifier = 0.0;

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

    public void runAdjustmentCycle(String currencyId, long windowStartMillis) {
        moneySupplyTracker.refreshSnapshot(currencyId, windowStartMillis).thenAccept(snapshot -> {
            statisticsService.getRecentTransactions(500).thenAccept(recentTransactions -> {
                double transactionVolume = recentTransactions.stream()
                    .mapToDouble(entry -> Math.abs(entry.getAmount()))
                    .sum();
                double effectiveRate = inflationCalculator.computeEffectiveInflationRate(snapshot, transactionVolume);
                InflationCalculator.InflationState state = inflationCalculator.classify(effectiveRate);
                applyAdjustment(state);
                
                // Broadcast Notifikasi
                broadcastInflationNotification(effectiveRate, state);
                
                logger.info(() -> "Inflation cycle: rate=" + String.format("%.4f", effectiveRate) + " state=" + state);
            });
        });
    }

    private void broadcastInflationNotification(double effectiveRate, InflationCalculator.InflationState state) {
        if (Math.abs(effectiveRate) < 0.01) return; // Abaikan jika stabil
        
        Component message;
        if (effectiveRate > 0) {
            message = Component.text("▴ Inflasi: Naik " + String.format("%.2f%%", effectiveRate * 100), NamedTextColor.RED);
        } else {
            message = Component.text("▾ Inflasi: Turun " + String.format("%.2f%%", Math.abs(effectiveRate) * 100), NamedTextColor.GREEN);
        }
        
        // Kirim ke semua player online
        schedulerAdapter.runSync(() -> Bukkit.getServer().sendMessage(message));
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

    public double getCurrentTaxRateModifier() {
        return currentTaxRateModifier;
    }
            }
