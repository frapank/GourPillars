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
import org.gourmet.gourPillars.other.toMini

class LeaveListener : Listener {

    private val arenaManager: ArenaManager = GourPillars.arenaManager
    private val partyManager = GourPillars.partyManager
    private val prefix = "<bold><aqua>Game </bold><gray>|"

    @EventHandler
    fun quitListener(event: PlayerQuitEvent){

        val player: Player = event.player
        val arena: Arena = arenaManager.getArenaByPlayer(player) ?: return
        val gameRunnable: GameTask = arena.gameTask

        if(partyManager.isInParty(player)) {
            partyManager.leaveParty(player)
        }

        arena.removePlayer(player)

        if(arena.gameState == State.INGAME) {
            if(partyManager.isInParty(player)) {
                partyManager.leaveParty(player)
                gameRunnable.playerEliminated(player)
            } else {
                gameRunnable.playerEliminated(player)
            }

            gameRunnable.alivePlayer.forEach{(member, _)->
                member.sendMessage("$prefix <green>${player.name} <yellow>e' uscito dal gioco".toMini())
            }

        }

    }
}
