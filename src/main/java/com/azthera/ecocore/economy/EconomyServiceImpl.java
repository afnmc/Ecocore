package com.azthera.ecocore.economy;
 
import com.azthera.ecocore.data.model.PlayerAccount;
import com.azthera.ecocore.data.model.TransactionLogEntry;
import com.azthera.ecocore.data.repository.PlayerAccountRepository;
import com.azthera.ecocore.data.repository.TransactionLogRepository;
import com.azthera.ecocore.util.Result;
 
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
 
/**
 * Default {@link EconomyService} implementation. Every write updates the
 * cache synchronously (so subsequent reads within the same tick are
 * consistent) and schedules an async persist via the repository layer.
 */
public final class EconomyServiceImpl implements EconomyService {
 
    private final PlayerAccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final CurrencyManager currencyManager;
    private final TaxManager taxManager;
    private final MoneySinkManager moneySinkManager;
    private final Logger logger;
 
    public EconomyServiceImpl(PlayerAccountRepository accountRepository, TransactionLogRepository transactionLogRepository,
                               CurrencyManager currencyManager, TaxManager taxManager, MoneySinkManager moneySinkManager,
                               Logger logger) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.currencyManager = currencyManager;
        this.taxManager = taxManager;
        this.moneySinkManager = moneySinkManager;
        this.logger = logger;
    }
 
    @Override
    public String getPrimaryCurrencyId() {
        return currencyManager.getPrimaryCurrencyId();
    }
 
    @Override
    public void ensureAccountLoaded(UUID playerId) {
        accountRepository.loadForPlayerAsync(playerId).thenAccept(existingAccounts -> {
            for (Currency currency : currencyManager.getAllCurrencies()) {
                boolean alreadyExists = existingAccounts.stream()
                    .anyMatch(account -> account.getCurrencyId().equals(currency.id()));
                boolean alreadyCached = accountRepository.getCached(playerId, currency.id()).isPresent();
 
                if (!alreadyExists && !alreadyCached) {
                    PlayerAccount newAccount = new PlayerAccount(
                        playerId, currency.id(), currency.startingBalance(), System.currentTimeMillis()
                    );
                    accountRepository.save(newAccount);
                    logTransaction(playerId, currency.id(), TransactionType.STARTING_BALANCE,
                        currency.startingBalance(), currency.startingBalance(), "Initial account creation");
                }
            }
        });
    }
 
    @Override
    public double getBalance(UUID playerId) {
        return getBalance(playerId, currencyManager.getPrimaryCurrencyId());
    }
 
    @Override
    public double getBalance(UUID playerId, String currencyId) {
        return accountRepository.getCached(playerId, currencyId)
            .map(PlayerAccount::getBalance)
            .orElse(0.0);
    }
 
    @Override
    public boolean has(UUID playerId, double amount) {
        return has(playerId, currencyManager.getPrimaryCurrencyId(), amount);
    }
 
    @Override
    public boolean has(UUID playerId, String currencyId, double amount) {
        return getBalance(playerId, currencyId) >= amount;
    }
 
    @Override
    public Result<Double> deposit(UUID playerId, double amount, TransactionType type, String description) {
        return deposit(playerId, currencyManager.getPrimaryCurrencyId(), amount, type, description);
    }
 
    @Override
    public Result<Double> deposit(UUID playerId, String currencyId, double amount, TransactionType type, String description) {
        if (amount <= 0) {
            return Result.failure("Deposit amount must be positive.");
        }
        if (!currencyManager.isKnownCurrency(currencyId)) {
            return Result.failure("Unknown currency: " + currencyId);
        }
 
        double netAmount = applyTaxIfApplicable(playerId, currencyId, type, amount);
 
        PlayerAccount account = getOrCreateCached(playerId, currencyId);
        double newBalance = account.getBalance() + netAmount;
        account.setBalance(newBalance);
        account.setLastUpdatedMillis(System.currentTimeMillis());
        accountRepository.save(account);
 
        logTransaction(playerId, currencyId, type, netAmount, newBalance, description);
        return Result.success(newBalance);
    }
 
    @Override
    public Result<Double> withdraw(UUID playerId, double amount, TransactionType type, String description) {
        return withdraw(playerId, currencyManager.getPrimaryCurrencyId(), amount, type, description);
    }
 
    @Override
    public Result<Double> withdraw(UUID playerId, String currencyId, double amount, TransactionType type, String description) {
        if (amount <= 0) {
            return Result.failure("Withdraw amount must be positive.");
        }
        if (!currencyManager.isKnownCurrency(currencyId)) {
            return Result.failure("Unknown currency: " + currencyId);
        }
 
        Optional<PlayerAccount> cachedAccount = accountRepository.getCached(playerId, currencyId);
        double currentBalance = cachedAccount.map(PlayerAccount::getBalance).orElse(0.0);
 
        if (currentBalance < amount) {
            InsufficientFundsException cause = new InsufficientFundsException(playerId, currencyId, amount, currentBalance);
            return Result.failure(cause.getMessage(), cause);
        }
 
        PlayerAccount account = cachedAccount.orElseGet(() ->
            new PlayerAccount(playerId, currencyId, 0.0, System.currentTimeMillis()));
        double newBalance = currentBalance - amount;
        account.setBalance(newBalance);
        account.setLastUpdatedMillis(System.currentTimeMillis());
        accountRepository.save(account);
 
        logTransaction(playerId, currencyId, type, -amount, newBalance, description);
        return Result.success(newBalance);
    }
 
    @Override
    public Result<Void> transfer(UUID fromPlayerId, UUID toPlayerId, double amount, String description) {
        if (amount <= 0) {
            return Result.failure("Transfer amount must be positive.");
        }
 
        String primaryCurrencyId = currencyManager.getPrimaryCurrencyId();
        Result<Double> withdrawResult = withdraw(fromPlayerId, primaryCurrencyId, amount, TransactionType.TRANSFER_OUT, description);
        if (withdrawResult.isFailure()) {
            return Result.failure("Transfer failed: sender has insufficient funds.");
        }
 
        Result<Double> depositResult = deposit(toPlayerId, primaryCurrencyId, amount, TransactionType.TRANSFER_IN, description);
        if (depositResult.isFailure()) {
            logger.warning(() -> "Transfer deposit leg failed for " + toPlayerId + ", refunding sender " + fromPlayerId);
            deposit(fromPlayerId, primaryCurrencyId, amount, TransactionType.TRANSFER_IN, "Refund: failed transfer");
            return Result.failure("Transfer failed: could not credit recipient. Sender was refunded.");
        }
 
        return Result.success(null);
    }
 
    private double applyTaxIfApplicable(UUID playerId, String currencyId, TransactionType type, double grossAmount) {
        if (!taxManager.isTaxable(type)) {
            return grossAmount;
        }
 
        double tax = taxManager.computeTax(grossAmount);
        if (tax <= 0) {
            return grossAmount;
        }
 
        moneySinkManager.recordSink(currencyId, tax, "tax:" + type);
        double referenceBalance = getBalance(playerId, currencyId);
        logTransaction(playerId, currencyId, TransactionType.TAX, -tax, referenceBalance, "Tax on " + type);
 
        return grossAmount - tax;
    }
 
    private PlayerAccount getOrCreateCached(UUID playerId, String currencyId) {
        return accountRepository.getCached(playerId, currencyId)
            .orElseGet(() -> new PlayerAccount(playerId, currencyId, 0.0, System.currentTimeMillis()));
    }
 
    private void logTransaction(UUID playerId, String currencyId, TransactionType type, double amount,
                                 double balanceAfter, String description) {
        TransactionLogEntry entry = new TransactionLogEntry(
            0L, playerId, currencyId, type.name(), amount, balanceAfter, description, System.currentTimeMillis()
        );
        transactionLogRepository.log(entry);
    }
}
