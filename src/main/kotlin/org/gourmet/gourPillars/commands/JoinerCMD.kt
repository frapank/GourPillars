package org.gourmet.gourPillars.commands

import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.arena.State
import org.gourmet.gourPillars.managers.toMini
import revxrsal.commands.annotation.Command

object JoinerCMD {

    private val arenaManager = GourPillars.arenaManager

    @Command("join <name>")
    fun joinCommand(player: Player, name: String){
        val arena: Arena = arenaManager.getArenaByName(name) ?: run{
            player.sendMessage("Arena non esistente".toMini())
            return
        }
        if(arena.gameState != State.INGAME){
            player.sendMessage("Arena gia in game".toMini())
            arena.addPlayer(player)
        }
    }

    @Command("leave")
    fun leaveCommand(player: Player){
        val arena: Arena = arenaManager.getArenaByPlayer(player) ?: run {
            player.sendMessage("Non sei in nessuna arena")
            return
        }

        arena.spawnManager.teleportPlayerToSpawn(player)
        arena.removePlayer(player)

    }

}