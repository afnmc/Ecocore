package com.azthera.ecocore.command.sub;

import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.config.GuiConfig;
import com.azthera.ecocore.data.repository.ShopItemRepository;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.gui.impl.ShopAdminEditorGui;
import com.azthera.ecocore.shop.ShopManager;
import com.azthera.ecocore.util.NumberFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /ecocore shopadmin edit <itemId>} — opens {@link ShopAdminEditorGui}
 * for the given shop item. The player-facing shop opener lives at
 * {@code /ecocore shop}.
 */
public final class ShopSubCommand implements SubCommand {

    private final ShopManager shopManager;
    private final ShopItemRepository shopItemRepository;
    private final GuiSessionManager sessionManager;
    private final GuiConfig guiConfig;
    private final NumberFormatter numberFormatter;

    public ShopSubCommand(ShopManager shopManager, ShopItemRepository shopItemRepository,
                           GuiSessionManager sessionManager, GuiConfig guiConfig, NumberFormatter numberFormatter) {
        this.shopManager = shopManager;
        this.shopItemRepository = shopItemRepository;
        this.sessionManager = sessionManager;
        this.guiConfig = guiConfig;
        this.numberFormatter = numberFormatter;
    }

    @Override
    public String getName() {
        return "shopadmin";
    }

    @Override
    public String getPermission() {
        return "ecocore.command.eco.shop";
    }

    @Override
    public String getUsage() {
        return "/ecocore shopadmin edit <itemId>";
    }

    @Override
    public String getDescription() {
        return "Edit item shop melalui GUI Admin Editor.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Perintah ini hanya bisa digunakan oleh pemain.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("edit")) {
            sender.sendMessage(Component.text("Usage: " + getUsage(), NamedTextColor.RED));
            return;
        }

        String itemId = args[1];
        if (shopManager.getDefinition(itemId).isEmpty()) {
            sender.sendMessage(Component.text("Item tidak ditemukan: " + itemId, NamedTextColor.RED));
            return;
        }

        new ShopAdminEditorGui(shopManager, shopItemRepository, sessionManager, numberFormatter, itemId).open(player);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("edit");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("edit")) {
            return shopManager.getAllDefinitions().stream()
                .map(com.azthera.ecocore.shop.ShopItemDefinition::getItemId)
                .toList();
        }
        return List.of();
    }
}
