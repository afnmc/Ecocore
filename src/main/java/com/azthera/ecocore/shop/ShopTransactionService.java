package com.azthera.ecocore.shop;

import com.azthera.ecocore.data.model.PriceHistoryEntry;
import com.azthera.ecocore.data.model.ShopItemRecord;
import com.azthera.ecocore.data.repository.ShopItemRepository;
import com.azthera.ecocore.economy.EconomyService;
import com.azthera.ecocore.economy.TransactionType;
import com.azthera.ecocore.quest.QuestManager;
import com.azthera.ecocore.util.Result;

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
 */
public final class ShopTransactionService {

    private final ShopManager shopManager;
    private final ShopItemRepository shopItemRepository;
    private final StockManager stockManager;
    private final PricingEngine pricingEngine;
    private final EconomyService economyService;
    private final QuestManager questManager;

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

        double totalCost = record.getCurrentPrice() * quantity;
        if (!economyService.has(playerId, totalCost)) {
            return Result.failure("Insufficient funds.");
        }

        Result<Double> withdrawResult = economyService.withdraw(
            playerId, totalCost, TransactionType.SHOP_BUY, "Bought " + quantity + "x " + itemId
        );
        if (withdrawResult.isFailure()) {
            return Result.failure("Payment failed.");
        }

        stockManager.decrementStock(record, quantity);
        double newPrice = pricingEngine.onBuy(record, quantity);
        record.setCurrentPrice(newPrice);
        shopItemRepository.save(record);
        recordPriceSnapshot(record);

        questManager.recordProgress(playerId, "buy:any", quantity);

        return Result.success(totalCost);
    }

    public Result<Double> sell(UUID playerId, String itemId, int quantity) {
        if (quantity <= 0) {
            return Result.failure("Quantity must be positive.");
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

        double totalPayout = record.getCurrentPrice() * quantity;

        Result<Double> depositResult = economyService.deposit(
            playerId, totalPayout, TransactionType.SHOP_SELL, "Sold " + quantity + "x " + itemId
        );
        if (depositResult.isFailure()) {
            return Result.failure("Payout failed.");
        }

        double newPrice = pricingEngine.onSell(record, quantity);
        record.setCurrentPrice(newPrice);
        shopItemRepository.save(record);
        recordPriceSnapshot(record);

        questManager.recordProgress(playerId, "sell:any", quantity);

        return Result.success(depositResult.orElse(totalPayout));
    }

    private void recordPriceSnapshot(ShopItemRecord record) {
        PriceHistoryEntry entry = new PriceHistoryEntry(
            0L, record.getItemId(), record.getCurrentPrice(), record.getDemand(), record.getSupply(),
            System.currentTimeMillis()
        );
        shopItemRepository.recordPriceHistory(entry);
    }
         }
