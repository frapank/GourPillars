package org.gourmet.gourPillars.commands

import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.Utils
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import revxrsal.commands.annotation.Command

object JoinerCMD {
    private val arenaManager = GourPillars.arenaManager
    private val partyManager = GourPillars.partyManager

    @Command("join <name>")
    fun joinCommand(
        player: Player,
        name: String,
    ) {
        if (GourPillars.isEditing) {
            player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_ARENA_EDIT)
            return
        }

        val arena: Arena =
            arenaManager.getArenaByName(name) ?: run {
                player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_ARENA_NOT_EXIST)
                return
            }

        // Can't join if you are not the party leader
        if (partyManager.isInParty(player) && !partyManager.isOwner(player)) {
            player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_USER_IN_PARTY)
            return
        }

        if (arena.gameState != State.INGAME && arena.gameState != State.STOPPED) {
            // This will add all the player in the party
            if (partyManager.isInParty(player)) {
                val party = partyManager.getPartyByPlayer(player)
                if (party?.partyAdmin == player) {
                    if (!hasRoomForParty(arena, party.members.size)) {
                        sendPartyTooBigMessage(player, arena, party.members.size)
                        return
                    }
                    party.members.forEach { member ->
                        arena.addPlayer(member)
                    }
                }
            } else {
                arena.addPlayer(player)
            }
        }
    }

    @Command("joinrandom")
    fun joinRandom(player: Player) {
        if (GourPillars.isEditing) {
            player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_ARENA_EDIT)
            return
        }

        val currentArena = arenaManager.getArenaByPlayer(player)

        if (currentArena != null) {
            when (currentArena.gameState) {
                State.STOPPED -> {
                    player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_WAIT)
                    return
                }

                State.INGAME -> {
                    if (currentArena.gameTask.isAlive(player)) {
                        currentArena.gameTask.playerEliminated(player)
                    }
                    currentArena.removePlayer(player)
                }

                else -> {
                    player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_ALREADY_BEST_ARENA)
                    return
                }
            }
        }

        val arena =
            arenaManager.onlineArenas
                .filter { (_, arenaCurrent) ->
                    arenaCurrent.gameState != State.INGAME && arenaCurrent.gameState != State.STOPPED &&
                        arenaCurrent.inGamePlayer.size < arenaCurrent.maxPlayer
                }.maxByOrNull { it.value.inGamePlayer.size }

        if (arena == null) {
            player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_ARENA_NOT_AVAILABLE)
            return
        }

        if (partyManager.isInParty(player) && !partyManager.isOwner(player)) {
            player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_USER_IN_PARTY)
            return
        }

        val selectedArena = arena.value

        if (partyManager.isInParty(player)) {
            val party = partyManager.getPartyByPlayer(player)
            if (party?.partyAdmin == player) {
                if (!hasRoomForParty(selectedArena, party.members.size)) {
                    sendPartyTooBigMessage(player, selectedArena, party.members.size)
                    return
                }
                party.members.forEach { member ->
                    selectedArena.addPlayer(member)
                }
            }
        } else {
            selectedArena.addPlayer(player)
        }
    }

    @Command("leave")
    fun leaveCommand(player: Player) {
        val spectatingArena = arenaManager.getArenaBySpectator(player)
        if (spectatingArena != null) {
            spectatingArena.removeSpectator(player)
            player.sendDynamicMessage(MessageData.SPECTATE_LEFT)
            return
        }

        val arena: Arena =
            arenaManager.getArenaByPlayer(player) ?: run {
                player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_NOT_IN_ARENA)
                return
            }

        // The party admin takes the whole party out; everyone else only leaves for themselves.
        val party = partyManager.getPartyByPlayer(player)
        val leavingPlayers = if (party?.partyAdmin == player) party.members.toList() else listOf(player)

        leavingPlayers.forEach { member ->
            if (!arena.containPlayer(member)) return@forEach

            if (arena.gameState == State.INGAME) {
                if (arena.gameTask.isAlive(member)) {
                    arena.gameTask.playerEliminated(member)
                }
                arena.inGamePlayer.remove(member)
            } else {
                arena.removePlayer(member)
            }

            arena.spawnManager.teleportPlayerToSpawn(member)
            member.inventory.clear()
            Utils.resetPlayerState(member)
            GourPillars.lobbyScoreboardManager.setScoreboard(member)
            Utils.giveLobbyItems(member)
        }
    }

    private fun hasRoomForParty(
        arena: Arena,
        partySize: Int,
    ): Boolean = partySize <= arena.maxPlayer - arena.inGamePlayer.size

    private fun sendPartyTooBigMessage(
        player: Player,
        arena: Arena,
        partySize: Int,
    ) {
        val available = arena.maxPlayer - arena.inGamePlayer.size
        player.sendDynamicMessage(
            MessageData.JOIN_LEAVE_ERRORS_PARTY_TOO_BIG_FOR_ARENA,
            "{size}" to partySize.toString(),
            "{available}" to available.toString(),
        )
    }
}
