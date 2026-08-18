package de.bydora.tes.data;

import java.util.UUID;

/**
 * A registered player's TES data.
 *
 * @param uuid             the player's Minecraft UUID
 * @param username         the player's last known username
 * @param treuepunkte      current loyalty point balance
 * @param erfahrungspunkte current accumulated experience points
 * @param level            current TES level
 * @param paused           whether the player's reward mechanics are currently suspended
 * @param registeredAt     epoch millis when the player was registered
 * @param updatedAt        epoch millis of the last update to this record
 */
public record PlayerRecord(
        UUID uuid,
        String username,
        int treuepunkte,
        int erfahrungspunkte,
        int level,
        boolean paused,
        long registeredAt,
        long updatedAt
) {
}
