package org.gourmet.gourPillars.managers

import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.database.PlayerStats
import org.gourmet.gourPillars.other.Logger

object LevelBarManager {
    fun updateLevelInBar(player: Player) {
        if (!LevelManager.enabled) return

        object : BukkitRunnable() {
            override fun run() {
                try {
                    val stats: PlayerStats? = GourPillars.playersStats[player]
                    if (stats == null) {
                        Logger.warning("Can't fetch level for ${player.name}, not in cache")
                        return
                    }
                    player.level = stats.level
                } catch (e: Exception) {
                    Logger.warning("Error in updateLevelInBar: ${e.message}")
                }
            }
        }.runTaskLater(GourPillars.instance, 20L)
    }
}
