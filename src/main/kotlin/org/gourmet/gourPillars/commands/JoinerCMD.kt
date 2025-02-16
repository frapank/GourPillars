package org.gourmet.gourPillars.commands

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.arena.State
import org.gourmet.gourPillars.managers.arena.toMini
import revxrsal.commands.annotation.Command

object JoinerCMD {

    private val arenaManager = GourPillars.arenaManager
    private val partyManager = GourPillars.partyManager
    private val prefix = "<bold><aqua>Game </bold><gray>|"

    @Command("join <name>")
    fun joinCommand(player: Player, name: String){
        if(GourPillars.isEditing){
            player.sendMessage("$prefix <red>Un operatore sta modificando l'arena, non puoi giocare")
            return
        }
        val arena: Arena = arenaManager.getArenaByName(name) ?: run{
            player.sendMessage("$prefix <red>Arena non esistente".toMini())
            return
        }
        if(partyManager.isInParty(player) && !partyManager.isOwner(player)) {
            player.sendMessage("$prefix <red>Non puoi entrare in partita perchè sei in un party!".toMini())
            return
        }
        if(arena.gameState != State.INGAME || arena.gameState != State.STOPPED){
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

    @Command("spec <target>")
    fun spectate(player: Player, target: Player){
        val arena = arenaManager.getArenaByPlayer(target) ?: run{
            player.sendMessage("$prefix <green>${target.name} <yellow>non e' in game".toMini())
            return
        }
        if(arenaManager.isPlayerInArena(player)){
            player.sendMessage("$prefix <red>Sei gia in un game!".toMini())
            return
        }
        if(arena.gameState != State.INGAME){
            player.sendMessage("$prefix <green>Il game non e' ancora iniziato".toMini())
            return
        }
        arena.waitingPlayer.add(player)
        player.gameMode = GameMode.SPECTATOR
        player.teleport(arena.spawnMainLocation)
        player.sendMessage("$prefix <yellow>Sei entrato nel game di ${target.name}".toMini())

    }

    @Command("joinrandom")
    fun joinRandom(player: Player) {
        if (GourPillars.isEditing) {
            player.sendMessage("$prefix <red>Un operatore sta modificando l'arena, non puoi giocare".toMini())
            return
        }

        val currentArena = arenaManager.getArenaByPlayer(player)
        if (currentArena != null) {
            when (currentArena.gameState){
                State.STOPPED -> {
                    player.sendMessage("$prefix <red>Aspetta qualche secondo".toMini())
                    return
                }
                State.INGAME -> {
                    currentArena.gameTask.playerEliminated(player)
                    currentArena.removePlayer(player)
                }
                else -> {
                    player.sendMessage("$prefix <green>Sei gia nella arena migliore".toMini())
                    return
                }
            }
        }

        val arena = arenaManager.onlineArenas
            .filter { (key, arenaCurrent) ->
                arenaCurrent.gameState != State.INGAME && arenaCurrent.gameState != State.STOPPED &&
                        arenaCurrent.waitingPlayer.size < arenaCurrent.maxPlayer
            }
            .maxByOrNull { it.value.waitingPlayer.size }

        if (arena == null) {
            player.sendMessage("$prefix <red>Non ci sono arene disponibili per entrare.".toMini())
            return
        }

        if(partyManager.isInParty(player) && !partyManager.isOwner(player)) {
            player.sendMessage("$prefix <red>Non puoi entrare in partita perchè sei in un party!".toMini())
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
            player.sendMessage("$prefix <red>Non sei in nessuna arena".toMini())
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
                arena.waitingPlayer.remove(player)
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

    }


}