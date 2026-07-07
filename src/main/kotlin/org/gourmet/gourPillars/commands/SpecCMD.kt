package org.gourmet.gourPillars.commands

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import org.gourmet.gourPillars.other.toMini
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.annotation.CommandPermission

object SpecCMD {
    private val arenaManager = GourPillars.arenaManager

    @Command("spec <target>")
    @CommandPermission("gpillars.spectate")
    fun spectate(
        player: Player,
        target: String,
    ) {
        if (arenaManager.isPlayerInArena(player)) {
            player.sendDynamicMessage(MessageData.SPECTATE_ERRORS_ALREADY_IN_GAME)
            return
        }
        if (arenaManager.isSpectating(player)) {
            player.sendDynamicMessage(MessageData.SPECTATE_ERRORS_ALREADY_SPECTATING)
            return
        }
        if (BuildCMD.buildSessionPlayers.contains(player)) {
            player.sendMessage("<red>Leave your build session first".toMini())
            return
        }

        val targetPlayer = Bukkit.getPlayerExact(target)?.takeIf { it != player }
        val arenaViaPlayer = targetPlayer?.let { arenaManager.getArenaByPlayer(it) }
        val arena = arenaViaPlayer ?: arenaManager.getSpectatableArena(target)

        if (arena == null) {
            player.sendDynamicMessage(MessageData.SPECTATE_ERRORS_NOT_FOUND)
            return
        }
        if (arena.gameState != State.INGAME) {
            player.sendDynamicMessage(MessageData.SPECTATE_ERRORS_NOT_IN_GAME)
            return
        }

        arena.addSpectator(player)

        if (arenaViaPlayer != null && targetPlayer != null) {
            player.sendDynamicMessage(
                MessageData.SPECTATE_JOINED_PLAYER,
                "{player}" to targetPlayer.name,
                "{arena}" to arena.name,
            )
        } else {
            player.sendDynamicMessage(MessageData.SPECTATE_JOINED_ARENA, "{arena}" to arena.name)
        }
    }
}
