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

    /**
     * The Treuepunkte cost of a Treueshop reward (spec §3.2.1.1), identified by its config id
     * (e.g. {@code "prozessverstaerker"}), reading {@code treueshop.rewards.<id>.cost}. One
     * generic accessor rather than a getter per reward keeps the ~16-entry reward table in a
     * single readable config block instead of scattered Java constants.
     */
    public int treueshopRewardCost(String rewardId, int fallback) {
        return plugin.getConfig().getInt("treueshop.rewards." + rewardId + ".cost", fallback);
    }

    private static final String TREUESHOP_HASTE_MINUTES_PATH = "treueshop.segen-der-zwerge.haste-minutes";
    private static final String TREUESHOP_KRAFTELIXIER_MINUTES_PATH = "treueshop.kraftelixier.effect-minutes";
    private static final int DEFAULT_TREUESHOP_HASTE_MINUTES = 30;
    private static final int DEFAULT_TREUESHOP_KRAFTELIXIER_MINUTES = 30;

    /**
     * How long Segen der Zwerge's Haste II effect lasts (spec §3.2.1.1, Belohnung 3).
     */
    public int treueshopHasteMinutes() {
        return plugin.getConfig().getInt(TREUESHOP_HASTE_MINUTES_PATH, DEFAULT_TREUESHOP_HASTE_MINUTES);
    }

    /**
     * How long Kraftelixier's effect bundle lasts (spec §3.2.1.1, Belohnung 7).
     */
    public int treueshopKraftelixierMinutes() {
        return plugin.getConfig().getInt(TREUESHOP_KRAFTELIXIER_MINUTES_PATH, DEFAULT_TREUESHOP_KRAFTELIXIER_MINUTES);
    }

    private static final String TREUESHOP_PROZESSVERSTAERKER_BOOST_MINUTES_PATH = "treueshop.prozessverstaerker.boost-minutes";
    private static final int DEFAULT_TREUESHOP_PROZESSVERSTAERKER_BOOST_MINUTES = 15;

    /**
     * How long a Prozessverstärker boost lasts on a single use (spec §3.2.1.1, Belohnung 1) —
     * governs both the furnace 2x-speed window and the beehive honey-doubling window; re-using
     * the item on an already-boosted block adds this on top of its remaining time.
     */
    public int treueshopProzessverstaerkerBoostMinutes() {
        return plugin.getConfig().getInt(TREUESHOP_PROZESSVERSTAERKER_BOOST_MINUTES_PATH, DEFAULT_TREUESHOP_PROZESSVERSTAERKER_BOOST_MINUTES);
    }

    private static final String HANDELSBONUS_DISCOUNT_DIAMONDS_PATH = "treueshop.handelsbonus.discount-diamonds";
    private static final String HANDELSBONUS_COOLDOWN_MIN_DAYS_PATH = "treueshop.handelsbonus.cooldown-min-days";
    private static final String HANDELSBONUS_COOLDOWN_MAX_DAYS_PATH = "treueshop.handelsbonus.cooldown-max-days";
    private static final String HANDELSBONUS_STAATSKASSE_WORLD_PATH = "treueshop.handelsbonus.staatskasse.world";
    private static final String HANDELSBONUS_STAATSKASSE_X_PATH = "treueshop.handelsbonus.staatskasse.x";
    private static final String HANDELSBONUS_STAATSKASSE_Y_PATH = "treueshop.handelsbonus.staatskasse.y";
    private static final String HANDELSBONUS_STAATSKASSE_Z_PATH = "treueshop.handelsbonus.staatskasse.z";
    private static final int DEFAULT_HANDELSBONUS_DISCOUNT_DIAMONDS = 5;
    private static final int DEFAULT_HANDELSBONUS_COOLDOWN_MIN_DAYS = 21;
    private static final int DEFAULT_HANDELSBONUS_COOLDOWN_MAX_DAYS = 28;

    /**
     * How many diamonds a triggered Handelsbonus discounts, in total, off future shop purchases
     * (spec §3.2.1.1, Belohnung 4).
     */
    public int treueshopHandelsbonusDiscountDiamonds() {
        return plugin.getConfig().getInt(HANDELSBONUS_DISCOUNT_DIAMONDS_PATH, DEFAULT_HANDELSBONUS_DISCOUNT_DIAMONDS);
    }

    /**
     * The random post-trigger cooldown range (days) before a player can trigger Handelsbonus
     * again — spec: "einem zufälligen Zeitraum zwischen 3 und 4 Wochen".
     */
    public int treueshopHandelsbonusCooldownMinDays() {
        return plugin.getConfig().getInt(HANDELSBONUS_COOLDOWN_MIN_DAYS_PATH, DEFAULT_HANDELSBONUS_COOLDOWN_MIN_DAYS);
    }

    public int treueshopHandelsbonusCooldownMaxDays() {
        return plugin.getConfig().getInt(HANDELSBONUS_COOLDOWN_MAX_DAYS_PATH, DEFAULT_HANDELSBONUS_COOLDOWN_MAX_DAYS);
    }

    /**
     * The world name of the Staatskasse chest funding Handelsbonus discounts (spec: "Truhe dessen
     * Koordinaten in der Configdatei... angegeben werden müssen"). Blank/unset means no chest is
     * configured yet — Handelsbonus purchases still work, but discounts nothing until an admin
     * sets one.
     */
    public String treueshopHandelsbonusStaatskasseWorld() {
        return plugin.getConfig().getString(HANDELSBONUS_STAATSKASSE_WORLD_PATH, "");
    }

    public int treueshopHandelsbonusStaatskasseX() {
        return plugin.getConfig().getInt(HANDELSBONUS_STAATSKASSE_X_PATH, 0);
    }

    public int treueshopHandelsbonusStaatskasseY() {
        return plugin.getConfig().getInt(HANDELSBONUS_STAATSKASSE_Y_PATH, 0);
    }

    public int treueshopHandelsbonusStaatskasseZ() {
        return plugin.getConfig().getInt(HANDELSBONUS_STAATSKASSE_Z_PATH, 0);
    }
}
