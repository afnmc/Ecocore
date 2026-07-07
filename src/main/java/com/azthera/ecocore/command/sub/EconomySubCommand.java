package com.azthera.ecocore.command.sub;
 
import com.azthera.ecocore.command.SubCommand;
import com.azthera.ecocore.economy.CurrencyManager;
import com.azthera.ecocore.economy.EconomyStatisticsService;
import com.azthera.ecocore.util.NumberFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
 
/**
 * {@code /eco economy} — prints a snapshot of economy-wide statistics
 * (total money supply, account count, cumulative money sink) for the
 * primary currency.
 */
public final class EconomySubCommand implements SubCommand {
 
    private final EconomyStatisticsService statisticsService;
    private final CurrencyManager currencyManager;
    private final NumberFormatter numberFormatter;
 
    public EconomySubCommand(EconomyStatisticsService statisticsService, CurrencyManager currencyManager,
                              NumberFormatter numberFormatter) {
        this.statisticsService = statisticsService;
        this.currencyManager = currencyManager;
        this.numberFormatter = numberFormatter;
    }
 
    @Override
    public String getName() {
        return "economy";
    }
 
    @Override
    public String getPermission() {
        return "ecocore.command.eco.economy";
    }
 
    @Override
    public String getUsage() {
        return "/eco economy";
    }
 
    @Override
    public String getDescription() {
        return "Lihat statistik ekonomi server.";
    }
 
    @Override
    public void execute(CommandSender sender, String[] args) {
        String currencyId = currencyManager.getPrimaryCurrencyId();
 
        statisticsService.getTotalMoneySupply(currencyId).thenAccept(totalSupply ->
            statisticsService.getAccountCount(currencyId).thenAccept(accountCount -> {
                double cumulativeSink = statisticsService.getCumulativeSink(currencyId);
 
                sender.sendMessage(Component.text("=== Statistik Ekonomi ===", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Total Money Supply: " + numberFormatter.format(totalSupply), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Jumlah Akun: " + accountCount, NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Cumulative Money Sink: " + numberFormatter.format(cumulativeSink), NamedTextColor.YELLOW));
            }));
    }
}
