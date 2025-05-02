package org.gourmet.gourPillars.commands

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.LevelBarManager
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import org.gourmet.gourPillars.other.toMini
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.annotation.CommandPermission

object StatsCMD {

    private val jsonManager = GourPillars.jsonManager

    @Command("stats")
    fun statsCommand(player: Player){
        val playerData = jsonManager.getPlayerData(player)
        val kills = playerData?.kills ?: "error"
        val deaths = playerData?.deaths ?: "error"
        val wins = playerData?.wins ?: "error"
        val defeats = playerData?.defeats ?: "error"
        val gamesPlayed = playerData?.gamesPlayed ?: "error"
        val xp = playerData?.xp ?: "error"
        val level = playerData?.level ?: "error"
        val kd = jsonManager.getPlayerKD(player)

        player.sendDynamicMessage(MessageData.STATS_USER,
            "{player}" to player.name,
            "{deaths}" to deaths.toString(),
            "{defeats}" to defeats.toString(),
            "{kills}" to kills.toString(),
            "{wins}" to wins.toString(),
            "{gamesPlayed}" to gamesPlayed.toString(),
            "{kd}" to kd.toString(),
            "{xp}" to xp.toString(),
            "{level}" to level.toString()
        )
    }

    @Command("stats <target>")
    fun statsCommand(player: Player, target: Player){
        val playerData = jsonManager.getPlayerData(target)
        val kills = playerData?.kills ?: "error"
        val deaths = playerData?.deaths ?: "error"
        val wins = playerData?.wins ?: "error"
        val defeats = playerData?.defeats ?: "error"
        val gamesPlayed = playerData?.gamesPlayed ?: "error"
        val xp = playerData?.xp ?: "error"
        val level = playerData?.level ?: "error"
        val kd = jsonManager.getPlayerKD(target)

        player.sendDynamicMessage(MessageData.STATS_TARGET,
            "{player}" to player.name,
            "{deaths}" to deaths.toString(),
            "{defeats}" to defeats.toString(),
            "{kills}" to kills.toString(),
            "{wins}" to wins.toString(),
            "{gamesPlayed}" to gamesPlayed.toString(),
            "{kd}" to kd.toString(),
            "{xp}" to xp.toString(),
            "{level}" to level.toString()
        )


    }

    @Command("levelset <level>")
    @CommandPermission("pillars.admin")
    fun levelSet(player: Player, level: Int){
        jsonManager.addXP(player, level)
        LevelBarManager.updateLevelInBar(player)
    }

}