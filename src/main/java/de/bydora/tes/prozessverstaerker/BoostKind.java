package de.bydora.tes.prozessverstaerker;

import org.bukkit.Material;

import java.util.Set;

/**
 * The two block categories the Prozessverstärker (spec §3.2.1.1, Belohnung 1) can boost.
 */
public enum BoostKind {

    /** Any furnace variant ("Ofen, egal welche Art") — burns twice as fast while boosted. */
    FURNACE(Set.of(Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER)),

    /**
     * A beehive or bee nest ("Bienenstock") — each natural honey-level increment is doubled
     * while boosted.
     */
    BEEHIVE(Set.of(Material.BEEHIVE, Material.BEE_NEST));

    private final Set<Material> materials;

    BoostKind(Set<Material> materials) {
        this.materials = materials;
    }

    /**
     * The kind whose {@link #materials} contains {@code material}, or {@code null} if it isn't
     * boostable.
     */
    public static BoostKind forMaterial(Material material) {
        for (BoostKind kind : values()) {
            if (kind.materials.contains(material)) {
                return kind;
            }
        }
        return null;
    }
}
