package org.gourmet.gourPillars.managers.arena

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.GameScoreboardManager
import org.gourmet.gourPillars.task.GameTask
import org.gourmet.gourPillars.task.ResetArenaTask
import org.gourmet.gourPillars.task.CountDownTask

class Arena(
    val spawnMap: MutableMap<Location, Player?>,
    private val maxPlayer: Int,
    val minPlayer: Int,
    val name: String)
{

    val scoreboardManager: GameScoreboardManager = GameScoreboardManager(this)
    val gameTask: GameTask = GameTask(this, GourPillars.instance)
    val resetArenaTask = ResetArenaTask(this)
    val waitingPlayer: MutableSet<Player> = mutableSetOf()
    var gameState: State = State.WAITING
    val spawnManager = GourPillars.spawnManager

    private fun startArena(){
        CountDownTask(this).runTaskTimer(GourPillars.instance, 0L, 20L)
    }


    /* Utils */
    fun addPlayer(player: Player){
        if(waitingPlayer.contains(player)){
            player.sendMessage("<red>Sei gia in questa arena".toMini())
            return
        }

        if(gameState == State.INGAME || gameState == State.STOPPED){
            player.sendMessage("<red>Arena non pronta".toMini())
            return
        }

        if(waitingPlayer.size < maxPlayer){
            waitingPlayer.add(player)
            scoreboardManager.setWaitingBoard(player)
            player.sendMessage("<green>Sei entrato in game".toMini())
            reloadWaitingScoreboard()
            player.inventory.clear()
            player.health = 20.0
            player.foodLevel = 20
            sendMessageToPlayerInGame("<yellow>${player.name} mancano (<green>${waitingPlayer.size}<white>/<green>$minPlayer<yellow>)")
            for ((location, playerInSpawn) in spawnMap) {
                if (playerInSpawn == null) {
                    Utils.setGlass(true, location)
                    player.gameMode = GameMode.SURVIVAL
                    Bukkit.getLogger().warning("$location >>>>>> ${location.world}")
                    player.teleport(location)
                    spawnMap[location] = player
                    break
                }
            }

            if(waitingPlayer.size >= minPlayer && gameState == State.WAITING){
                startArena()
                this.gameState = State.STARTING
            }
            return
        } else {
            player.sendMessage("<red>Partita piena!".toMini())
            return
        }
    }

    fun removePlayer(player: Player){
        spawnMap.forEach{ (location, playerInSpawn) ->
            if(playerInSpawn == player){
                Utils.setGlass(false, location)
                spawnMap[location] = null
            }
        }
        waitingPlayer.remove(player)
        player.sendMessage("<red>Sei uscito da questa arena".toMini())
        reloadWaitingScoreboard()
        if(waitingPlayer.size < minPlayer){
            gameState = State.WAITING
            val playerRequired = maxPlayer - waitingPlayer.size
            sendMessageToPlayerInGame("<green>Mancano <yellow>$playerRequired<green> player per cominciare")
            return
        }
    }

    fun reloadWaitingScoreboard(){
        waitingPlayer.forEach { player ->
            scoreboardManager.setWaitingBoard(player)
        }
    }

    fun reloadInGameScoreboard(){
        waitingPlayer.forEach { player ->
            scoreboardManager.setGameScoreboard(player)
        }
    }

    /* Getter and Settere */
    fun containPlayer(player: Player): Boolean {
        return waitingPlayer.contains(player)
    }

    fun sendMessageToPlayerInGame(message: String){
        waitingPlayer.forEach{ player: Player ->  player.sendMessage(message.toMini())}
    }
    fun sendTitleToPlayerInGame(title: String, subtitle: String){
        waitingPlayer.forEach{ player: Player ->
            player.sendTitle(title.replace("&", "§"), subtitle.replace("&", "§"))
        }

    }



}