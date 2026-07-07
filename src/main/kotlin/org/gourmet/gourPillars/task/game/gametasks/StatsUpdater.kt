package org.gourmet.gourPillars.task.game.gametasks

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.DatabaseManager.PlayerStats
import org.gourmet.gourPillars.other.Logger

object StatsUpdater {
    private val databaseManager = GourPillars.databaseManager

    private fun fetchDatabaseStats(playerName: String): Pair<PlayerStats?, Boolean> {
        if (!databaseManager.isOnline) return Pair(null, false)
        val stats = databaseManager.getStatistics(playerName)
        if (stats == null) {
            Logger.warning("Can't fetch stats, $playerName is not in the database")
            return Pair(null, false)
        }
        return Pair(stats, true)
    }

    private fun fetchLocalStats(player: Player): Pair<PlayerStats?, Boolean> {
        val localStats = databaseManager.playersStats[player]
        if (localStats == null) {
            Logger.warning("Can't fetch local stats, ${player.name} is not in the local cache")
            return Pair(null, false)
        }
        return Pair(localStats, true)
    }

    fun updateKill(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(
            GourPillars.instance,
            Runnable {
                val (dbStats, dbOk) = fetchDatabaseStats(player.name)
                if (!dbOk || dbStats == null) return@Runnable

                // Update the kill count in the database
                databaseManager.updateStatistics(
                    dbStats.name,
                    dbStats.kills + 1,
                    dbStats.wins,
                    dbStats.xp,
                    dbStats.level,
                    dbStats.playedGame,
                    dbStats.bestWinStreak,
                    dbStats.currentWinStreak,
                )

                // Update the kill count in the local cache
                val (localStats, localOk) = fetchLocalStats(player)
                if (!localOk || localStats == null) return@Runnable
                localStats.kills = localStats.kills + 1
            },
        )
    }

    fun updateWins(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(
            GourPillars.instance,
            Runnable {
                val (dbStats, dbOk) = fetchDatabaseStats(player.name)
                if (!dbOk || dbStats == null) return@Runnable

                // Update the win count in the database
                databaseManager.updateStatistics(
                    dbStats.name,
                    dbStats.kills,
                    dbStats.wins + 1,
                    dbStats.xp,
                    dbStats.level,
                    dbStats.playedGame,
                    dbStats.bestWinStreak,
                    dbStats.currentWinStreak,
                )

                // Update the win count in the local cache
                val (localStats, localOk) = fetchLocalStats(player)
                if (!localOk || localStats == null) return@Runnable
                localStats.wins = localStats.wins + 1
            },
        )
    }

    fun updateGamesPlayed(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(
            GourPillars.instance,
            Runnable {
                val (dbStats, dbOk) = fetchDatabaseStats(player.name)
                if (!dbOk || dbStats == null) return@Runnable

                // Increment playedGame in the database
                databaseManager.updateStatistics(
                    dbStats.name,
                    dbStats.kills,
                    dbStats.wins,
                    dbStats.xp,
                    dbStats.level,
                    dbStats.playedGame + 1,
                    dbStats.bestWinStreak,
                    dbStats.currentWinStreak,
                )

                // Increment playedGame in the local cache
                val (localStats, localOk) = fetchLocalStats(player)
                if (!localOk || localStats == null) return@Runnable
                localStats.playedGame = localStats.playedGame + 1
            },
        )
    }

    fun incrementStreak(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(
            GourPillars.instance,
            Runnable {
                val (dbStats, dbOk) = fetchDatabaseStats(player.name)
                if (!dbOk || dbStats == null) return@Runnable

                val newCurrent = dbStats.currentWinStreak + 1
                val newBest = maxOf(newCurrent, dbStats.bestWinStreak)

                // Update the streak in the database
                databaseManager.updateStatistics(
                    dbStats.name,
                    dbStats.kills,
                    dbStats.wins,
                    dbStats.xp,
                    dbStats.level,
                    dbStats.playedGame,
                    newBest,
                    newCurrent,
                )

                // Update the streak in the local cache
                val (localStats, localOk) = fetchLocalStats(player)
                if (!localOk || localStats == null) return@Runnable
                localStats.currentWinStreak = newCurrent
                localStats.bestWinStreak = newBest
            },
        )
    }

    fun looseStreak(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(
            GourPillars.instance,
            Runnable {
                val (dbStats, dbOk) = fetchDatabaseStats(player.name)
                if (!dbOk || dbStats == null) return@Runnable

                val newBest = maxOf(dbStats.currentWinStreak, dbStats.bestWinStreak)
                val newCurrent = 0

                // Update the streak in the database
                databaseManager.updateStatistics(
                    dbStats.name,
                    dbStats.kills,
                    dbStats.wins,
                    dbStats.xp,
                    dbStats.level,
                    dbStats.playedGame,
                    newBest,
                    newCurrent,
                )

                // Update the streak in the local cache
                val (localStats, localOk) = fetchLocalStats(player)
                if (!localOk || localStats == null) return@Runnable
                localStats.bestWinStreak = newBest
                localStats.currentWinStreak = newCurrent
            },
        )
    }
}
