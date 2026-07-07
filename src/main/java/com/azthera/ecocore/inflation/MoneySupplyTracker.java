package com.azthera.ecocore.inflation;
 
import com.azthera.ecocore.economy.EconomyStatisticsService;
 
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
 
/**
 * Tracks the economy's total money supply (sum of all player balances) and
 * how much money was created versus removed over a rolling window, feeding
 * both figures to {@code VelocityCalculator} and {@code InflationCalculator}.
 * A snapshot is refreshed once per inflation calculation cycle rather than
 * queried live, since summing every account balance is a full-table
 * aggregate query best run periodically, not on every price calculation.
 */
public final class MoneySupplyTracker {
 
    private final EconomyStatisticsService statisticsService;
    private final AtomicReference<Snapshot> lastSnapshot = new AtomicReference<>(new Snapshot(0.0, 0.0, 0.0, 0L));
 
    public MoneySupplyTracker(EconomyStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }
 
    /**
     * Refreshes the snapshot: current total money supply, and net money
     * created (positive) or removed (negative) since {@code sinceMillis}.
     */
    public CompletableFuture<Snapshot> refreshSnapshot(String currencyId, long sinceMillis) {
        long now = System.currentTimeMillis();
 
        CompletableFuture<Double> totalSupplyFuture = statisticsService.getTotalMoneySupply(currencyId);
        CompletableFuture<Double> netCreatedFuture = statisticsService.getNetMoneyCreated(currencyId, sinceMillis, now);
 
        return totalSupplyFuture.thenCombine(netCreatedFuture, (totalSupply, netCreated) -> {
            double previousSupply = lastSnapshot.get().totalSupply();
            Snapshot snapshot = new Snapshot(totalSupply, netCreated, previousSupply, now);
            lastSnapshot.set(snapshot);
            return snapshot;
        });
    }
 
    public Snapshot getLastSnapshot() {
        return lastSnapshot.get();
    }
 
    /**
     * @param totalSupply sum of all player balances at snapshot time.
     * @param netCreatedSincePrevious money created minus money removed since the previous snapshot.
     * @param previousTotalSupply the total supply at the previous snapshot, for computing growth rate.
     * @param timestampMillis when this snapshot was taken.
     */
    public record Snapshot(double totalSupply, double netCreatedSincePrevious, double previousTotalSupply,
                            long timestampMillis) {
 
        public double getGrowthRate() {
            if (previousTotalSupply <= 0) {
                return 0.0;
            }
            return (totalSupply - previousTotalSupply) / previousTotalSupply;
        }
    }
}
