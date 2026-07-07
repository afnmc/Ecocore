package com.azthera.ecocore.command.sub;
 
import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.market.MarketEventManager;
import com.azthera.ecocore.market.MarketEventType;
import com.azthera.ecocore.market.MarketSimulationEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
 
import java.util.Arrays;
import java.util.List;
 
/**
 * {@code /eco market <status|tick|start> [eventType]} — admin tools for
 * inspecting and manually influencing the AI Market simulation.
 */
public final class MarketSubCommand implements SubCommand {
 
    private final MarketSimulationEngine simulationEngine;
    private final MarketEventManager eventManager;
 
    public MarketSubCommand(MarketSimulationEngine simulationEngine, MarketEventManager eventManager) {
        this.simulationEngine = simulationEngine;
        this.eventManager = eventManager;
    }
 
    @Override
    public String getName() {
        return "market";
    }
 
    @Override
    public String getPermission() {
        return "ecocore.command.eco.market";
    }
 
    @Override
    public String getUsage() {
        return "/eco market <status|tick|start> [eventType]";
    }
 
    @Override
    public String getDescription() {
        return "Kelola AI Market.";
    }
 
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: " + getUsage(), NamedTextColor.RED));
            return;
        }
 
        switch (args[0].toLowerCase()) {
            case "tick" -> {
                simulationEngine.runSimulationTick();
                sender.sendMessage(Component.text("Market simulation tick dijalankan manual.", NamedTextColor.GREEN));
            }
            case "status" -> sender.sendMessage(Component.text("Gunakan /eco market start <event> untuk memaksa event.", NamedTextColor.GRAY));
            case "start" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /eco market start <eventType>", NamedTextColor.RED));
                    return;
                }
                try {
                    MarketEventType.valueOf(args[1].toUpperCase());
                    eventManager.maybeStartRandomEvent();
                    sender.sendMessage(Component.text("Mencoba memulai event market.", NamedTextColor.GREEN));
                } catch (IllegalArgumentException exception) {
                    sender.sendMessage(Component.text("Event type tidak dikenal.", NamedTextColor.RED));
                }
            }
            default -> sender.sendMessage(Component.text("Usage: " + getUsage(), NamedTextColor.RED));
        }
    }
 
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("status", "tick", "start");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return Arrays.stream(MarketEventType.values()).map(Enum::name).toList();
        }
        return List.of();
    }
}
