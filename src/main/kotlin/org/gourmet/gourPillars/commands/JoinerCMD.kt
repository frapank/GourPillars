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
    private val prefix = "<bold><aqua>Game </bold><gray>|"

    @Command("join <name>")
    fun joinCommand(player: Player, name: String){
        if(GourPillars.isEditing){
            player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_ARENA_EDIT)
            return
        }

        val arena: Arena = arenaManager.getArenaByName(name) ?: run{
            player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_ARENA_NOT_EXIST)
            return
        }

        //Can't join if you are not the party leader
        if(partyManager.isInParty(player) && !partyManager.isOwner(player)) {
            player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_USER_IN_PARTY)
            return
        }

        if(arena.gameState != State.INGAME || arena.gameState != State.STOPPED){
            //This will add all the player in the party
            if(partyManager.isInParty(player)){
                val party = partyManager.getPartyByPlayer(player)
                if(party?.partyAdmin == player){
                    party.members.forEach{ member ->
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
            when (currentArena.gameState){
                State.STOPPED -> {
                    player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_WAIT)
                    return
                }
                State.INGAME -> {
                    currentArena.gameTask.playerEliminated(player)
                    currentArena.removePlayer(player)
                }
                else -> {
                    player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_ALREADY_BEST_ARENA)
                    return
                }
            }
        }

        val arena = arenaManager.onlineArenas
            .filter { (_, arenaCurrent) ->
                arenaCurrent.gameState != State.INGAME && arenaCurrent.gameState != State.STOPPED &&
                        arenaCurrent.inGamePlayer.size < arenaCurrent.maxPlayer
            }
            .maxByOrNull { it.value.inGamePlayer.size }

        if (arena == null) {
            player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_ARENA_NOT_AVAILABLE)
            return
        }

        if(partyManager.isInParty(player) && !partyManager.isOwner(player)) {
            player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_USER_IN_PARTY)
            return
        }

        val selectedArena = arena.value

        if(partyManager.isInParty(player)){
            val party = partyManager.getPartyByPlayer(player)
            if(party?.partyAdmin == player){
                party.members.forEach{ member ->
                    selectedArena.addPlayer(member)
                }
            }
        } else {
            selectedArena.addPlayer(player)
        }

    }

    @Command("leave")
    fun leaveCommand(player: Player){

        val arena: Arena = arenaManager.getArenaByPlayer(player) ?: run {
            player.sendDynamicMessage(MessageData.JOIN_LEAVE_ERRORS_NOT_IN_ARENA)
            return
        }

        if(arena.gameState == State.INGAME) {

            if(partyManager.isInParty(player)){
                val party = partyManager.getPartyByPlayer(player)
                if(party?.partyAdmin == player){
                    party.members.forEach{ member ->
                        arena.gameTask.playerEliminated(member)
                        arena.spawnManager.teleportPlayerToSpawn(member)
                    }
                }
            } else {
                arena.gameTask.playerEliminated(player)
                arena.inGamePlayer.remove(player)
                arena.spawnManager.teleportPlayerToSpawn(player)
            }

        } else {

            if(partyManager.isInParty(player)){
                val party = partyManager.getPartyByPlayer(player)
                if(party?.partyAdmin == player){
                    party.members.forEach{ member ->
                        arena.removePlayer(member)
                        arena.spawnManager.teleportPlayerToSpawn(member)
                    }
                }
            } else {
                arena.removePlayer(player)
                arena.spawnManager.teleportPlayerToSpawn(player)
            }

        }

        player.inventory.clear()
        player.health = 20.0
        player.foodLevel = 20
        GourPillars.lobbyScoreboardManager.setScoreboard(player)
        Utils.giveLobbyItems(player)

    }


}