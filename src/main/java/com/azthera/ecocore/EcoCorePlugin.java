package com.azthera.ecocore;
 
import com.azthera.ecocore.api.EcoCoreAPI;
import com.azthera.ecocore.api.EcoCoreProvider;
import com.azthera.ecocore.bootstrap.Module;
import com.azthera.ecocore.bootstrap.ModuleManager;
import com.azthera.ecocore.command.EcoCommand;
import com.azthera.ecocore.command.sub.*;
import com.azthera.ecocore.config.ConfigManager;
import com.azthera.ecocore.data.DatabaseManager;
import com.azthera.ecocore.data.dao.*;
import com.azthera.ecocore.data.dao.impl.*;
import com.azthera.ecocore.data.migration.SchemaMigrator;
import com.azthera.ecocore.data.migration.SqlDialect;
import com.azthera.ecocore.data.repository.*;
import com.azthera.ecocore.economy.*;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.inflation.*;
import com.azthera.ecocore.integration.*;
import com.azthera.ecocore.jobs.*;
import com.azthera.ecocore.jobs.listener.JobActionListener;
import com.azthera.ecocore.listener.InventoryClickListener;
import com.azthera.ecocore.listener.NPCInteractListener;
import com.azthera.ecocore.listener.PlayerJoinQuitListener;
import com.azthera.ecocore.logging.*;
import com.azthera.ecocore.market.*;
import com.azthera.ecocore.minions.*;
import com.azthera.ecocore.quest.*;
import com.azthera.ecocore.sell.*;
import com.azthera.ecocore.shop.*;
import com.azthera.ecocore.util.AsyncUtil;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.NumberFormatter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
 
import java.util.List;
import java.util.logging.Logger;
 
/**
 * Composition root of EcoCore. Owns the construction and wiring of every
 * service/manager/repository in dependency order, registers commands and
 * listeners, bridges to Vault/PlaceholderAPI/LuckPerms, and exposes the
 * public {@link EcoCoreAPI} to other plugins via {@link EcoCoreProvider}.
 * No other class in the plugin should construct these objects itself —
 * everything is instantiated exactly once, here.
 */
public final class EcoCorePlugin extends JavaPlugin {
 
    // Core infrastructure
    private SchedulerAdapter schedulerAdapter;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private AsyncUtil asyncUtil;
    private MessageService messageService;
    private NumberFormatter numberFormatter;
    private ModuleManager moduleManager;
 
    // Logging
    private DebugLogger debugLogger;
    private ErrorLogger errorLogger;
    private EconomyLogger economyLogger;
    private AuditLogger auditLogger;
 
    // Repositories
    private PlayerAccountRepository playerAccountRepository;
    private ShopItemRepository shopItemRepository;
    private JobDataRepository jobDataRepository;
    private QuestDataRepository questDataRepository;
    private MinionRepository minionRepository;
    private TransactionLogRepository transactionLogRepository;
    private MarketEventRepository marketEventRepository;
 
    // Economy
    private CurrencyManager currencyManager;
    private TaxManager taxManager;
    private MoneySinkManager moneySinkManager;
    private EconomyService economyService;
    private EconomyStatisticsService economyStatisticsService;
    private VaultEconomyProvider vaultEconomyProvider;
 
    // Shop
    private PricingEngine pricingEngine;
    private StockManager stockManager;
    private RestockScheduler restockScheduler;
    private ShopManager shopManager;
    private ShopNPCManager shopNPCManager;
    private ShopTransactionService shopTransactionService;
 
    // Market
    private DemandSupplyCalculator demandSupplyCalculator;
    private WeightedEconomyModel weightedEconomyModel;
    private ServerNewsBroadcaster serverNewsBroadcaster;
    private MarketEventManager marketEventManager;
    private MarketSimulationEngine marketSimulationEngine;
    private MarketTickTask marketTickTask;
 
    // Inflation
    private MoneySupplyTracker moneySupplyTracker;
    private VelocityCalculator velocityCalculator;
    private InflationCalculator inflationCalculator;
    private InflationAdjustmentService inflationAdjustmentService;
    private InflationModule inflationModule;
 
