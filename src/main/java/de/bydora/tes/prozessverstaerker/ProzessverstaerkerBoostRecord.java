package de.bydora.tes.prozessverstaerker;

/**
 * One block currently boosted by the Prozessverstärker (spec §3.2.1.1, Belohnung 1).
 *
 * @param world     the block's world name
 * @param x         block x
 * @param y         block y
 * @param z         block z
 * @param kind      which boost behavior applies
 * @param expiresAt epoch millis the boost stops applying
 */
public record ProzessverstaerkerBoostRecord(String world, int x, int y, int z, BoostKind kind, long expiresAt) {

    public boolean isExpired(long now) {
        return now >= expiresAt;
    }
}
