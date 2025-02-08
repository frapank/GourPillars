package org.gourmet.gourPillars.commands.set

import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission

@Command("arena")
@CommandPermission("gpillars.admim")
object ArenaCMD {

    private val arenaManager = GourPillars.arenaManager

    @Subcommand("set spawn <arena> <spawn>")
    fun setSpawns(player: Player, arenaName: String, number: Int){
    }

}