package com.azthera.ecocore.command.sub;
 
import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.logging.DebugLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
 
/**
 * {@code /eco debug} — toggles verbose debug logging for troubleshooting
 * pricing, job progress, and quest generation without restarting the server.
 */
public final class DebugSubCommand implements SubCommand {
 
    private final DebugLogger debugLogger;
 
    public DebugSubCommand(DebugLogger debugLogger) {
        this.debugLogger = debugLogger;
    }
 
    @Override
    public String getName() {
        return "debug";
    }
 
    @Override
    public String getPermission() {
        return "ecocore.command.eco.debug";
    }
 
    @Override
    public String getUsage() {
        return "/eco debug";
    }
 
    @Override
    public String getDescription() {
        return "Toggle mode debug.";
    }
 
    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean newState = debugLogger.toggle();
        sender.sendMessage(Component.text("Debug mode: " + (newState ? "ON" : "OFF"),
            newState ? NamedTextColor.GREEN : NamedTextColor.RED));
    }
}
