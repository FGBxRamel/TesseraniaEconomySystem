package de.bydora.tes.config;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Central access point for TES's configurable values, backed by {@code config.yml}.
 */
public final class TesConfig {

    private static final String TALER_TO_TP_PATH = "ratios.taler-to-tp";
    private static final String TALER_TO_EP_PATH = "ratios.taler-to-ep";

    private static final int DEFAULT_TALER_TO_TP = 5;
    private static final int DEFAULT_TALER_TO_EP = 3;

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
}
