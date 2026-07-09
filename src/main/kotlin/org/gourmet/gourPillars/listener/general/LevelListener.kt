package org.gourmet.gourPillars.listener.general

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerExpChangeEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.database.PlayerStats
import org.gourmet.gourPillars.managers.LevelManager
import org.gourmet.gourPillars.other.Logger

class LevelListener : Listener {
    @EventHandler
    fun onXpGive(event: PlayerExpChangeEvent) {
        if (!LevelManager.enabled) return

        try {
            val player = event.player
            val stats: PlayerStats? = GourPillars.playersStats[player]
            if (stats == null) {
                Logger.warning("Can't fetch level, ${player.name} not in cache")
                return
            }
            event.amount = stats.level
        } catch (e: Exception) {
            Logger.warning("Error in onXpGive: ${e.message}")
        }
    }
}
