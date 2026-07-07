package com.azthera.ecocore.gui;
 
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
 
import java.util.function.BiConsumer;
 
/**
 * A single clickable slot definition within a GUI: the icon shown and the
 * action performed when a player clicks it. The click action receives both
 * the clicking player and the {@link ClickType} so buttons can behave
 * differently on left/right/shift-click (e.g. buy 1 vs buy 64).
 */
public final class GuiButton {
 
    private final ItemStack icon;
    private final BiConsumer<Player, ClickType> onClick;
 
    public GuiButton(ItemStack icon, BiConsumer<Player, ClickType> onClick) {
        this.icon = icon;
        this.onClick = onClick;
    }
 
    public static GuiButton of(ItemStack icon, BiConsumer<Player, ClickType> onClick) {
        return new GuiButton(icon, onClick);
    }
 
    public static GuiButton display(ItemStack icon) {
        return new GuiButton(icon, (player, clickType) -> { });
    }
 
    public ItemStack getIcon() {
        return icon;
    }
 
    public void handleClick(Player player, ClickType clickType) {
        onClick.accept(player, clickType);
    }
}
