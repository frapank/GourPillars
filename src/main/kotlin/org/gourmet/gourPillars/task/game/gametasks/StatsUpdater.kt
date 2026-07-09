package org.gourmet.gourPillars.task.game.gametasks

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.database.PlayerStats
import org.gourmet.gourPillars.managers.LevelBarManager
import org.gourmet.gourPillars.managers.LevelManager
import org.gourmet.gourPillars.other.Logger
import java.util.concurrent.CompletableFuture

object StatsUpdater {
    private val database = GourPillars.database

    private fun apply(
        player: Player,
        dbCall: (String) -> CompletableFuture<Void?>,
        applyLocally: (PlayerStats) -> Unit,
    ) {
        if (!database.isOnline) return

        dbCall(player.name)
            .thenRun {
                Bukkit.getScheduler().runTask(
                    GourPillars.instance,
                    Runnable {
                        val localStats = GourPillars.playersStats[player]
                        if (localStats == null) {
                            Logger.warning("Can't fetch local stats, ${player.name} is not in the local cache")
                            return@Runnable
                        }
                        applyLocally(localStats)
                    },
                )
            }.exceptionally { e ->
                Logger.warning("Unexpected error updating stats for ${player.name}: ${e.message}")
                null
            }
    }

    fun updateKill(player: Player) = apply(player, database::incrementKills) { it.kills++ }

    fun updateWins(player: Player) = apply(player, database::incrementWins) { it.wins++ }

    fun updateGamesPlayed(player: Player) = apply(player, database::incrementGamesPlayed) { it.playedGame++ }

    fun incrementStreak(player: Player) =
        apply(player, database::incrementWinStreak) {
            it.currentWinStreak++
            it.bestWinStreak = maxOf(it.bestWinStreak, it.currentWinStreak)
        }

    fun looseStreak(player: Player) =
        apply(player, database::resetWinStreak) {
            it.bestWinStreak = maxOf(it.bestWinStreak, it.currentWinStreak)
            it.currentWinStreak = 0
        }

    fun addXp(
        player: Player,
        source: String,
    ) {
        if (!LevelManager.enabled) return
        val amount = LevelManager.xpFor(source)
        if (amount <= 0) return

        apply(player, { name -> database.incrementXp(name, amount) }) { stats ->
            stats.xp += amount
            val newLevel = LevelManager.levelForXp(stats.xp)
            if (newLevel > stats.level) {
                stats.level = newLevel
                database.setLevel(player.name, newLevel)
                LevelBarManager.updateLevelInBar(player)
                LevelManager.announceLevelUp(player, newLevel)
            }
        }
    }
}
