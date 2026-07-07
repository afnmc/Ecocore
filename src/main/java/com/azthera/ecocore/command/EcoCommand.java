package com.azthera.ecocore.command;
 
import com.azthera.ecocore.util.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
 
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
 
/**
 * Dispatcher for {@code /eco <subcommand> [args...]}. Owns the registry of
 * every {@link SubCommand}, checks each one's declared permission before
 * dispatch, and shows a generated help listing when no/unknown subcommand
 * is given. New subcommands are added purely by registering them here — no
 * other class needs to change.
 */
public final class EcoCommand implements CommandExecutor, TabCompleter {
 
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();
    private final MessageService messageService;
 
    public EcoCommand(MessageService messageService) {
        this.messageService = messageService;
    }
 
    public void register(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }
 
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
 
        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            sender.sendMessage(messageService.render("general.unknown-subcommand"));
            sendHelp(sender);
            return true;
        }
 
        if (!subCommand.getPermission().isEmpty() && !sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(messageService.render("general.no-permission"));
            return true;
        }
 
        String[] remainingArgs = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[0];
        subCommand.execute(sender, remainingArgs);
        return true;
    }
 
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            for (SubCommand subCommand : subCommands.values()) {
                if (subCommand.getPermission().isEmpty() || sender.hasPermission(subCommand.getPermission())) {
                    if (subCommand.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        matches.add(subCommand.getName());
                    }
                }
            }
            return matches;
        }
 
        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                String[] remainingArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
                return subCommand.tabComplete(sender, remainingArgs);
            }
        }
 
        return List.of();
    }
 
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== EcoCore Commands ==="));
        for (SubCommand subCommand : subCommands.values()) {
            if (!subCommand.getPermission().isEmpty() && !sender.hasPermission(subCommand.getPermission())) {
                continue;
            }
            sender.sendMessage(Component.text(subCommand.getUsage() + " - " + subCommand.getDescription()));
        }
    }
}
