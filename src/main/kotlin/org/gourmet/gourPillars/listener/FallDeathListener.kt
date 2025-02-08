package org.gourmet.gourPillars.listener

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.State

class FallDeathListener : Listener{

    val arenaManager = GourPillars.arenaManager

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val playerLoc = player.y
        val arena = arenaManager.getArenaByPlayer(player) ?: return
        val gameState = arena.gameState
        if (gameState == State.INGAME) {
            if (playerLoc <= 80) {
                player.health = 0.0
                arena.gameTask.playerEliminated(player)
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity

        Bukkit.getScheduler().runTaskLater(GourPillars.instance, Runnable {
            player.spigot().respawn()
        }, 1L)
    }

}
