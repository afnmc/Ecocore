package com.azthera.ecocore.economy;
 
import com.azthera.ecocore.util.NumberFormatter;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
 
import java.util.List;
 
/**
 * Bridges EcoCore's {@link EconomyService} (primary currency only) to the
 * Vault Economy API so third-party plugins can interact with EcoCore's
 * economy transparently. Balance reads are served from EcoCore's in-memory
 * cache for performance; offline players who have never joined while the
 * cache was warm will read as a 0 balance until they join, which is an
 * intentional trade-off to avoid blocking database calls on the main thread.
 * Bank support is not implemented, matching EcoCore's design (no bank module).
 */
public final class VaultEconomyProvider implements Economy {
 
    private final EconomyService economyService;
    private final CurrencyManager currencyManager;
    private final NumberFormatter numberFormatter;
 
    public VaultEconomyProvider(EconomyService economyService, CurrencyManager currencyManager) {
        this.economyService = economyService;
        this.currencyManager = currencyManager;
        this.numberFormatter = new NumberFormatter(2, false);
    }
 
    @Override
    public boolean isEnabled() {
        return true;
    }
 
    @Override
    public String getName() {
        return "EcoCore";
    }
 
    @Override
    public boolean hasBankSupport() {
        return false;
    }
 
    @Override
    public int fractionalDigits() {
        return 2;
    }
 
    @Override
    public String format(double amount) {
        Currency primary = currencyManager.getPrimaryCurrency();
        return numberFormatter.format(amount) + primary.symbol();
    }
 
    @Override
    public String currencyNamePlural() {
        return currencyManager.getPrimaryCurrency().displayName();
    }
 
    @Override
    public String currencyNameSingular() {
        return currencyManager.getPrimaryCurrency().displayName();
    }
 
    @SuppressWarnings("deprecation")
    private OfflinePlayer resolvePlayer(String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAccount(String playerName) {
        return hasAccount(resolvePlayer(playerName));
    }
 
    @Override
    public boolean hasAccount(OfflinePlayer player) {
        economyService.ensureAccountLoaded(player.getUniqueId());
        return true;
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }
 
    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public double getBalance(String playerName) {
        return getBalance(resolvePlayer(playerName));
    }
 
    @Override
    public double getBalance(OfflinePlayer player) {
        return economyService.getBalance(player.getUniqueId());
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }
 
    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public boolean has(String playerName, double amount) {
        return has(resolvePlayer(playerName), amount);
    }
 
    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return economyService.has(player.getUniqueId(), amount);
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }
 
    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(resolvePlayer(playerName), amount);
    }
 
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        var result = economyService.withdraw(player.getUniqueId(), amount, TransactionType.WITHDRAW, "Vault withdraw");
        if (result.isSuccess()) {
            double newBalance = result.orElse(economyService.getBalance(player.getUniqueId()));
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
        }
        double currentBalance = economyService.getBalance(player.getUniqueId());
        return new EconomyResponse(0, currentBalance, EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }
 
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(resolvePlayer(playerName), amount);
    }
 
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        var result = economyService.deposit(player.getUniqueId(), amount, TransactionType.DEPOSIT, "Vault deposit");
        if (result.isSuccess()) {
            double newBalance = result.orElse(economyService.getBalance(player.getUniqueId()));
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
        }
        double currentBalance = economyService.getBalance(player.getUniqueId());
        return new EconomyResponse(0, currentBalance, EconomyResponse.ResponseType.FAILURE, "Deposit failed");
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }
 
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }
 
    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank support is not implemented.");
    }
 
    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank support is not implemented.");
    }
 
    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank support is not implemented.");
    }
 
    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank support is not implemented.");
    }
 
    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank support is not implemented.");
    }
 
    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank support is not implemented.");
    }
 
    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank support is not implemented.");
    }
 
    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank support is not implemented.");
    }
 
    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank support is not implemented.");
    }
 
    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank support is not implemented.");
    }
 
    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Bank support is not implemented.");
    }
 
    @Override
    public List<String> getBanks() {
        return List.of();
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(resolvePlayer(playerName));
    }
 
    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        economyService.ensureAccountLoaded(player.getUniqueId());
        return true;
    }
 
    @Override
    @SuppressWarnings("deprecation")
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }
 
    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
}
