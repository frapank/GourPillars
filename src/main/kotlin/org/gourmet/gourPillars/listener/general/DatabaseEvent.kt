package org.gourmet.gourPillars.listener.general

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.DatabaseManager.PlayerStats
import org.gourmet.gourPillars.other.toMini

class DatabaseEvent : Listener {

    private val databaseManager = GourPillars.databaseManager

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onJoin(event: PlayerJoinEvent) {
        val player: Player = event.player

        if (!databaseManager.isOnline) {
            warnAdmin(player)
            return
        }

        databaseManager.createUser(player)
        val stats: PlayerStats? = databaseManager.getStatistics(player.name)
        if (stats != null) {
            databaseManager.playersStats[player] = stats
        } else {
            databaseManager.playersStats[player] = PlayerStats(
                name = player.name
            )
        }
    }

    @EventHandler
    fun onLeave(event: PlayerQuitEvent) {
        val player: Player = event.player
        if (!databaseManager.isOnline) return

        databaseManager.playersStats.remove(player)
    }

    private fun warnAdmin(player: Player) {
        if (!player.isOp && !player.hasPermission("gpillars.admin")) return

        val reason = databaseManager.lastError ?: "motivo sconosciuto"
        player.sendMessage(
            "<red>[GourPillars] Database non raggiungibile ($reason). Le statistiche dei giocatori sono disabilitate.</red>".toMini()
        )
    }
}
