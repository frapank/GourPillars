package org.gourmet.gourPillars.commands

import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.annotation.CommandPermission

object StatsCMD {

    private val databaseManager = GourPillars.databaseManager

    @Command("stats")
    fun statsCommand(player: Player){
        val playerData = databaseManager.playersData.get(player)
        val kills = playerData?.stats?.kills ?: 0
        val wins = playerData?.stats?.wins ?: 0
        val xp = playerData?.stats?.xp ?: 0
        val level = playerData?.stats?.level ?: 0
        val winStreak = playerData?.stats?.currentWinStreak ?: 0
        val bestWinStreak = playerData?.stats?.bestWinStreak ?: 0
        val gamePlayed = playerData?.stats?.playedGame ?: 0
        val defeats = gamePlayed - wins

        player.sendDynamicMessage(MessageData.STATS_USER,
            "{player}" to player.name,
            "{defeats}" to defeats.toString(),
            "{kills}" to kills.toString(),
            "{wins}" to wins.toString(),
            "{xp}" to xp.toString(),
            "{level}" to level.toString(),
            "{winStreak}" to winStreak.toString(),
            "{bestWinStreak}" to bestWinStreak.toString(),
            "{gamesPlayed}" to gamePlayed.toString()
        )
    }

    @Command("stats <target>")
    @CommandPermission("gpillars.stats.other")
    fun statsCommand(player: Player, target: Player){
        val playerData = databaseManager.playersData.get(target)
        val kills = playerData?.stats?.kills ?: 0
        val wins = playerData?.stats?.wins ?: 0
        val xp = playerData?.stats?.xp ?: 0
        val level = playerData?.stats?.level ?: 0
        val winStreak = playerData?.stats?.currentWinStreak ?: 0
        val bestWinStreak = playerData?.stats?.bestWinStreak ?: 0
        val gamePlayed = playerData?.stats?.playedGame ?: 0
        val defeats = gamePlayed - wins

        player.sendDynamicMessage(MessageData.STATS_TARGET,
            "{player}" to target.name,
            "{defeats}" to defeats.toString(),
            "{kills}" to kills.toString(),
            "{wins}" to wins.toString(),
            "{xp}" to xp.toString(),
            "{level}" to level.toString(),
            "{winStreak}" to winStreak.toString(),
            "{bestWinStreak}" to bestWinStreak.toString(),
            "{gamesPlayed}" to gamePlayed.toString()
        )


    }
}