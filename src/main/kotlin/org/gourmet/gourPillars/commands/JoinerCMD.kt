package org.gourmet.gourPillars.commands

import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.arena.State
import org.gourmet.gourPillars.managers.arena.toMini
import revxrsal.commands.annotation.Command

object JoinerCMD {

    private val arenaManager = GourPillars.arenaManager

    @Command("join <name>")
    fun joinCommand(player: Player, name: String){
        if(GourPillars.isEditing){
            player.sendMessage("<red>Un operatore sta modificando l'arena, non puoi giocare")
            return
        }
        val arena: Arena = arenaManager.getArenaByName(name) ?: run{
            player.sendMessage("<red>Arena non esistente".toMini())
            return
        }
        if(arena.gameState != State.INGAME){
            arena.addPlayer(player)
        }
    }

    @Command("leave")
    fun leaveCommand(player: Player){
        val arena: Arena = arenaManager.getArenaByPlayer(player) ?: run {
            player.sendMessage("<red>Non sei in nessuna arena")
            return
        }
        arena.spawnManager.teleportPlayerToSpawn(player)
        if(arena.gameState == State.INGAME){
            arena.gameTask.playerEliminated(player)
        }

        arena.removePlayer(player)
        GourPillars.lobbyScoreboardManager.setScoreboard(player)

    }

}