    // Sell
    private SellFilterManager sellFilterManager;
    private SellMultiplierResolver sellMultiplierResolver;
    private SellService sellService;
    private AutoSellManager autoSellManager;
 
    // Jobs
    private ComboTracker comboTracker;
    private JobRewardCalculator jobRewardCalculator;
    private PrestigeManager prestigeManager;
    private SkillTreeRegistry skillTreeRegistry;
    private BonusScheduler bonusScheduler;
    private JobProgressService jobProgressService;
    private JobManager jobManager;
 
    // Quest
    private QuestRegistry questRegistry;
    private QuestChainResolver questChainResolver;
    private QuestProgressTracker questProgressTracker;
    private QuestRewardService questRewardService;
    private QuestGenerator questGenerator;
    private QuestManager questManager;
 
    // Minions
    private MinionLimitPolicy minionLimitPolicy;
    private MinionFuelManager minionFuelManager;
    private MinionStorageManager minionStorageManager;
    private MinionUpgradeService minionUpgradeService;
    private MinionTaskScheduler minionTaskScheduler;
    private MinionManager minionManager;
 
    // GUI framework
    private GuiSessionManager guiSessionManager;
    private GuiIconResolver guiIconResolver;
 
    // Integrations
    private LuckPermsIntegration luckPermsIntegration;
 
    @Override
    public void onEnable() {
        Logger logger = getLogger();
 
        initializeSchedulerAdapter(logger);
        initializeConfig();
        initializeDatabase(logger);
        initializeCoreUtilities(logger);
        initializeRepositories(logger);
        initializeEconomy(logger);
        initializeShop(logger);
        initializeMarket(logger);
        initializeInflation(logger);
        initializeSell();
        initializeJobs(logger);
        initializeQuest(logger);
        initializeMinions(logger);
        initializeGuiFramework();
        loadShopCatalogFromConfig();
        registerDefaultQuestCatalog();
        registerModules(logger);
        registerListeners();
        registerCommands();
        registerIntegrations(logger);
        registerPublicApi();
 
        moduleManager.enableAll();
        logger.info("EcoCore has been enabled successfully.");
    }
 
    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        EcoCoreProvider.unregister();
        if (asyncUtil != null) {
            asyncUtil.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("EcoCore has been disabled.");
    }
 
    private void initializeSchedulerAdapter(Logger logger) {
        boolean folia = isFoliaServer();
        this.schedulerAdapter = folia ? new FoliaSchedulerAdapter(this) : new BukkitSchedulerAdapter(this);
        logger.info(() -> "Scheduler mode: " + (folia ? "Folia" : "Paper (classic)"));
    }
 
    private boolean isFoliaServer() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
 
    private void initializeConfig() {
        this.configManager = new ConfigManager(this);
        configManager.loadAll();
    }
 
    private void initializeDatabase(Logger logger) {
        this.databaseManager = new DatabaseManager(this);
        databaseManager.connect(configManager.getMainConfig());
        new SchemaMigrator(databaseManager, logger).migrate();
    }
 
    private void initializeCoreUtilities(Logger logger) {
        this.asyncUtil = new AsyncUtil(schedulerAdapter);
        this.messageService = new MessageService(configManager.getMessagePrefix());
        messageService.reload(configManager.getMessagesConfig().asFlatMap());
        this.numberFormatter = new NumberFormatter(2, true);
        this.moduleManager = new ModuleManager(logger);
 
        this.debugLogger = new DebugLogger(logger, configManager.isDebugMode());
        this.errorLogger = new ErrorLogger(logger);
        this.economyLogger = new EconomyLogger(logger);
        this.auditLogger = new AuditLogger(logger);
    }
 
