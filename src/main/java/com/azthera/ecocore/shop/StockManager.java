package com.azthera.ecocore.shop;
 
import com.azthera.ecocore.data.model.ShopItemRecord;
import com.azthera.ecocore.data.repository.ShopItemRepository;
 
/**
 * Handles stock decrement/increment for buy/sell transactions and answers
 * "is this item currently purchasable" for the shop GUI and transaction service.
 * Selling to the shop does not increase stock by default (that would let
 * players farm infinite stock by selling then buying back) — restocking is
 * exclusively handled by {@code RestockScheduler}.
 */
public final class StockManager {
 
    private final ShopItemRepository shopItemRepository;
 
    public StockManager(ShopItemRepository shopItemRepository) {
        this.shopItemRepository = shopItemRepository;
    }
 
    public boolean hasStock(ShopItemRecord record, int quantity) {
        return record.getStock() >= quantity;
    }
 
    public boolean isOutOfStock(ShopItemRecord record) {
        return record.getStock() <= 0;
    }
 
    /**
     * Decrements stock after a successful buy. Caller must have already
     * verified {@link #hasStock(ShopItemRecord, int)}.
     */
    public void decrementStock(ShopItemRecord record, int quantity) {
        int newStock = Math.max(0, record.getStock() - quantity);
        record.setStock(newStock);
        shopItemRepository.save(record);
    }
 
    public void restockToFull(ShopItemRecord record, int fullStock, long nowMillis) {
        record.setStock(fullStock);
        record.setLastRestockMillis(nowMillis);
        shopItemRepository.save(record);
    }
 
    public boolean isDueForRestock(ShopItemRecord record, long nowMillis) {
        long elapsedSeconds = (nowMillis - record.getLastRestockMillis()) / 1000L;
        return elapsedSeconds >= record.getRestockIntervalSeconds();
    }
}
