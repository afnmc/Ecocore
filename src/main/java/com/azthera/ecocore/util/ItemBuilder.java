package com.azthera.ecocore.util;
 
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
 
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
 
/**
 * Fluent builder for ItemStacks used across shop, GUI, and reward systems.
 * Centralizes Adventure Component naming/lore and persistent-data tagging
 * so every module builds items the same way.
 */
public final class ItemBuilder {
 
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;
    private final List<Component> loreLines = new ArrayList<>();
 
    private ItemBuilder(ItemStack base) {
        this.itemStack = base;
        this.itemMeta = base.getItemMeta();
    }
 
    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }
 
    public static ItemBuilder of(ItemStack existing) {
        return new ItemBuilder(existing.clone());
    }
 
    public ItemBuilder amount(int amount) {
        itemStack.setAmount(Math.max(1, amount));
        return this;
    }
 
    public ItemBuilder name(Component name) {
        itemMeta.displayName(name);
        return this;
    }
 
    public ItemBuilder lore(Component line) {
        loreLines.add(line);
        return this;
    }
 
    public ItemBuilder lore(List<Component> lines) {
        loreLines.addAll(lines);
        return this;
    }
 
    public ItemBuilder clearLore() {
        loreLines.clear();
        return this;
    }
 
    public ItemBuilder flags(ItemFlag... flags) {
        itemMeta.addItemFlags(flags);
        return this;
    }
 
    public ItemBuilder unbreakable(boolean unbreakable) {
        itemMeta.setUnbreakable(unbreakable);
        return this;
    }
 
    public ItemBuilder customModelData(int data) {
        itemMeta.setCustomModelData(data);
        return this;
    }
 
    public <T, Z> ItemBuilder persistentData(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        itemMeta.getPersistentDataContainer().set(key, type, value);
        return this;
    }
 
    public ItemBuilder editMeta(Consumer<ItemMeta> consumer) {
        consumer.accept(itemMeta);
        return this;
    }
 
    public ItemStack build() {
        if (!loreLines.isEmpty()) {
            itemMeta.lore(loreLines);
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
