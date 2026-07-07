package com.azthera.ecocore.shop;
 
/**
 * A named grouping of shop items used to organize the Dynamic Shop GUI into
 * tabs/categories. Purely presentational — pricing logic does not depend on
 * category membership.
 */
public final class ShopCategory {
 
    private final String id;
    private final String displayName;
    private final String iconMaterial;
    private final int sortOrder;
 
    public ShopCategory(String id, String displayName, String iconMaterial, int sortOrder) {
        this.id = id;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.sortOrder = sortOrder;
    }
 
    public String getId() {
        return id;
    }
 
    public String getDisplayName() {
        return displayName;
    }
 
    public String getIconMaterial() {
        return iconMaterial;
    }
 
    public int getSortOrder() {
        return sortOrder;
    }
}
