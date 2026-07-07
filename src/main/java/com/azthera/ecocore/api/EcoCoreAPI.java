package com.azthera.ecocore.api;
 
import com.azthera.ecocore.economy.EconomyService;
import com.azthera.ecocore.jobs.JobManager;
import com.azthera.ecocore.minions.MinionManager;
import com.azthera.ecocore.quest.QuestManager;
import com.azthera.ecocore.shop.ShopManager;
 
/**
 * Public-facing API surface for third-party plugins to integrate with
 * EcoCore without depending on its internal package structure. Obtain an
 * instance via {@link EcoCoreProvider#get()} once EcoCore has fully enabled.
 * Only exposes the manager/service types meant for external consumption —
 * internal classes like repositories, DAOs, and config classes are
 * deliberately not reachable through this interface.
 */
public interface EcoCoreAPI {
 
    EconomyService getEconomyService();
 
    ShopManager getShopManager();
 
    JobManager getJobManager();
 
    QuestManager getQuestManager();
 
    MinionManager getMinionManager();
 
    /**
     * @return the running EcoCore plugin version string, useful for
     * third-party plugins to gate feature usage against a minimum version.
     */
    String getVersion();
}
