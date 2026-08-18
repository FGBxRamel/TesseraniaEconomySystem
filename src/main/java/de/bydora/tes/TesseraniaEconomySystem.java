package de.bydora.tes;

import de.bydora.tes.command.confirm.ConfirmationManager;
import de.bydora.tes.config.TesConfig;
import de.bydora.tes.data.Database;
import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.data.SqlitePlayerRepository;
import de.bydora.tes.shop.PendingNotificationListener;
import de.bydora.tes.shop.PendingNotificationRepository;
import de.bydora.tes.shop.ShopMaintenanceTask;
import de.bydora.tes.shop.ShopProtectionListener;
import de.bydora.tes.shop.ShopRegistry;
import de.bydora.tes.shop.ShopRepository;
import de.bydora.tes.shop.ShopTradeListener;
import de.bydora.tes.shop.ShopTransactionRepository;
import de.bydora.tes.shop.SqlitePendingNotificationRepository;
import de.bydora.tes.shop.SqliteShopRepository;
import de.bydora.tes.shop.SqliteShopTransactionRepository;
import de.bydora.tes.shop.session.ShopChatListener;
import de.bydora.tes.shop.session.ShopSessionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.UUID;

public final class TesseraniaEconomySystem extends JavaPlugin {

    private final ConfirmationManager<UUID> removeConfirmations = new ConfirmationManager<>(Duration.ofSeconds(30));

    private Database database;
    private TesConfig tesConfig;
    private PlayerRepository playerRepository;
    private ShopRepository shopRepository;
    private ShopTransactionRepository shopTransactionRepository;
    private PendingNotificationRepository pendingNotificationRepository;
    private ShopRegistry shopRegistry;
    private ShopSessionManager shopSessionManager;
    private ShopChatListener shopChatListener;
    private BukkitTask shopMaintenanceTask;

    @Override
    public void onEnable() {
        tesConfig = new TesConfig(this);
        tesConfig.load();

        database = new Database(this);
        database.open();
        playerRepository = new SqlitePlayerRepository(database);
        shopRepository = new SqliteShopRepository(database);
        shopTransactionRepository = new SqliteShopTransactionRepository(database);
        pendingNotificationRepository = new SqlitePendingNotificationRepository(database);

        shopRegistry = new ShopRegistry(shopRepository);
        shopRegistry.load();
        shopSessionManager = new ShopSessionManager(Duration.ofSeconds(tesConfig.shopSessionTimeoutSeconds()));

        shopChatListener = new ShopChatListener(this, shopSessionManager, shopRepository, shopRegistry);

        Bukkit.getPluginManager().registerEvents(new ShopProtectionListener(shopRegistry), this);
        Bukkit.getPluginManager().registerEvents(shopChatListener, this);
        Bukkit.getPluginManager().registerEvents(new ShopTradeListener(shopRegistry, shopTransactionRepository), this);
        Bukkit.getPluginManager().registerEvents(new PendingNotificationListener(pendingNotificationRepository), this);

        ShopMaintenanceTask maintenanceTask = new ShopMaintenanceTask(this, shopRegistry, shopRepository,
                shopTransactionRepository, playerRepository, pendingNotificationRepository, tesConfig);
        shopMaintenanceTask = maintenanceTask.runTaskTimer(this, 100L, 100L);
    }

    @Override
    public void onDisable() {
        if (shopMaintenanceTask != null) {
            shopMaintenanceTask.cancel();
        }
        if (database != null) {
            database.close();
        }
    }

    /**
     * Looked up lazily by command handlers (registered during the bootstrap phase, before
     * {@link #onEnable()} has run) via {@code JavaPlugin.getPlugin(TesseraniaEconomySystem.class)}.
     */
    public PlayerRepository playerRepository() {
        return playerRepository;
    }

    public ShopRepository shopRepository() {
        return shopRepository;
    }

    public ShopTransactionRepository shopTransactionRepository() {
        return shopTransactionRepository;
    }

    public PendingNotificationRepository pendingNotificationRepository() {
        return pendingNotificationRepository;
    }

    public ShopRegistry shopRegistry() {
        return shopRegistry;
    }

    public ShopSessionManager shopSessionManager() {
        return shopSessionManager;
    }

    public ShopChatListener shopChatListener() {
        return shopChatListener;
    }

    public TesConfig tesConfig() {
        return tesConfig;
    }

    public ConfirmationManager<UUID> removeConfirmations() {
        return removeConfirmations;
    }
}
