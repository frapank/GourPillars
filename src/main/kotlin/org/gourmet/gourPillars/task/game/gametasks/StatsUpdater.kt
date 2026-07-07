package org.gourmet.gourPillars.task.game.gametasks

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.database.PlayerStats
import org.gourmet.gourPillars.other.Logger
import java.util.concurrent.CompletableFuture

object StatsUpdater {
    private val database = GourPillars.database

    private fun apply(
        player: Player,
        compute: (PlayerStats) -> PlayerStats,
    ) {
        if (!database.isOnline) return

        database
            .getStatistics(player.name)
            .thenCompose { dbStats ->
                if (dbStats == null) {
                    Logger.warning("Can't fetch stats, ${player.name} is not in the database")
                    return@thenCompose CompletableFuture.completedFuture<Void?>(null)
                }
                val updated = compute(dbStats)
                database
                    .updateStatistics(
                        updated.name,
                        updated.kills,
                        updated.wins,
                        updated.xp,
                        updated.level,
                        updated.playedGame,
                        updated.bestWinStreak,
                        updated.currentWinStreak,
                    ).thenRun { cacheLocally(player, updated) }
            }.exceptionally { e ->
                Logger.warning("Unexpected error updating stats for ${player.name}: ${e.message}")
                null
            }
    }

    private fun cacheLocally(
        player: Player,
        updated: PlayerStats,
    ) {
        Bukkit.getScheduler().runTask(
            GourPillars.instance,
            Runnable {
                val localStats = GourPillars.playersStats[player]
                if (localStats == null) {
                    Logger.warning("Can't fetch local stats, ${player.name} is not in the local cache")
                    return@Runnable
                }
                localStats.kills = updated.kills
                localStats.wins = updated.wins
                localStats.xp = updated.xp
                localStats.level = updated.level
                localStats.playedGame = updated.playedGame
                localStats.bestWinStreak = updated.bestWinStreak
                localStats.currentWinStreak = updated.currentWinStreak
            },
        )
    }

    fun updateKill(player: Player) = apply(player) { it.copy(kills = it.kills + 1) }

    fun updateWins(player: Player) = apply(player) { it.copy(wins = it.wins + 1) }

    fun updateGamesPlayed(player: Player) = apply(player) { it.copy(playedGame = it.playedGame + 1) }

    fun incrementStreak(player: Player) =
        apply(player) {
            val newCurrent = it.currentWinStreak + 1
            it.copy(currentWinStreak = newCurrent, bestWinStreak = maxOf(newCurrent, it.bestWinStreak))
        }

    fun looseStreak(player: Player) =
        apply(player) {
            it.copy(bestWinStreak = maxOf(it.currentWinStreak, it.bestWinStreak), currentWinStreak = 0)
        }
}
