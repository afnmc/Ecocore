package com.azthera.ecocore.command.sub;
 
import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.config.ConfigManager;
import com.azthera.ecocore.util.MessageService;
import org.bukkit.command.CommandSender;
 
/**
 * {@code /eco reload} — reloads every YAML config file and every registered
 * module, with no server restart required.
 */
public final class ReloadSubCommand implements SubCommand {
 
    private final ConfigManager configManager;
    private final Runnable moduleReloadCallback;
    private final MessageService messageService;
 
    public ReloadSubCommand(ConfigManager configManager, Runnable moduleReloadCallback, MessageService messageService) {
        this.configManager = configManager;
        this.moduleReloadCallback = moduleReloadCallback;
        this.messageService = messageService;
    }
 
    @Override
    public String getName() {
        return "reload";
    }
 
    @Override
    public String getPermission() {
        return "ecocore.command.eco.reload";
    }
 
    @Override
    public String getUsage() {
        return "/eco reload";
    }
 
    @Override
    public String getDescription() {
        return "Reload seluruh konfigurasi EcoCore.";
    }
 
    @Override
    public void execute(CommandSender sender, String[] args) {
        configManager.reloadAll();
        moduleReloadCallback.run();
        sender.sendMessage(messageService.render("general.reload-success"));
    }
}