    private void initializeRepositories(Logger logger) {
        SqlDialect dialect = new SqlDialect(databaseManager.getDatabaseType());
 
        PlayerAccountDAO playerAccountDAO = new SqlPlayerAccountDAO(databaseManager, dialect);
        ShopItemDAO shopItemDAO = new SqlShopItemDAO(databaseManager, dialect);
        JobDataDAO jobDataDAO = new SqlJobDataDAO(databaseManager, dialect);
        QuestDataDAO questDataDAO = new SqlQuestDataDAO(databaseManager, dialect);
        MinionDAO minionDAO = new SqlMinionDAO(databaseManager, dialect);
        TransactionLogDAO transactionLogDAO = new SqlTransactionLogDAO(databaseManager, dialect);
        MarketEventDAO marketEventDAO = new SqlMarketEventDAO(databaseManager, dialect);
 
        this.playerAccountRepository = new PlayerAccountRepository(playerAccountDAO, asyncUtil, logger);
        this.shopItemRepository = new ShopItemRepository(shopItemDAO, asyncUtil, logger);
        this.jobDataRepository = new JobDataRepository(jobDataDAO, asyncUtil, logger);
        this.questDataRepository = new QuestDataRepository(questDataDAO, asyncUtil, logger);
        this.minionRepository = new MinionRepository(minionDAO, asyncUtil, logger);
        this.transactionLogRepository = new TransactionLogRepository(transactionLogDAO, asyncUtil, logger);
        this.marketEventRepository = new MarketEventRepository(marketEventDAO, asyncUtil, logger);
    }
 
