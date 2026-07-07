package com.azthera.ecocore.command.sub;
 
import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.economy.EconomyService;
import com.azthera.ecocore.economy.TransactionType;
import com.azthera.ecocore.util.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
 
import java.util.List;
 
/**
 * {@code /eco give <player> <amount>} — grants currency to a player,
 * logged as an admin transaction for audit purposes.
 */
public final class GiveSubCommand implements SubCommand {
 
    private final EconomyService economyService;
    private final MessageService messageService;
 
    public GiveSubCommand(EconomyService economyService, MessageService messageService) {
        this.economyService = economyService;
        this.messageService = messageService;
    }
 
    @Override
    public String getName() {
        return "give";
    }
 
    @Override
    public String getPermission() {
        return "ecocore.command.eco.give";
    }
 
    @Override
    public String getUsage() {
        return "/eco give <player> <amount>";
    }
 
    @Override
    public String getDescription() {
        return "Berikan uang kepada pemain.";
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: " + getUsage(), NamedTextColor.RED));
            return;
        }
 
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Component.text("Jumlah tidak valid.", NamedTextColor.RED));
            return;
        }
 
        if (amount <= 0) {
            sender.sendMessage(Component.text("Jumlah harus lebih dari 0.", NamedTextColor.RED));
            return;
        }
 
        economyService.ensureAccountLoaded(target.getUniqueId());
        var result = economyService.deposit(target.getUniqueId(), amount, TransactionType.ADMIN_GIVE,
            "Admin give by " + sender.getName());
 
        if (result.isSuccess()) {
            sender.sendMessage(messageService.render("economy.give-success",
                MessageService.placeholder("amount", String.valueOf(amount)),
                MessageService.placeholder("player", args[0])));
        } else {
            result.onFailure(reason -> sender.sendMessage(Component.text(reason, NamedTextColor.RED)));
        }
    }
 
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).toList();
        }
        return List.of();
    }
}
