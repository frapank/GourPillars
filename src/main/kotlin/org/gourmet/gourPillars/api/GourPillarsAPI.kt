package org.gourmet.gourPillars.api

import org.bukkit.Location
import org.bukkit.entity.Player
import org.gourmet.gourPillars.database.PlayerStats
import org.gourmet.gourPillars.managers.game.arena.GameEvents
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

    fun getCurrentEvent(arenaName: String): GameEvents?

    // Null if the player has no stored stats.
    fun getPlayerStatistics(playerName: String): CompletableFuture<PlayerStats?>

    // From the in-memory cache, no DB call. Null if not cached yet.
    fun getCachedPlayerStatistics(player: Player): PlayerStats?
}
