package org.gourmet.gourPillars.database

import java.util.concurrent.CompletableFuture

interface Database {
    val isOnline: Boolean
    val lastError: String?

    fun createUser(playerName: String): CompletableFuture<Void?>

    fun getStatistics(playerName: String): CompletableFuture<PlayerStats?>

    fun incrementKills(playerName: String): CompletableFuture<Void?>

    fun incrementWins(playerName: String): CompletableFuture<Void?>

    fun incrementGamesPlayed(playerName: String): CompletableFuture<Void?>

    /**
     * Atomically adds [amount] to the player's xp and recomputes their level from the resulting
     * total (never regressing it), all in a single statement so concurrent writers (including
     * other server instances sharing the same database) can't stomp on each other's level.
     */
    fun incrementXp(
        playerName: String,
        amount: Int,
        xpPerLevel: Int,
    ): CompletableFuture<Void?>

    fun incrementWinStreak(playerName: String): CompletableFuture<Void?>

    fun resetWinStreak(playerName: String): CompletableFuture<Void?>

    /** Releases the connection pool and background threads. Safe to call more than once. */
    fun close()
}

data class PlayerStats(
    val name: String,
    var kills: Int = 0,
    var wins: Int = 0,
    var xp: Int = 0,
    var level: Int = 0,
    var playedGame: Int = 0,
    var bestWinStreak: Int = 0,
    var currentWinStreak: Int = 0,
)
