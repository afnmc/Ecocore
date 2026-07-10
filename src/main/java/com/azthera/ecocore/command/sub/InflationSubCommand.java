package com.azthera.ecocore.command.sub;

import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.economy.CurrencyManager;
import com.azthera.ecocore.inflation.InflationAdjustmentService;
import com.azthera.ecocore.inflation.InflationCalculator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public final class InflationSubCommand implements SubCommand {
    private final InflationAdjustmentService adjustmentService;
    private final CurrencyManager currencyManager;

    public InflationSubCommand(InflationAdjustmentService adjustmentService, CurrencyManager currencyManager) {
        this.adjustmentService = adjustmentService;
        this.currencyManager = currencyManager;
    }

    @Override public String getName() { return "inflation"; }
    @Override public String getPermission() { return "ecocore.command.eco.inflation"; }
    @Override public String getUsage() { return "/eco inflation [run]"; }
    @Override public String getDescription() { return "Lihat atau jalankan siklus inflasi."; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("run")) {
            long oneHourAgo = System.currentTimeMillis() - 3_600_000L;
            adjustmentService.runAdjustmentCycle(currencyManager.getPrimaryCurrencyId(), oneHourAgo);
            sender.sendMessage(Component.text("Siklus inflasi dijalankan manual.", NamedTextColor.GREEN));
            return;
        }

        double modifier = adjustmentService.getCurrentTaxRateModifier();
        double effectiveRate = adjustmentService.getLastEffectiveRate();
        InflationCalculator.InflationState state = adjustmentService.getLastState();

        sender.sendMessage(Component.text("=== Status Inflasi ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Tax Rate Modifier: " + String.format("%.4f", modifier), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Effective Inflation Rate: " + String.format("%.2f%%", effectiveRate * 100), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("State: " + state.name(), NamedTextColor.YELLOW));

        if (effectiveRate > 0) {
            sender.sendMessage(Component.text("▴ Inflasi: Naik " + String.format("%.2f%%", effectiveRate * 100), NamedTextColor.RED));
        } else if (effectiveRate < 0) {
            sender.sendMessage(Component.text("▾ Inflasi: Turun " + String.format("%.2f%%", Math.abs(effectiveRate) * 100), NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("● Inflasi: Stabil", NamedTextColor.GRAY));
        }
    }
}