package org.gourmet.gourPillars.listener.general

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerExpChangeEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.DatabaseManager.PlayerStats
import org.gourmet.gourPillars.other.Logger

class LevelListener : Listener {
    private val databaseManager = GourPillars.databaseManager

    @EventHandler
    fun onXpGive(event: PlayerExpChangeEvent) {
        try {
            val player = event.player
            val stats: PlayerStats? = databaseManager.playersStats[player]
            if (stats == null) {
                Logger.warning("Can't fetch level, \${player.name} not in cache")
                return
            }
            event.amount = stats.level
        } catch (e: Exception) {
            Logger.warning("Error in onXpGive: \${e.message}")
        }
    }
}
