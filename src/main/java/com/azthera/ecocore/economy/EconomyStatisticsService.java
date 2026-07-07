package com.azthera.ecocore.economy;
 
import com.azthera.ecocore.data.model.TransactionLogEntry;
import com.azthera.ecocore.data.repository.PlayerAccountRepository;
import com.azthera.ecocore.data.repository.TransactionLogRepository;
 
import java.util.List;
import java.util.concurrent.CompletableFuture;
 
/**
 * Read-only aggregation service exposing economy-wide statistics for the
 * {@code /eco economy} admin command and for {@code InflationCalculator} /
 * {@code MoneySupplyTracker} to consume without depending on repositories directly.
 */
public final class EconomyStatisticsService {
 
    private final PlayerAccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final MoneySinkManager moneySinkManager;
 
    public EconomyStatisticsService(PlayerAccountRepository accountRepository,
                                     TransactionLogRepository transactionLogRepository,
                                     MoneySinkManager moneySinkManager) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.moneySinkManager = moneySinkManager;
    }
 
    public CompletableFuture<Double> getTotalMoneySupply(String currencyId) {
        return accountRepository.sumBalancesAsync(currencyId);
    }
 
    public CompletableFuture<Long> getAccountCount(String currencyId) {
        return accountRepository.countAccountsAsync(currencyId);
    }
 
    public CompletableFuture<List<TransactionLogEntry>> getRecentTransactions(int limit) {
        return transactionLogRepository.findRecentAsync(limit);
    }
 
    public CompletableFuture<Double> getNetMoneyCreated(String currencyId, long fromMillis, long toMillis) {
        return transactionLogRepository.sumNetAmountAsync(currencyId, fromMillis, toMillis);
    }
 
    public double getCumulativeSink(String currencyId) {
        return moneySinkManager.getCumulativeSink(currencyId);
    }
 
    public CompletableFuture<List<com.azthera.ecocore.data.model.PlayerAccount>> getTopBalances(String currencyId, int limit) {
        return accountRepository.findTopBalancesAsync(currencyId, limit, 0);
    }
}
