package org.gourmet.gourPillars.task.game.gametasks

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger

object StatsUpdater {

    private val databaseManager = GourPillars.databaseManager

    private fun fetchDatabaseStats(playerName: String): Pair<org.gourmet.gourPillars.managers.DatabaseManager.PlayerStats?, Boolean> {
        if (!databaseManager.isOnline) return Pair(null, false)
        val stats = databaseManager.getStatistics(playerName)
        if (stats == null) {
            Logger.warning("Can't fetch stats, $playerName is not in the database")
            return Pair(null, false)
        }
        return Pair(stats, true)
    }

    private fun fetchLocalStats(player: Player): Pair<org.gourmet.gourPillars.managers.DatabaseManager.PlayerStats?, Boolean> {
        val localStats = databaseManager.playersData[player]?.stats
        if (localStats == null) {
            Logger.warning("Can't fetch local stats, ${player.name} is not in the local database")
            return Pair(null, false)
        }
        return Pair(localStats, true)
    }

    fun updateKill(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(GourPillars.instance, Runnable {
            val (dbStats, dbOk) = fetchDatabaseStats(player.name)
            if (!dbOk || dbStats == null) return@Runnable

            // Aggiorna il kill count in database
            databaseManager.updateStatistics(
                dbStats.name,
                dbStats.kills + 1,
                dbStats.wins,
                dbStats.xp,
                dbStats.level,
                dbStats.playedGame,
                dbStats.bestWinStreak,
                dbStats.currentWinStreak
            )

            // Aggiorna il kill count in locale
            val (localStats, localOk) = fetchLocalStats(player)
            if (!localOk || localStats == null) return@Runnable
            localStats.kills++
        })
    }


    fun updateWins(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(GourPillars.instance, Runnable {
            val (dbStats, dbOk) = fetchDatabaseStats(player.name)
            if (!dbOk || dbStats == null) return@Runnable

            // Aggiorna il win count in database
            databaseManager.updateStatistics(
                dbStats.name,
                dbStats.kills,
                dbStats.wins + 1,
                dbStats.xp,
                dbStats.level,
                dbStats.playedGame,
                dbStats.bestWinStreak,
                dbStats.currentWinStreak
            )

            // Aggiorna il win count in locale
            val (localStats, localOk) = fetchLocalStats(player)
            if (!localOk || localStats == null) return@Runnable
            localStats.wins++
        })
    }

    fun updateGamesPlayed(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(GourPillars.instance, Runnable {
            val (dbStats, dbOk) = fetchDatabaseStats(player.name)
            if (!dbOk || dbStats == null) return@Runnable

            // Incrementa playedGame in database
            databaseManager.updateStatistics(
                dbStats.name,
                dbStats.kills,
                dbStats.wins,
                dbStats.xp,
                dbStats.level,
                dbStats.playedGame + 1,
                dbStats.bestWinStreak,
                dbStats.currentWinStreak
            )

            // Incrementa playedGame in locale
            val (localStats, localOk) = fetchLocalStats(player)
            if (!localOk || localStats == null) return@Runnable
            localStats.playedGame++
        })
    }

    fun incrementStreak(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(GourPillars.instance, Runnable {
            val (dbStats, dbOk) = fetchDatabaseStats(player.name)
            if (!dbOk || dbStats == null) return@Runnable

            val newCurrent = dbStats.currentWinStreak + 1
            val newBest = if (newCurrent > dbStats.bestWinStreak) newCurrent else dbStats.bestWinStreak

            // Aggiorna streak in database
            databaseManager.updateStatistics(
                dbStats.name,
                dbStats.kills,
                dbStats.wins,
                dbStats.xp,
                dbStats.level,
                dbStats.playedGame,
                newBest,
                newCurrent
            )

            // Aggiorna streak in locale
            val (localStats, localOk) = fetchLocalStats(player)
            if (!localOk || localStats == null) return@Runnable
            localStats.currentWinStreak = newCurrent
            localStats.bestWinStreak = newBest
        })
    }

    fun looseStreak(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(GourPillars.instance, Runnable {
            val (dbStats, dbOk) = fetchDatabaseStats(player.name)
            if (!dbOk || dbStats == null) return@Runnable

            val newBest =
                if (dbStats.currentWinStreak > dbStats.bestWinStreak) dbStats.currentWinStreak else dbStats.bestWinStreak
            val newCurrent = 0

            // Aggiorna streak in database
            databaseManager.updateStatistics(
                dbStats.name,
                dbStats.kills,
                dbStats.wins,
                dbStats.xp,
                dbStats.level,
                dbStats.playedGame,
                newBest,
                newCurrent
            )

            // Aggiorna streak in locale
            val (localStats, localOk) = fetchLocalStats(player)
            if (!localOk || localStats == null) return@Runnable
            localStats.bestWinStreak = newBest
            localStats.currentWinStreak = newCurrent
        })
    }
}
