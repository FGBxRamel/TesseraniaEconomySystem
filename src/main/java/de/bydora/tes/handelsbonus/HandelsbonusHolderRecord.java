package de.bydora.tes.handelsbonus;

import java.util.UUID;

/**
 * A player who has triggered the Handelsbonus (spec §3.2.1.1, Belohnung 4) and is still within
 * its post-trigger cooldown, or still has unused discount left over from it.
 *
 * @param uuid              the holder
 * @param discountRemaining diamonds still discountable on future shop purchases — never expires
 *                          on its own ("Nicht verbrauchte Werte bleiben erhalten"), only decreases
 *                          as it's spent
 * @param cooldownUntil     epoch millis before which this player can't trigger Handelsbonus
 *                          again, and which counts them toward the max-2-concurrent cap
 */
public record HandelsbonusHolderRecord(UUID uuid, int discountRemaining, long cooldownUntil) {

    public boolean cooldownActive(long now) {
        return now < cooldownUntil;
    }
}