    private void initializeEconomy(Logger logger) {
        this.currencyManager = new CurrencyManager(configManager.getEconomyConfig());
        this.taxManager = new TaxManager(configManager.getEconomyConfig());
        this.moneySinkManager = new MoneySinkManager(configManager.getEconomyConfig(), logger);
        this.economyService = new EconomyServiceImpl(
            playerAccountRepository, transactionLogRepository, currencyManager, taxManager, moneySinkManager, logger
        );
        this.economyStatisticsService = new EconomyStatisticsService(
            playerAccountRepository, transactionLogRepository, moneySinkManager
        );
 
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            this.vaultEconomyProvider = new VaultEconomyProvider(economyService, currencyManager);
            getServer().getServicesManager().register(Economy.class, vaultEconomyProvider, this, ServicePriority.Highest);
            logger.info("Vault economy provider registered.");
        } else {
            logger.warning("Vault not found; third-party plugins using Vault's Economy API will not see EcoCore balances.");
        }
    }
 
    private void initializeShop(Logger logger) {
        this.pricingEngine = new ElasticPricingEngine();
        this.stockManager = new StockManager(shopItemRepository);
        this.restockScheduler = new RestockScheduler(
            shopItemRepository, stockManager, configManager.getShopConfig(), schedulerAdapter, logger
        );
        this.shopManager = new ShopManager(shopItemRepository, restockScheduler, configManager.getShopConfig(), logger);
        this.shopNPCManager = new ShopNPCManager();
        this.shopTransactionService = new ShopTransactionService(
            shopManager, shopItemRepository, stockManager, pricingEngine, economyService
        );
    }
 
    private void initializeMarket(Logger logger) {
        this.demandSupplyCalculator = new DemandSupplyCalculator();
        this.weightedEconomyModel = new WeightedEconomyModel(demandSupplyCalculator);
        this.serverNewsBroadcaster = new ServerNewsBroadcaster(messageService, schedulerAdapter, configManager.getMarketConfig());
        this.marketEventManager = new MarketEventManager(
            marketEventRepository, shopManager, pricingEngine, shopItemRepository,
            serverNewsBroadcaster, configManager.getMarketConfig(), logger
        );
        this.marketSimulationEngine = new MarketSimulationEngine(
            shopItemRepository, pricingEngine, weightedEconomyModel, marketEventManager,
            configManager.getMarketConfig(), logger
        );
        this.marketTickTask = new MarketTickTask(
            marketSimulationEngine, marketEventManager, schedulerAdapter, configManager.getMarketConfig(), logger
        );
    }
 
    private void initializeInflation(Logger logger) {
        this.moneySupplyTracker = new MoneySupplyTracker(economyStatisticsService);
        this.velocityCalculator = new VelocityCalculator();
        this.inflationCalculator = new InflationCalculator(velocityCalculator, configManager.getInflationConfig());
        this.inflationAdjustmentService = new InflationAdjustmentService(
            shopItemRepository, moneySupplyTracker, inflationCalculator, taxManager,
            economyStatisticsService, configManager.getInflationConfig(), logger
        );
        this.inflationModule = new InflationModule(
            inflationAdjustmentService, currencyManager, configManager.getInflationConfig(), schedulerAdapter, logger
        );
    }
 
    private void initializeSell() {
        this.sellFilterManager = new SellFilterManager(configManager.getSellConfig());
        this.sellMultiplierResolver = new SellMultiplierResolver(configManager.getSellConfig());
        this.sellService = new SellService(
            shopManager, shopTransactionService, sellFilterManager, sellMultiplierResolver, economyService
        );
        this.autoSellManager = new AutoSellManager(sellService, sellFilterManager);
    }
 
    private void initializeJobs(Logger logger) {
        this.comboTracker = new ComboTracker(configManager.getJobsConfig());
        this.jobRewardCalculator = new JobRewardCalculator(configManager.getJobsConfig());
        this.prestigeManager = new PrestigeManager(jobDataRepository, configManager.getJobsConfig());
        this.skillTreeRegistry = new SkillTreeRegistry();
        this.bonusScheduler = new BonusScheduler();
        this.jobProgressService = new JobProgressService(
            jobDataRepository, jobRewardCalculator, comboTracker, prestigeManager,
            bonusScheduler, economyService, configManager.getJobsConfig()
        );
        this.jobManager = new JobManager(
            jobDataRepository, bonusScheduler, schedulerAdapter, configManager.getJobsConfig(), logger
        );
    }
 
    private void initializeQuest(Logger logger) {
        this.questRegistry = new QuestRegistry();
        this.questChainResolver = new QuestChainResolver(questRegistry);
        this.questProgressTracker = new QuestProgressTracker(questDataRepository, questRegistry);
        this.questRewardService = new QuestRewardService(questDataRepository, questRegistry, economyService);
        this.questGenerator = new QuestGenerator(questRegistry, questDataRepository, configManager.getQuestConfig());
        this.questManager = new QuestManager(
            questDataRepository, questRegistry, questGenerator, questProgressTracker, questRewardService,
            questChainResolver, schedulerAdapter, configManager.getQuestConfig(), logger
        );
    }
 
    private void initializeMinions(Logger logger) {
        this.minionLimitPolicy = new MinionLimitPolicy(minionRepository, configManager.getMinionsConfig());
        this.minionFuelManager = new MinionFuelManager();
        this.minionStorageManager = new MinionStorageManager(configManager.getMinionsConfig(), logger);
        this.minionUpgradeService = new MinionUpgradeService(minionRepository, economyService, configManager.getMinionsConfig());
        this.minionTaskScheduler = new MinionTaskScheduler(
            minionRepository, minionFuelManager, minionUpgradeService, minionStorageManager,
            shopTransactionService, schedulerAdapter, logger
        );
        this.minionManager = new MinionManager(
            minionRepository, minionLimitPolicy, minionStorageManager, minionTaskScheduler,
            configManager.getMinionsConfig(), logger
        );
    }
 
    private void initializeGuiFramework() {
        this.guiSessionManager = new GuiSessionManager();
        this.guiIconResolver = new GuiIconResolver();
    }
 
    /**
     * Loads the entire shop catalog (categories + items) from
     * {@code modules/shop-items.yml} via {@link ShopCatalogLoader}. This is
     * the only place the catalog is populated — server owners add, edit, or
     * remove shop items purely by editing that YAML file and running
     * {@code /eco reload}; no plugin code change or rebuild is needed.
     */
    private void loadShopCatalogFromConfig() {
        new ShopCatalogLoader(getLogger()).loadInto(shopManager, configManager.getShopItemsConfig());
    }
 
    /**
     * Registers a small starter quest catalog (one per rotation type) so
     * the Quest module has something to hand out immediately after install.
     * As with the shop catalog, server owners are expected to expand this
     * through a future config-driven quest catalog file.
     */
    private void registerDefaultQuestCatalog() {
        questRegistry.register(new QuestDefinition(
            "daily_mine_stone", QuestType.DAILY, "Penambang Harian",
            "Tambang 64 stone atau deepslate.", "mine:stone", 64.0, 50.0, 100.0,
            "block.stone.break", false, null, List.of()
        ));
        questRegistry.register(new QuestDefinition(
            "daily_sell_any", QuestType.DAILY, "Pedagang Harian",
            "Jual barang apapun ke shop sebanyak 20 kali.", "sell:any", 20.0, 40.0, 80.0,
            "entity.villager.trade", false, null, List.of()
        ));
        questRegistry.register(new QuestDefinition(
            "weekly_fish_catch", QuestType.WEEKLY, "Nelayan Mingguan",
            "Tangkap 100 ikan.", "fish:catch", 100.0, 300.0, 500.0,
            "entity.fishing_bobber.retrieve", true, null, List.of()
        ));
        questRegistry.register(new QuestDefinition(
            "monthly_kill_zombie", QuestType.MONTHLY, "Pemburu Bulanan",
            "Kalahkan 200 zombie.", "kill:zombie", 200.0, 1000.0, 2000.0,
            "entity.zombie.death", true, null, List.of()
        ));
        questRegistry.register(new QuestDefinition(
            "random_explore", QuestType.RANDOM, "Penjelajah",
            "Jelajahi 50 chunk baru.", "explore:chunk", 50.0, 60.0, 100.0,
            "entity.player.levelup", false, null, List.of()
        ));
    }
 
    private void registerModules(Logger logger) {
        moduleManager.registerModule(shopManager);
        moduleManager.registerModule(marketTickTask);
        moduleManager.registerModule(inflationModule);
        moduleManager.registerModule(jobManager);
        moduleManager.registerModule(questManager);
        moduleManager.registerModule(minionManager);
    }
 
    private void registerListeners() {
        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerJoinQuitListener(
            economyService, jobManager, questManager, minionManager, autoSellManager
        ), this);
        pluginManager.registerEvents(new InventoryClickListener(guiSessionManager), this);
        pluginManager.registerEvents(new NPCInteractListener(
            shopNPCManager, shopManager, shopTransactionService, configManager.getGuiConfig(),
            guiIconResolver, messageService, guiSessionManager, numberFormatter
        ), this);
        pluginManager.registerEvents(new JobActionListener(jobProgressService, autoSellManager, messageService), this);
    }
 
    private void registerCommands() {
        EcoCommand ecoCommand = new EcoCommand(messageService);
 
        ecoCommand.register(new ReloadSubCommand(configManager, this::reloadAllModules, messageService));
        ecoCommand.register(new GiveSubCommand(economyService, messageService));
        ecoCommand.register(new DebugSubCommand(debugLogger));
        ecoCommand.register(new MarketSubCommand(marketSimulationEngine, marketEventManager));
        ecoCommand.register(new InflationSubCommand(inflationAdjustmentService, currencyManager));
        ecoCommand.register(new JobsSubCommand(jobManager));
        ecoCommand.register(new QuestSubCommand(questManager));
        ecoCommand.register(new ShopSubCommand(shopManager, shopItemRepository, guiSessionManager, configManager.getGuiConfig(), numberFormatter));
        ecoCommand.register(new EconomySubCommand(economyStatisticsService, currencyManager, numberFormatter));
 
        var command = getCommand("eco");
        if (command != null) {
            command.setExecutor(ecoCommand);
            command.setTabCompleter(ecoCommand);
        }
    }
 
    private void registerIntegrations(Logger logger) {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderApiExpansion(this, economyService, jobManager, questManager, minionManager, numberFormatter)
                .register();
            logger.info("PlaceholderAPI expansion registered.");
        }
 
        this.luckPermsIntegration = new LuckPermsIntegration(logger);
        luckPermsIntegration.tryInitialize();
    }
 
    private void registerPublicApi() {
        EcoCoreProvider.register(new EcoCoreApiImpl());
    }
 
    /**
     * Invoked by {@code /eco reload}. Reloads every config file, then pushes
     * the fresh config objects into every component that holds one, then
     * finally reloads every registered {@link Module}.
     */
    private void reloadAllModules() {
        configManager.reloadAll();
        debugLogger.setEnabled(configManager.isDebugMode());
        messageService.reload(configManager.getMessagesConfig().asFlatMap());
 
        currencyManager.reload(configManager.getEconomyConfig());
        taxManager.reload(configManager.getEconomyConfig());
        moneySinkManager.reload(configManager.getEconomyConfig());
 
        shopManager.updateShopConfig(configManager.getShopConfig());
        marketTickTask.updateMarketConfig(configManager.getMarketConfig());
        serverNewsBroadcaster.reload(configManager.getMarketConfig());
 
        inflationCalculator.reload(configManager.getInflationConfig());
        inflationAdjustmentService.reload(configManager.getInflationConfig());
        inflationModule.updateInflationConfig(configManager.getInflationConfig());
 
        sellFilterManager.load(configManager.getSellConfig());
        sellMultiplierResolver.load(configManager.getSellConfig());
 
        comboTracker.reload(configManager.getJobsConfig());
        jobRewardCalculator.reload(configManager.getJobsConfig());
        prestigeManager.reload(configManager.getJobsConfig());
        jobManager.updateJobsConfig(configManager.getJobsConfig());
 
        questGenerator.reload(configManager.getQuestConfig());
        questManager.updateQuestConfig(configManager.getQuestConfig());
 
        minionLimitPolicy.reload(configManager.getMinionsConfig());
        minionStorageManager.reload(configManager.getMinionsConfig());
        minionManager.updateMinionsConfig(configManager.getMinionsConfig());
 
        moduleManager.reloadAll();
    }
 
    /**
     * Lightweight {@link Module} wrapper for the Inflation system, scheduling
     * {@code InflationAdjustmentService#runAdjustmentCycle} on a repeating
     * async task at the configured interval. Kept as a private nested class
     * here rather than its own top-level file since it is purely scheduling
     * glue with no logic of its own beyond what {@code InflationAdjustmentService}
     * already implements.
     */
    private static final class InflationModule implements Module {
 
        private final InflationAdjustmentService adjustmentService;
        private final CurrencyManager currencyManager;
        private com.azthera.ecocore.config.InflationConfig inflationConfig;
        private final SchedulerAdapter schedulerAdapter;
        private final Logger logger;
        private EcoScheduledTask scheduledTask;
        private boolean enabled;
 
        private InflationModule(InflationAdjustmentService adjustmentService, CurrencyManager currencyManager,
                                 com.azthera.ecocore.config.InflationConfig inflationConfig,
                                 SchedulerAdapter schedulerAdapter, Logger logger) {
            this.adjustmentService = adjustmentService;
            this.currencyManager = currencyManager;
            this.inflationConfig = inflationConfig;
            this.schedulerAdapter = schedulerAdapter;
            this.logger = logger;
        }
 
        @Override
        public String getName() {
            return "inflation";
        }
 
        @Override
        public void enable() {
            this.enabled = true;
            long intervalTicks = inflationConfig.getCalculationIntervalSeconds() * 20L;
            scheduledTask = schedulerAdapter.runAsyncRepeating(() -> {
                long windowStart = System.currentTimeMillis() - (inflationConfig.getCalculationIntervalSeconds() * 1000L);
                adjustmentService.runAdjustmentCycle(currencyManager.getPrimaryCurrencyId(), windowStart);
            }, intervalTicks, intervalTicks);
            logger.info(() -> "Inflation cycle scheduled every " + inflationConfig.getCalculationIntervalSeconds() + " seconds.");
        }
 
        @Override
        public void disable() {
            this.enabled = false;
            if (scheduledTask != null && !scheduledTask.isCancelled()) {
                scheduledTask.cancel();
            }
        }
 
        @Override
        public void reload() {
            disable();
            enable();
        }
 
        @Override
        public boolean isEnabled() {
            return enabled;
        }
 
        private void updateInflationConfig(com.azthera.ecocore.config.InflationConfig inflationConfig) {
            this.inflationConfig = inflationConfig;
        }
    }
 
    /**
     * Concrete {@link EcoCoreAPI} implementation exposing only the
     * manager/service types intended for third-party consumption.
     */
    private final class EcoCoreApiImpl implements EcoCoreAPI {
 
        @Override
        public EconomyService getEconomyService() {
            return economyService;
        }
 
        @Override
        public ShopManager getShopManager() {
            return shopManager;
        }
 
        @Override
        public JobManager getJobManager() {
            return jobManager;
        }
 
        @Override
        public QuestManager getQuestManager() {
            return questManager;
        }
 
        @Override
        public MinionManager getMinionManager() {
            return minionManager;
        }
 
        @Override
        public String getVersion() {
            return getPluginMeta().getVersion();
        }
    }
}
