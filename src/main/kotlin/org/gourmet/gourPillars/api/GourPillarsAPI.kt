package org.gourmet.gourPillars.api

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.gourmet.gourPillars.api.event.GameEventDefinition
import org.gourmet.gourPillars.database.PlayerStats
import java.util.concurrent.CompletableFuture

// Get an instance via GourPillars.api or Bukkit.getServicesManager().load(GourPillarsAPI::class.java).
// Main thread only, except getPlayerStatistics (completes on a background DB thread).
interface GourPillarsAPI {
    fun getArenas(): List<ArenaInfo>

    // Public, not full, not mid-match.
    fun getAvailableArenas(): List<ArenaInfo>

    fun getArena(name: String): ArenaInfo?

    fun isArenaFull(name: String): Boolean?

    fun getArenaOfPlayer(player: Player): ArenaInfo?

    // Same rules as /join.
    fun sendPlayerToArena(
        player: Player,
        arenaName: String,
    ): ArenaJoinResult

    // Same as /leave. False if the player wasn't in an arena.
    fun removePlayerFromArena(player: Player): Boolean

    // Still standing in the glass spawn cage, before the match starts.
    fun isPlayerCaged(player: Player): Boolean

    fun getPlayerLocation(player: Player): Location

    fun isSpectating(player: Player): Boolean

    fun getSpectatedArena(player: Player): ArenaInfo?

    // Snapshot copy, null if the arena doesn't exist.
    fun getSpectators(arenaName: String): List<Player>?

    // Snapshot copy, null if the arena doesn't exist.
    fun getPlayersInArena(arenaName: String): List<Player>?

    // Snapshot copy, null if the arena doesn't exist or isn't in-game.
    fun getAlivePlayers(arenaName: String): List<Player>?

    // Null if the player isn't in an active match.
    fun getMatchKills(player: Player): Int?

    fun getTimeRemainingSeconds(arenaName: String): Int?

    // Id of the game event active in the arena's current match (see registerEvent),
    // or null if none/not loaded/not in-game.
    fun getCurrentEvent(arenaName: String): String?

    // Id of the game event active in the match the player is in, or null if none.
    // Cheaper than getArenaOfPlayer(player)?.currentEvent for hot paths (e.g. damage
    // listeners of passive events).
    fun getCurrentEventOfPlayer(player: Player): String?

    // Adds a game event to the vote GUI and the pre-match selection. One plugin can
    // register any number of events. Returns false if the id is already taken;
    // throws IllegalArgumentException on a malformed or reserved id.
    // Events are unregistered automatically when their plugin is disabled.
    fun registerEvent(
        owner: Plugin,
        event: GameEventDefinition,
    ): Boolean

    // Removes a registered event: pending votes for it are dropped and, if it is
    // active in a running match, its handler is stopped. False if the id is unknown.
    fun unregisterEvent(eventId: String): Boolean

    // Unregisters every event the plugin registered. Returns how many were removed.
    fun unregisterEvents(owner: Plugin): Int

    // Snapshot copy, in registration order.
    fun getRegisteredEvents(): List<GameEventDefinition>

    fun getRegisteredEvent(eventId: String): GameEventDefinition?

    // Null if the player has no stored stats.
    fun getPlayerStatistics(playerName: String): CompletableFuture<PlayerStats?>

    // From the in-memory cache, no DB call. Null if not cached yet.
    fun getCachedPlayerStatistics(player: Player): PlayerStats?
}
