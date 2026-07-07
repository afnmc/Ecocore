package com.azthera.ecocore.data.dao;
 
import com.azthera.ecocore.data.model.PlayerAccount;
 
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
/**
 * Raw CRUD contract for player currency balances. One row exists per
 * (player, currencyId) pair to support multi-currency accounts.
 */
public interface PlayerAccountDAO {
 
    Optional<PlayerAccount> find(UUID playerId, String currencyId) throws SQLException;
 
    List<PlayerAccount> findAllForPlayer(UUID playerId) throws SQLException;
 
    List<PlayerAccount> findAllForCurrency(String currencyId, int limit, int offset) throws SQLException;
 
    /**
     * Inserts the account if it does not exist, or updates its balance/timestamp
     * if it does (upsert keyed on player_id + currency_id).
     */
    void upsert(PlayerAccount account) throws SQLException;
 
    void delete(UUID playerId, String currencyId) throws SQLException;
 
    long countAccountsForCurrency(String currencyId) throws SQLException;
 
    /**
     * @return sum of all balances for the given currency, used by
     * MoneySupplyTracker to compute total money supply.
     */
    double sumBalancesForCurrency(String currencyId) throws SQLException;
}
