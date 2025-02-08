package org.gourmet.gourPillars.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.ArenaManager
import org.gourmet.gourPillars.task.GameTask
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.arena.State

class LeaveListener : Listener {
    private val arenaManager: ArenaManager = GourPillars.arenaManager

    @EventHandler
    fun quitListener(event: PlayerQuitEvent){
        val player: Player = event.player
        val arena: Arena = arenaManager.getArenaByPlayer(player) ?: return
        val gameRunnable: GameTask = arena.gameTask
        arena.removePlayer(player)
        if(arena.gameState == State.INGAME){
            gameRunnable.playerEliminated(player)
            gameRunnable.alivePlayer.forEach{(player, _)->
                player.sendMessage("${player.name} e' uscito dal gioco")
            }
        }
    }
}
