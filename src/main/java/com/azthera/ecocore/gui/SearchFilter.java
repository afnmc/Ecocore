package com.azthera.ecocore.gui;
 
import com.azthera.ecocore.shop.ShopItemDefinition;
 
import java.util.Collection;
import java.util.List;
import java.util.Locale;
 
/**
 * Case-insensitive substring filter applied to shop item definitions by
 * item id or display name, used by {@code ShopGui}'s search button.
 */
public final class SearchFilter {
 
    private String query = "";
 
    public void setQuery(String query) {
        this.query = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }
 
    public String getQuery() {
        return query;
    }
 
    public boolean isActive() {
        return !query.isBlank();
    }
 
    public void clear() {
        this.query = "";
    }
 
    public List<ShopItemDefinition> apply(Collection<ShopItemDefinition> definitions) {
        if (!isActive()) {
            return List.copyOf(definitions);
        }
        return definitions.stream()
            .filter(this::matches)
            .toList();
    }
 
    private boolean matches(ShopItemDefinition definition) {
        return definition.getItemId().toLowerCase(Locale.ROOT).contains(query)
            || definition.getDisplayName().toLowerCase(Locale.ROOT).contains(query);
    }
}
