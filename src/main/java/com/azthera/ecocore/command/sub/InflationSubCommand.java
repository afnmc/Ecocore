package com.azthera.ecocore.command.sub;
 
import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.economy.CurrencyManager;
import com.azthera.ecocore.inflation.InflationAdjustmentService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
 
/**
 * {@code /eco inflation [run]} — shows the current inflation-derived tax
 * modifier, or forces an adjustment cycle to run immediately.
 */
public final class InflationSubCommand implements SubCommand {
 
    private final InflationAdjustmentService adjustmentService;
    private final CurrencyManager currencyManager;
 
    public InflationSubCommand(InflationAdjustmentService adjustmentService, CurrencyManager currencyManager) {
        this.adjustmentService = adjustmentService;
        this.currencyManager = currencyManager;
    }
 
    @Override
    public String getName() {
        return "inflation";
    }
 
    @Override
    public String getPermission() {
        return "ecocore.command.eco.inflation";
    }
 
    @Override
    public String getUsage() {
        return "/eco inflation [run]";
    }
 
    @Override
    public String getDescription() {
        return "Lihat atau jalankan siklus inflasi.";
    }
 
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("run")) {
            long oneHourAgo = System.currentTimeMillis() - 3_600_000L;
            adjustmentService.runAdjustmentCycle(currencyManager.getPrimaryCurrencyId(), oneHourAgo);
            sender.sendMessage(Component.text("Siklus inflasi dijalankan manual.", NamedTextColor.GREEN));
            return;
        }
 
        double modifier = adjustmentService.getCurrentTaxRateModifier();
        sender.sendMessage(Component.text("Tax rate modifier saat ini: " + String.format("%.4f", modifier), NamedTextColor.YELLOW));
    }
}
