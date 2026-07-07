package com.azthera.ecocore.command;
 
import org.bukkit.command.CommandSender;
 
import java.util.List;
 
/**
 * A single {@code /eco <subcommand>} handler. Each implementation declares
 * its own permission node (checked by {@code EcoCommand} before dispatch)
 * and provides simple positional tab-completion.
 */
public interface SubCommand {
 
    String getName();
 
    String getPermission();
 
    String getUsage();
 
    String getDescription();
 
    /**
     * Executes the subcommand. Implementations should send feedback to
     * {@code sender} directly rather than throwing, since {@code EcoCommand}
     * does not catch arbitrary exceptions from subcommands (only permission
     * and unknown-subcommand cases are handled centrally).
     */
    void execute(CommandSender sender, String[] args);
 
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
