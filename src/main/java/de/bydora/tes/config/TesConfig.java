package de.bydora.tes.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

/**
 * Central access point for TES's configurable values, backed by {@code config.yml}.
 */
public final class TesConfig {

    private static final String TALER_TO_TP_PATH = "ratios.taler-to-tp";
    private static final String TALER_TO_EP_PATH = "ratios.taler-to-ep";
    private static final String SHOP_RESTRICTED_WORLDS_PATH = "shops.restricted-worlds";
    private static final String SHOP_PROXIMITY_BLOCKS_PATH = "shops.proximity-blocks";
    private static final String SHOP_SESSION_TIMEOUT_SECONDS_PATH = "shops.session-timeout-seconds";

    private static final int DEFAULT_TALER_TO_TP = 5;
    private static final int DEFAULT_TALER_TO_EP = 3;
    private static final int DEFAULT_SHOP_PROXIMITY_BLOCKS = 10;
    private static final int DEFAULT_SHOP_SESSION_TIMEOUT_SECONDS = 120;

    private final JavaPlugin plugin;

    public TesConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Saves the bundled default {@code config.yml} if none exists yet and (re)loads it.
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    /**
     * How many loyalty points (Treuepunkte) a player earns per Taler spent on a completed transaction.
     */
    public int talerToTpRatio() {
        return plugin.getConfig().getInt(TALER_TO_TP_PATH, DEFAULT_TALER_TO_TP);
    }

    /**
     * How many experience points (Erfahrungspunkte) a player earns per Taler spent on a completed transaction.
     */
    public int talerToEpRatio() {
        return plugin.getConfig().getInt(TALER_TO_EP_PATH, DEFAULT_TALER_TO_EP);
    }

    /**
     * World names (lowercase) shops cannot be created in, e.g. the Kreativwelt.
     */
    public List<String> shopRestrictedWorlds() {
        return plugin.getConfig().getStringList(SHOP_RESTRICTED_WORLDS_PATH).stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();
    }

    /**
     * How close an owner must stand to their shop to close it via {@code /shop schließen}.
     */
    public int shopProximityBlocks() {
        return plugin.getConfig().getInt(SHOP_PROXIMITY_BLOCKS_PATH, DEFAULT_SHOP_PROXIMITY_BLOCKS);
    }

    /**
     * How long a shop creation/edit chat session waits for the next input before expiring.
     */
    public int shopSessionTimeoutSeconds() {
        return plugin.getConfig().getInt(SHOP_SESSION_TIMEOUT_SECONDS_PATH, DEFAULT_SHOP_SESSION_TIMEOUT_SECONDS);
    }
}
