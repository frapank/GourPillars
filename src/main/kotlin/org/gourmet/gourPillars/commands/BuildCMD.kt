package org.gourmet.gourPillars.commands

import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Utils
import org.gourmet.gourPillars.other.toMini
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.annotation.CommandPermission

object BuildCMD {

    val buildSessionPlayers: MutableSet<Player> = mutableSetOf()

    @Command("build")
    @CommandPermission("gpillars.build")
    fun buildCommand(player: Player) {
        if(GourPillars.arenaManager.isPlayerInArena(player)) {
            player.sendMessage("<red>Devi essere in lobby per usare il comando!".toMini())
            return
        }
        if(buildSessionPlayers.contains(player)) {
            buildSessionPlayers.remove(player)
            player.sendMessage("<red>Non sei più in sessione build".toMini())
            Utils.giveLobbyItems(player)
        } else {
            buildSessionPlayers.add(player)
            player.sendMessage("<green>Sei entrato in sessione build".toMini())
        }
    }
}