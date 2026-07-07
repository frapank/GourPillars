package org.gourmet.gourPillars.managers

import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.DatabaseManager.PlayerStats
import org.gourmet.gourPillars.other.Logger

object LevelBarManager {
    private val databaseManager = GourPillars.databaseManager

    fun updateLevelInBar(player: Player) {
        object : BukkitRunnable() {
            override fun run() {
                try {
                    val stats: PlayerStats? = databaseManager.playersStats[player]
                    if (stats == null) {
                        Logger.warning("Can't fetch level for \${player.name}, not in cache")
                        return
                    }
                    player.level = stats.level
                } catch (e: Exception) {
                    Logger.warning("Error in updateLevelInBar: \${e.message}")
                }
            }
        }.runTaskLaterAsynchronously(GourPillars.instance, 20L)
    }
}
