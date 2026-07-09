package com.azthera.ecocore.data.repository;

import com.azthera.ecocore.data.cache.Cache;
import com.azthera.ecocore.data.cache.InMemoryCache;
import com.azthera.ecocore.data.dao.PlayerAccountDAO;
import com.azthera.ecocore.data.model.PlayerAccount;
import com.azthera.ecocore.util.AsyncUtil;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Repository for player currency balances. Wraps {@link PlayerAccountDAO} with
 * an in-memory cache keyed by "{playerId}:{currencyId}" so that balance checks
 * during shop transactions and Vault economy calls do not block on the database.
 * Writes go to the cache immediately (synchronously) and are persisted to the
 * database asynchronously.
 *
 * <p><b>Since V3:</b> Fixed race condition in {@link #loadForPlayerAsync(UUID)}
 * where an async load from database could overwrite a newer cache entry that
 * was updated by {@code /eco give} or a shop transaction. Now only overwrites
 * cache if the loaded data is newer than what's already cached.</p>
 */
public final class PlayerAccountRepository {
    private final PlayerAccountDAO dao;
    private final AsyncUtil asyncUtil;
    private final Logger logger;
    private final Cache<String, PlayerAccount> cache = new InMemoryCache<>();

    public PlayerAccountRepository(PlayerAccountDAO dao, AsyncUtil asyncUtil, Logger logger) {
        this.dao = dao;
        this.asyncUtil = asyncUtil;
        this.logger = logger;
    }

    private String key(UUID playerId, String currencyId) {
        return playerId + ":" + currencyId;
    }

    /**
     * Synchronous, cache-only read. Returns empty if the account has not been
     * loaded into cache yet — callers on a hot path (Vault bridge) should have
     * already triggered a load via {@link #loadForPlayerAsync(UUID)} on join.
     */
    public Optional<PlayerAccount> getCached(UUID playerId, String currencyId) {
        return cache.get(key(playerId, currencyId));
    }

    public CompletableFuture<Optional<PlayerAccount>> loadAsync(UUID playerId, String currencyId) {
        return asyncUtil.supplyAsync(() -> {
            try {
                Optional<PlayerAccount> account = dao.find(playerId, currencyId);
                account.ifPresent(a -> cache.put(key(playerId, currencyId), a));
                return account;
            } catch (SQLException exception) {
                logger.severe("Failed to load account " + playerId + "/" + currencyId + ": " + exception.getMessage());
                return Optional.<PlayerAccount>empty();
            }
        });
    }

    public CompletableFuture<List<PlayerAccount>> loadForPlayerAsync(UUID playerId) {
        return asyncUtil.supplyAsync(() -> {
            try {
                List<PlayerAccount> accounts = dao.findAllForPlayer(playerId);
                // FIX: Don't overwrite cache if cached account is newer than loaded one
                accounts.forEach(loadedAccount -> {
                    String key = key(playerId, loadedAccount.getCurrencyId());
                    Optional<PlayerAccount> cached = cache.get(key);
                    if (cached.isEmpty() || cached.get().getLastUpdatedMillis() < loadedAccount.getLastUpdatedMillis()) {
                        cache.put(key, loadedAccount);
                    }
                });
                return accounts;
            } catch (SQLException exception) {
                logger.severe("Failed to load accounts for player " + playerId + ": " + exception.getMessage());
                return List.<PlayerAccount>of();
            }
        });
    }

    /**
     * Updates the cache immediately and schedules an async persist.
     * This is the primary write path used by EconomyService.
     */
    public void save(PlayerAccount account) {
        cache.put(key(account.getPlayerId(), account.getCurrencyId()), account);
        asyncUtil.runAsync(() -> {
            try {
                dao.upsert(account);
            } catch (SQLException exception) {
                logger.severe("Failed to persist account " + account.getPlayerId() + ": " + exception.getMessage());
            }
        });
    }

    public CompletableFuture<Void> delete(UUID playerId, String currencyId) {
        cache.invalidate(key(playerId, currencyId));
        return asyncUtil.runAsync(() -> {
            try {
                dao.delete(playerId, currencyId);
            } catch (SQLException exception) {
                logger.severe("Failed to delete account " + playerId + "/" + currencyId + ": " + exception.getMessage());
            }
        });
    }

    public void invalidatePlayer(UUID playerId, List<String> currencyIds) {
        for (String currencyId : currencyIds) {
            cache.invalidate(key(playerId, currencyId));
        }
    }

    public CompletableFuture<List<PlayerAccount>> findTopBalancesAsync(String currencyId, int limit, int offset) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.findAllForCurrency(currencyId, limit, offset);
            } catch (SQLException exception) {
                logger.severe("Failed to load top balances for " + currencyId + ": " + exception.getMessage());
                return List.<PlayerAccount>of();
            }
        });
    }

    public CompletableFuture<Double> sumBalancesAsync(String currencyId) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.sumBalancesForCurrency(currencyId);
            } catch (SQLException exception) {
                logger.severe("Failed to sum balances for " + currencyId + ": " + exception.getMessage());
                return 0.0;
            }
        });
    }

    public CompletableFuture<Long> countAccountsAsync(String currencyId) {
        return asyncUtil.supplyAsync(() -> {
            try {
                return dao.countAccountsForCurrency(currencyId);
            } catch (SQLException exception) {
                logger.severe("Failed to count accounts for " + currencyId + ": " + exception.getMessage());
                return 0L;
            }
        });
    }
                           }
