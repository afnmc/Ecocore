package com.azthera.ecocore.market;
 
import com.azthera.ecocore.data.model.ShopItemRecord;
 
import java.util.Collection;
import java.util.List;
import java.util.Random;
 
/**
 * Selects which shop items the AI Market should act on during a simulation
 * tick, and whether each selected action is a simulated buy or sell. Selection
 * is weighted (not uniformly random) using {@link DemandSupplyCalculator}, so
 * the simulation tends to nudge items that currently have room to move rather
 * than blindly hammering already-extreme prices — this is the "bukan random
 * murni, gunakan weighted economy" requirement.
 */
public final class WeightedEconomyModel {
 
    private final DemandSupplyCalculator demandSupplyCalculator;
    private final Random random = new Random();
 
    public WeightedEconomyModel(DemandSupplyCalculator demandSupplyCalculator) {
        this.demandSupplyCalculator = demandSupplyCalculator;
    }
 
    /**
     * Picks up to {@code actionCount} items from {@code candidates}, weighted
     * by {@code computeActionWeight}, and returns the chosen simulated actions.
     */
    public List<SimulatedAction> selectActions(Collection<ShopItemRecord> candidates, int actionCount) {
        List<ShopItemRecord> pool = List.copyOf(candidates);
        if (pool.isEmpty() || actionCount <= 0) {
            return List.of();
        }
 
        double[] weights = new double[pool.size()];
        double totalWeight = 0.0;
        for (int i = 0; i < pool.size(); i++) {
            weights[i] = demandSupplyCalculator.computeActionWeight(pool.get(i));
            totalWeight += weights[i];
        }
 
        List<SimulatedAction> actions = new java.util.ArrayList<>();
        for (int action = 0; action < actionCount; action++) {
            ShopItemRecord chosen = weightedPick(pool, weights, totalWeight);
            if (chosen == null) {
                continue;
            }
            boolean isBuy = decideDirection(chosen);
            int quantity = 1 + random.nextInt(5);
            actions.add(new SimulatedAction(chosen.getItemId(), isBuy, quantity));
        }
        return actions;
    }
 
    private ShopItemRecord weightedPick(List<ShopItemRecord> pool, double[] weights, double totalWeight) {
        if (totalWeight <= 0) {
            return pool.get(random.nextInt(pool.size()));
        }
        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (int i = 0; i < pool.size(); i++) {
            cumulative += weights[i];
            if (roll <= cumulative) {
                return pool.get(i);
            }
        }
        return pool.get(pool.size() - 1);
    }
 
    /**
     * Biases the AI's simulated direction toward correcting existing pressure:
     * if demand already exceeds supply, the AI is more likely to "sell" to cool
     * it down, and vice versa — modeling a market that self-corrects, not one
     * that amplifies swings indefinitely.
     */
    private boolean decideDirection(ShopItemRecord record) {
        double netPressure = demandSupplyCalculator.getNetPressure(record);
        double sellProbability = 0.5 + Math.min(0.4, Math.max(-0.4, netPressure / 100.0));
        return random.nextDouble() > sellProbability;
    }
 
    public record SimulatedAction(String itemId, boolean isBuy, int quantity) {
    }
}
