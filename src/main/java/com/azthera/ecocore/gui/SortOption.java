package com.azthera.ecocore.gui;
 
import com.azthera.ecocore.data.model.ShopItemRecord;
 
import java.util.Comparator;
 
/**
 * Sort orders available in the Dynamic Shop GUI. Each wraps a
 * {@code Comparator<ShopItemRecord>} so {@code ShopGui} can apply sorting
 * without needing to know the comparison logic itself.
 */
public enum SortOption {
 
    PRICE_ASC(Comparator.comparingDouble(ShopItemRecord::getCurrentPrice)),
    PRICE_DESC(Comparator.comparingDouble(ShopItemRecord::getCurrentPrice).reversed()),
    NAME_ASC(Comparator.comparing(ShopItemRecord::getItemId)),
    NAME_DESC(Comparator.comparing(ShopItemRecord::getItemId).reversed()),
    STOCK_ASC(Comparator.comparingInt(ShopItemRecord::getStock)),
    STOCK_DESC(Comparator.comparingInt(ShopItemRecord::getStock).reversed());
 
    private final Comparator<ShopItemRecord> comparator;
 
    SortOption(Comparator<ShopItemRecord> comparator) {
        this.comparator = comparator;
    }
 
    public Comparator<ShopItemRecord> getComparator() {
        return comparator;
    }
 
    public SortOption next() {
        SortOption[] values = values();
        int nextIndex = (this.ordinal() + 1) % values.length;
        return values[nextIndex];
    }
}
