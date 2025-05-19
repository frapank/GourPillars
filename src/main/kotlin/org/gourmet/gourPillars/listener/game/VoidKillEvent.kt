package org.gourmet.gourPillars.listener.game

import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.State

class VoidKillEvent : Listener {

    val arenaManager = GourPillars.Companion.arenaManager

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val playerLoc = player.y
        val arena = arenaManager.getArenaByPlayer(player) ?: return
        val gameState = arena.gameState
        if (gameState == State.INGAME) {
            if (playerLoc <= arena.minHeight) {
                if(player.gameMode == GameMode.SURVIVAL){
                    arena.gameTask.playerEliminatedFall(player)
                }
            }
        }
    }

}