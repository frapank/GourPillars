package org.gourmet.gourPillars.commands

import org.bukkit.entity.Player
import org.gourmet.gourPillars.managers.ZipManager
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission

@Command("test")
@CommandPermission("gpillars.admin")
object TestCMD {
    private val zipManager = ZipManager()

    @Subcommand("backup")
    fun backupCommand(player: Player) {
        zipManager.saveBackup(player.location.world.name)
        player.sendMessage("done")
    }
}
