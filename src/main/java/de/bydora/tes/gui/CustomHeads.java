package de.bydora.tes.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * Custom-textured {@code PLAYER_HEAD} icons used as pagination controls across TES's InvUI
 * screens, matching the icons already placed in the spec's reference GUI builds (creative world,
 * see {@code docs/gui-reference-capture.md}). These are not vanilla player skulls, so the texture
 * can't be derived from a player name — the base64 {@code textures} property below was obtained
 * in-game and is documented in {@code docs/gui-library.md} for regenerating/replacing it.
 */
public final class CustomHeads {

    private static final String NEXT_PAGE_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19";
    private static final String PREVIOUS_PAGE_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==";

    private CustomHeads() {
    }

    /**
     * The "➤ Weiter" (next page) head, shared by every paginated TES screen.
     */
    public static ItemStack nextPageHead() {
        return texturedHead(NEXT_PAGE_TEXTURE);
    }

    /**
     * The "⮜ Zurück" (previous page) head, shown in place of the close button from page 2
     * onward on every paginated TES screen (see {@link PaginationControls}).
     */
    public static ItemStack previousPageHead() {
        return texturedHead(PREVIOUS_PAGE_TEXTURE);
    }

    /**
     * Builds a {@code PLAYER_HEAD} {@link ItemStack} carrying the given base64-encoded
     * {@code textures} profile property.
     */
    public static ItemStack texturedHead(String base64Texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", base64Texture));
        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }
}
