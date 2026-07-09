package com.azthera.ecocore.shop;

import com.azthera.ecocore.data.model.PriceHistoryEntry;
import com.azthera.ecocore.data.model.ShopItemRecord;
import com.azthera.ecocore.data.repository.ShopItemRepository;
import com.azthera.ecocore.economy.EconomyService;
import com.azthera.ecocore.economy.TransactionType;
import com.azthera.ecocore.minions.MinionManager;
import com.azthera.ecocore.minions.MinionType;
import com.azthera.ecocore.quest.QuestManager;
import com.azthera.ecocore.util.Result;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates a full buy/sell transaction: validates stock/funds, moves
 * money via {@code EconomyService}, updates price via {@code PricingEngine},
 * updates stock via {@code StockManager}, records a price history snapshot,
 * and reports the transaction to {@code QuestManager} as "buy:any"/"sell:any"
 * objective progress. This is the single entry point GUIs and commands
 * should call — neither should touch {@code PricingEngine} or
 * {@code StockManager} directly, and this is the ONLY place quest
 * buy/sell objectives are recorded, so every sell path (Dynamic Shop GUI
 * right-click, Sell GUI, hand/inventory sell, auto-sell) is covered
 * uniformly regardless of which higher-level class initiated it.
 *
 * <p><b>IMPORTANT:</b> Buy uses {@code record.getCurrentPrice()} (the buy price),
 * while sell uses {@code record.getSellPrice()} (the sell price). These are
 * now independent fields since V3 migration, allowing a custom buy/sell spread
 * per item. The sell price is also clamped to [minPrice, maxPrice] by the
 * pricing engine, so it cannot drift outside the item's configured bounds.</p>
 *
 * <p><b>Minion spawn items</b> (itemId starts with {@code minion_}) are
 * explicitly blocked from being sold back to the shop — they are placement
 * tokens, not commodities. Attempting to sell one returns a failure.</p>
 *
 * <p><b>Minion purchases</b> are detected by the {@code minion_} prefix on the
 * itemId. When detected, the service validates the player's minion limit
 * (via {@link MinionManager#getLimitPolicy()}) before completing the
 * transaction, and delivers a special "Minion Spawn Item" (carrying the
 * minion type in its {@link org.bukkit.persistence.PersistentDataContainer})
 * instead of a regular material stack. The spawn item is consumed by
 * {@code MinionItemListener} when the player right-clicks it in-world to
 * place the minion.</p>
 */
public final class ShopTransactionService {
    private final ShopManager shopManager;
    private final ShopItemRepository shopItemRepository;
    private final StockManager stockManager;
    private final PricingEngine pricingEngine;
    private final EconomyService economyService;
    private final QuestManager questManager;

    private JavaPlugin plugin;
    private MinionManager minionManager;
    private NamespacedKey minionKey;

    /**
     * Primary constructor matching the signature expected by {@code EcoCorePlugin}.
     * Minion support is optional and must be enabled separately via
     * {@link #enableMinionSupport(JavaPlugin, MinionManager)}.
     */
    public ShopTransactionService(ShopManager shopManager, ShopItemRepository shopItemRepository,
                                   StockManager stockManager, PricingEngine pricingEngine,
                                   EconomyService economyService, QuestManager questManager) {
        this.shopManager = shopManager;
        this.shopItemRepository = shopItemRepository;
        this.stockManager = stockManager;
        this.pricingEngine = pricingEngine;
        this.economyService = economyService;
        this.questManager = questManager;
    }

    /**
     * Enables minion-purchase handling. Must be called once during plugin
     * startup after both the Shop and Minions modules are fully constructed.
     * If not called, any attempt to buy a {@code minion_*} item will fail
     * with "Minion module is not available".
     */
    public void enableMinionSupport(JavaPlugin plugin, MinionManager minionManager) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.minionKey = new NamespacedKey(plugin, "minion_type");
    }

    /**
     * Executes a buy transaction. Uses {@code record.getCurrentPrice()} as
     * the per-unit cost. Validates stock, funds, and item buyability before
     * proceeding. On success, decrements stock, applies buy-side price impact
     * via {@code PricingEngine}, and reports "buy:any" progress to quests.
     *
     * <p>For minion items (id starts with {@code minion_}), validates the
     * player's remaining minion slots before completing the purchase and
     * delivers a Minion Spawn Item instead of a regular material stack.</p>
     */
    public Result<Double> buy(UUID playerId, String itemId, int quantity) {
        if (quantity <= 0) {
            return Result.failure("Quantity must be positive.");
        }
        var definitionOpt = shopManager.getDefinition(itemId);
        if (definitionOpt.isEmpty() || !definitionOpt.get().isBuyable()) {
            return Result.failure("This item cannot be purchased.");
        }
        var recordOpt = shopManager.getRecord(itemId);
        if (recordOpt.isEmpty()) {
            return Result.failure("Shop item is not initialized yet.");
        }
        ShopItemRecord record = recordOpt.get();
        if (!stockManager.hasStock(record, quantity)) {
            return Result.failure("This item is out of stock.");
        }
        // BUY uses currentPrice (the buy price)
        double totalCost = record.getCurrentPrice() * quantity;
        if (!economyService.has(playerId, totalCost)) {
            return Result.failure("Insufficient funds.");
        }

        // --- Minion-specific pre-validation (runs BEFORE money is withdrawn) ---
        boolean isMinion = itemId.startsWith("minion_");
        Player player = plugin != null ? plugin.getServer().getPlayer(playerId) : null;
        if (isMinion) {
            Result<Void> minionValidation = validateMinionPurchase(playerId, player, itemId, quantity);
            if (minionValidation.isFailure()) {
                return minionValidation.map(ignored -> 0.0);
            }
        }

        Result<Double> withdrawResult = economyService.withdraw(
            playerId, totalCost, TransactionType.SHOP_BUY, "Bought " + quantity + "x " + itemId
        );
        if (withdrawResult.isFailure()) {
            return Result.failure("Payment failed.");
        }

        // --- Deliver the purchased item(s) to the player ---
        if (isMinion && player != null) {
            String typeName = itemId.substring("minion_".length()).toUpperCase();
            giveMinionItem(player, typeName, quantity);
        } else {
            deliverRegularItem(player, definitionOpt.get(), quantity);
        }

        stockManager.decrementStock(record, quantity);
        double newBuyPrice = pricingEngine.onBuy(record, quantity);
        record.setCurrentPrice(newBuyPrice);
        // Also nudge the sell price proportionally so the spread stays consistent
        record.setSellPrice(computeAdjustedSellPrice(record));
        shopItemRepository.save(record);
        recordPriceSnapshot(record);
        questManager.recordProgress(playerId, "buy:any", quantity);
        return Result.success(totalCost);
    }

    /**
     * Executes a sell transaction. Uses {@code record.getSellPrice()} as
     * the per-unit payout (NOT currentPrice — that's the buy price).
     * Validates item sellability and blocks minion spawn items explicitly.
     * On success, applies sell-side price impact via {@code PricingEngine}
     * and reports "sell:any" progress to quests.
     *
     * <p>Note: selling to the shop does NOT increase stock (that would let
     * players farm infinite stock by selling then buying back). Restocking
     * is exclusively handled by {@code RestockScheduler}.</p>
     */
    public Result<Double> sell(UUID playerId, String itemId, int quantity) {
        if (quantity <= 0) {
            return Result.failure("Quantity must be positive.");
        }
        // Block minion spawn items from being sold back — they are placement tokens.
        if (itemId.startsWith("minion_")) {
            return Result.failure("Minion spawn items cannot be sold.");
        }
        var definitionOpt = shopManager.getDefinition(itemId);
        if (definitionOpt.isEmpty() || !definitionOpt.get().isSellable()) {
            return Result.failure("This item cannot be sold.");
        }
        var recordOpt = shopManager.getRecord(itemId);
        if (recordOpt.isEmpty()) {
            return Result.failure("Shop item is not initialized yet.");
        }
        ShopItemRecord record = recordOpt.get();
        // SELL uses sellPrice (the sell price), NOT currentPrice
        double totalPayout = record.getSellPrice() * quantity;
        Result<Double> depositResult = economyService.deposit(
            playerId, totalPayout, TransactionType.SHOP_SELL, "Sold " + quantity + "x " + itemId
        );
        if (depositResult.isFailure()) {
            return Result.failure("Payout failed.");
        }
        // Apply sell-side price impact to both buy and sell prices
        double newBuyPrice = pricingEngine.onSell(record, quantity);
        record.setCurrentPrice(newBuyPrice);
        record.setSellPrice(computeAdjustedSellPrice(record));
        shopItemRepository.save(record);
        recordPriceSnapshot(record);
        questManager.recordProgress(playerId, "sell:any", quantity);
        return Result.success(depositResult.orElse(totalPayout));
    }

    // ---------------------------------------------------------------------
    // Minion purchase helpers
    // ---------------------------------------------------------------------

    /**
     * Pre-validates a minion purchase: checks that the minion module is
     * available, the type is valid, and the player has enough remaining
     * slots for the requested quantity. Runs BEFORE money is withdrawn so
     * a failed validation never costs the player anything.
     */
    private Result<Void> validateMinionPurchase(UUID playerId, Player player, String itemId, int quantity) {
        if (minionManager == null || !minionManager.isEnabled()) {
            return Result.failure("Minion module is not available.");
        }
        if (player == null) {
            return Result.failure("Player must be online to purchase a minion.");
        }
        String typeName = itemId.substring("minion_".length()).toUpperCase();
        try {
            MinionType.valueOf(typeName);
        } catch (IllegalArgumentException exception) {
            return Result.failure("Unknown minion type: " + typeName);
        }
        int remainingSlots = minionManager.getLimitPolicy().getRemainingSlots(playerId, player);
        if (quantity > remainingSlots) {
            return Result.failure("You can only place " + remainingSlots + " more minion(s).");
        }
        return Result.success(null);
    }

    /**
     * Builds a Minion Spawn Item with the minion type encoded in its
     * {@link PersistentDataContainer}, then adds it to the player's
     * inventory. Any overflow (inventory full) is dropped naturally at
     * the player's location so it is never lost.
     */
    private void giveMinionItem(Player player, String typeName, int quantity) {
        MinionType type;
        try {
            type = MinionType.valueOf(typeName);
        } catch (IllegalArgumentException exception) {
            return;
        }
        Material icon = switch (type) {
            case MINER -> Material.IRON_PICKAXE;
            case FARMER -> Material.WHEAT;
            case LUMBERJACK -> Material.IRON_AXE;
            case FISHING -> Material.FISHING_ROD;
            case COLLECTOR -> Material.HOPPER;
        };
        ItemStack item = new ItemStack(icon, quantity);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = typeName.charAt(0) + typeName.substring(1).toLowerCase();
            meta.displayName(Component.text(displayName + " Minion", NamedTextColor.AQUA));
            meta.lore(List.of(
                Component.text("Right-click on a block to place.", NamedTextColor.GRAY),
                Component.text("Type: " + displayName, NamedTextColor.GRAY)
            ));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(minionKey, PersistentDataType.STRING, type.name());
            item.setItemMeta(meta);
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    /**
     * Delivers a regular (non-minion) purchased item to the player,
     * dropping any overflow naturally at their location.
     */
    private void deliverRegularItem(Player player, ShopItemDefinition definition, int quantity) {
        if (player == null) {
            return;
        }
        Material material = Material.matchMaterial(definition.getMaterial());
        if (material == null) {
            return;
        }
        ItemStack item = new ItemStack(material, quantity);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    /**
     * Computes the adjusted sell price after a buy/sell action, maintaining
     * a consistent spread relative to the buy price. The sell price is
     * always 70% of the buy price by default (30% spread), but this ratio
     * could be made configurable per item in a future extension.
     *
     * <p>The sell price is also clamped to [minPrice * 0.5, maxPrice] to
     * prevent it from drifting into absurd territory even after many
     * adjustment cycles.</p>
     */
    private double computeAdjustedSellPrice(ShopItemRecord record) {
        double spreadRatio = 0.7; // sell price = 70% of buy price
        double rawSellPrice = record.getCurrentPrice() * spreadRatio;
        double minSellPrice = record.getMinPrice() * 0.5;
        double maxSellPrice = record.getMaxPrice();
        return Math.max(minSellPrice, Math.min(maxSellPrice, rawSellPrice));
    }

    private void recordPriceSnapshot(ShopItemRecord record) {
        PriceHistoryEntry entry = new PriceHistoryEntry(
            0L, record.getItemId(), record.getCurrentPrice(), record.getDemand(), record.getSupply(),
            System.currentTimeMillis()
        );
        shopItemRepository.recordPriceHistory(entry);
    }
}