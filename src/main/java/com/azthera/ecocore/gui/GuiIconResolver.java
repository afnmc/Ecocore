package com.azthera.ecocore.gui;
 
import com.azthera.ecocore.util.ItemBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
 
/**
 * Resolves an icon {@code ItemStack} from a GUI config file's {@code icons}
 * section by key, falling back to a configurable default material if the
 * key is missing or invalid. This is what makes "Semua icon configurable"
 * (all GUI icons config-driven) work uniformly across every GUI class.
 */
public final class GuiIconResolver {
 
    private static final Material FALLBACK_MATERIAL = Material.BARRIER;
 
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
 
    public ItemStack resolveIcon(YamlConfiguration guiConfig, String iconKey) {
        return resolveIcon(guiConfig, iconKey, null);
    }
 
    public ItemStack resolveIcon(YamlConfiguration guiConfig, String iconKey, String displayNameOverride) {
        String materialName = guiConfig.getString("icons." + iconKey);
        Material material = materialName != null ? Material.matchMaterial(materialName) : null;
        if (material == null) {
            material = FALLBACK_MATERIAL;
        }
 
        ItemBuilder builder = ItemBuilder.of(material);
        if (displayNameOverride != null) {
            builder.name(miniMessage.deserialize(displayNameOverride));
        }
        return builder.build();
    }
 
    public Material resolveMaterialOnly(YamlConfiguration guiConfig, String iconKey, Material fallback) {
        String materialName = guiConfig.getString("icons." + iconKey);
        Material material = materialName != null ? Material.matchMaterial(materialName) : null;
        return material != null ? material : fallback;
    }
 
    public int resolveSlot(YamlConfiguration guiConfig, String slotKey, int fallback) {
        return guiConfig.getInt("slots." + slotKey, fallback);
    }
 
    public String resolveTitle(YamlConfiguration guiConfig, String fallback) {
        return guiConfig.getString("title", fallback);
    }
 
    public int resolveRows(YamlConfiguration guiConfig, int fallback) {
        return guiConfig.getInt("rows", fallback);
    }
}
