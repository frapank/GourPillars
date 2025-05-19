package org.gourmet.gourPillars.commands

import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import revxrsal.commands.annotation.Command

object StatsCMD {

    private val databaseManager = GourPillars.databaseManager

    @Command("stats")
    fun statsCommand(player: Player){
        val playerData = databaseManager.playersData.get(player)
        val kills = playerData?.stats?.kills ?: "error"
        val wins = playerData?.stats?.wins ?: "error"
        val defeats = playerData?.stats?.defeats ?: "error"
        val xp = playerData?.stats?.xp ?: "error"
        val level = playerData?.stats?.level ?: "error"

        player.sendDynamicMessage(MessageData.STATS_USER,
            "{player}" to player.name,
            "{defeats}" to defeats.toString(),
            "{kills}" to kills.toString(),
            "{wins}" to wins.toString(),
            "{xp}" to xp.toString(),
            "{level}" to level.toString()
        )
    }

    @Command("stats <target>")
    fun statsCommand(player: Player, target: Player){
        val playerData = databaseManager.playersData.get(player)
        val kills = playerData?.stats?.kills ?: "error"
        val wins = playerData?.stats?.wins ?: "error"
        val defeats = playerData?.stats?.defeats ?: "error"
        val xp = playerData?.stats?.xp ?: "error"
        val level = playerData?.stats?.level ?: "error"

        player.sendDynamicMessage(MessageData.STATS_TARGET,
            "{player}" to player.name,
            "{defeats}" to defeats.toString(),
            "{kills}" to kills.toString(),
            "{wins}" to wins.toString(),
            "{xp}" to xp.toString(),
            "{level}" to level.toString()
        )


    }
}