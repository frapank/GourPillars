package org.gourmet.gourPillars.commands

import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.toMini
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.annotation.CommandPermission

object SetSpawnCMD {
    @Command("setspawn")
    @CommandPermission("gpillars.admin")
    fun setSpawn(player: Player) {
        GourPillars.spawnManager.setSpawn(player.location)
        player.sendMessage("<green>Lobby spawn set!".toMini())
    }
}
