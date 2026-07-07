package com.azthera.ecocore.logging;
 
import com.azthera.ecocore.data.model.TransactionLogEntry;
import com.azthera.ecocore.data.repository.TransactionLogRepository;
import com.azthera.ecocore.economy.TransactionType;
 
import java.util.UUID;
 
/**
 * Thin, purpose-specific facade over {@link TransactionLogRepository} used
 * exclusively by {@code EconomyServiceImpl} to record every balance-affecting
 * event. Kept as its own class (rather than calling the repository directly
 * from the service) so transaction-log formatting/enrichment can evolve
 * independently of the generic repository contract, and so admin log viewers
 * have one obvious entry point to depend on.
 */
public final class TransactionLogger {
 
    private final TransactionLogRepository transactionLogRepository;
 
    public TransactionLogger(TransactionLogRepository transactionLogRepository) {
        this.transactionLogRepository = transactionLogRepository;
    }
 
    public void record(UUID playerId, String currencyId, TransactionType type, double amount,
                        double balanceAfter, String description) {
        TransactionLogEntry entry = new TransactionLogEntry(
            0L, playerId, currencyId, type.name(), amount, balanceAfter, description, System.currentTimeMillis()
        );
        transactionLogRepository.log(entry);
    }
}
