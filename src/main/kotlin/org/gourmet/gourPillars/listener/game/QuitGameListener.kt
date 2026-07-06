package org.gourmet.gourPillars.listener.game

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.ArenaManager
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import org.gourmet.gourPillars.task.game.gametasks.GameTask

class QuitGameListener : Listener {

    private val arenaManager: ArenaManager = GourPillars.arenaManager
    private val partyManager = GourPillars.partyManager

    @EventHandler
    fun quitListener(event: PlayerQuitEvent) {

        val player: Player = event.player
        val arena: Arena = arenaManager.getArenaByPlayer(player) ?: return
        val gameRunnable: GameTask = arena.gameTask

        if (partyManager.isInParty(player)) {
            partyManager.leaveParty(player)
        }

        arena.removePlayer(player)

        //If player quit during the game, he will die and leave the party
        if (arena.gameState == State.INGAME) {
            if (partyManager.isInParty(player)) {
                partyManager.leaveParty(player)
                gameRunnable.playerEliminated(player)
            } else {
                gameRunnable.playerEliminated(player)
            }

            arena.inGamePlayer.forEach {member ->
                member.sendDynamicMessage(MessageData.ARENA_PLAYER_LEFT, "{player}" to player.name)
            }

        }

    }
}