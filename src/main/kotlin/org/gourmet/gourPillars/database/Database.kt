package org.gourmet.gourPillars.database

import java.util.concurrent.CompletableFuture

interface Database {
    val isOnline: Boolean
    val lastError: String?

    fun createUser(playerName: String): CompletableFuture<Void?>

    fun updateStatistics(
        playerName: String,
        kills: Int,
        wins: Int,
        xp: Int,
        level: Int,
        playedGame: Int,
        bestWinStreak: Int,
        currentWinStreak: Int,
    ): CompletableFuture<Void?>

    fun getStatistics(playerName: String): CompletableFuture<PlayerStats?>

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
