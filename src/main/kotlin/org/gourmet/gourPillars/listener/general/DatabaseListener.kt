package org.gourmet.gourPillars.listener.general

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.database.PlayerStats
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.toMini

class DatabaseListener : Listener {
    private val database = GourPillars.database

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onJoin(event: PlayerJoinEvent) {
        val player: Player = event.player

        if (!database.isOnline) {
            warnAdmin(player)
            return
        }

        database
            .createUser(player.name)
            .thenCompose { database.getStatistics(player.name) }
            .thenAccept { stats -> cachePlayerStats(player, stats) }
            .exceptionally { e ->
                Logger.warning("Unexpected error loading stats for ${player.name}: ${e.message}")
                null
            }
    }

    private fun cachePlayerStats(
        player: Player,
        stats: PlayerStats?,
    ) {
        Bukkit.getScheduler().runTask(
            GourPillars.instance,
            Runnable {
                // The player may have already disconnected while the lookup was in flight.
                if (!player.isOnline) return@Runnable
                GourPillars.playersStats[player] = stats ?: PlayerStats(name = player.name)
            },
        )
    }

    @EventHandler
    fun onLeave(event: PlayerQuitEvent) {
        val player: Player = event.player
        GourPillars.playersStats.remove(player)
    }

    private fun warnAdmin(player: Player) {
        if (!player.isOp && !player.hasPermission("gpillars.admin")) return

        val reason = database.lastError ?: "unknown reason"
        player.sendMessage(
            "<red>[GourPillars] Database unreachable ($reason). Player statistics are disabled.</red>".toMini(),
        )
    }
}
