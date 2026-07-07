package com.azthera.ecocore.sell;
 
import com.azthera.ecocore.economy.EconomyService;
import com.azthera.ecocore.economy.TransactionType;
import com.azthera.ecocore.shop.ShopManager;
import com.azthera.ecocore.shop.ShopTransactionService;
import com.azthera.ecocore.util.Result;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;
 
import java.util.ArrayList;
import java.util.List;
 
/**
 * Orchestrates selling items the player is holding or carrying in their
 * inventory. Delegates actual pricing/payout to {@code ShopTransactionService}
 * so sell prices always match the same dynamic shop pricing the Dynamic Shop
 * GUI shows — this module never invents its own price. Multiplier and
 * whitelist/blacklist filtering are layered on top via
 * {@code SellMultiplierResolver} and {@code SellFilterManager}.
 */
public final class SellService {
 
    private final ShopManager shopManager;
    private final ShopTransactionService shopTransactionService;
    private final SellFilterManager sellFilterManager;
    private final SellMultiplierResolver sellMultiplierResolver;
    private final EconomyService economyService;
 
    public SellService(ShopManager shopManager, ShopTransactionService shopTransactionService,
                        SellFilterManager sellFilterManager, SellMultiplierResolver sellMultiplierResolver,
                        EconomyService economyService) {
        this.shopManager = shopManager;
        this.shopTransactionService = shopTransactionService;
        this.sellFilterManager = sellFilterManager;
        this.sellMultiplierResolver = sellMultiplierResolver;
        this.economyService = economyService;
    }
 
    public Result<Double> sellHand(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType().isAir()) {
            return Result.failure("Kamu tidak memegang item apapun.");
        }
        if (!sellFilterManager.isSellable(handItem.getType())) {
            return Result.failure("Item ini tidak dapat dijual.");
        }
 
        String itemId = handItem.getType().name().toLowerCase();
        int quantity = handItem.getAmount();
 
        Result<Double> sellResult = executeSellWithMultiplier(player, itemId, handItem, quantity);
        sellResult.onSuccess(payout -> handItem.setAmount(0));
        return sellResult;
    }
 
    public Result<Double> sellInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        double totalPayout = 0.0;
        List<ItemStack> soldStacks = new ArrayList<>();
 
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (!sellFilterManager.isSellable(stack.getType())) {
                continue;
            }
 
            String itemId = stack.getType().name().toLowerCase();
            if (shopManager.getDefinition(itemId).isEmpty()) {
                continue;
            }
 
            Result<Double> stackResult = executeSellWithMultiplier(player, itemId, stack, stack.getAmount());
            if (stackResult.isSuccess()) {
                totalPayout += stackResult.orElse(0.0);
                soldStacks.add(stack);
            }
        }
 
        if (soldStacks.isEmpty()) {
            return Result.failure("Tidak ada item yang dapat dijual di inventory kamu.");
        }
 
        for (ItemStack sold : soldStacks) {
            sold.setAmount(0);
        }
 
        return Result.success(totalPayout);
    }
 
    private Result<Double> executeSellWithMultiplier(Player player, String itemId, ItemStack stack, int quantity) {
        if (shopManager.getDefinition(itemId).isEmpty()) {
            return Result.failure("Item ini tidak terdaftar di shop.");
        }
 
        double multiplier = sellMultiplierResolver.resolveMultiplier(player, stack.getType());
 
        Result<Double> baseSellResult = shopTransactionService.sell(player.getUniqueId(), itemId, quantity);
        if (baseSellResult.isFailure() || multiplier == 1.0) {
            return baseSellResult;
        }
 
        double basePayout = baseSellResult.orElse(0.0);
        double bonusAmount = basePayout * (multiplier - 1.0);
        if (bonusAmount > 0) {
            economyService.deposit(player.getUniqueId(), bonusAmount, TransactionType.SHOP_SELL, "Sell bonus multiplier");
        }
 
        return Result.success(basePayout + bonusAmount);
    }

    /**
     * Sells a specific material/quantity directly by itemId, bypassing hand/inventory
     * scanning. Used by {@code AutoSellManager} when a job action produces a drop
     * that should be sold immediately rather than placed in the inventory.
     */
    public Result<Double> sellByItemId(Player player, String itemId, Material material, int quantity) {
        if (!sellFilterManager.isSellable(material)) {
            return Result.failure("Item ini tidak dapat dijual.");
        }
        if (shopManager.getDefinition(itemId).isEmpty()) {
            return Result.failure("Item ini tidak terdaftar di shop.");
        }

        double multiplier = sellMultiplierResolver.resolveMultiplier(player, material);
        Result<Double> baseSellResult = shopTransactionService.sell(player.getUniqueId(), itemId, quantity);
        if (baseSellResult.isFailure() || multiplier == 1.0) {
            return baseSellResult;
        }

        double basePayout = baseSellResult.orElse(0.0);
        double bonusAmount = basePayout * (multiplier - 1.0);
        if (bonusAmount > 0) {
            economyService.deposit(player.getUniqueId(), bonusAmount, TransactionType.SHOP_SELL, "Sell bonus multiplier");
        }
        return Result.success(basePayout + bonusAmount);
    }
}
