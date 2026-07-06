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
        if (GourPillars.arenaManager.isPlayerInArena(player)) {
            player.sendMessage("<red>You must be in the lobby to use this command!".toMini())
            return
        }
        if (buildSessionPlayers.contains(player)) {
            buildSessionPlayers.remove(player)
            player.sendMessage("<red>You are no longer in a build session".toMini())
            Utils.giveLobbyItems(player)
        } else {
            buildSessionPlayers.add(player)
            player.sendMessage("<green>You entered a build session".toMini())
        }
    }
}