package org.gourmet.gourPillars.task.game.gametasks

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.data.PlayerData
import org.gourmet.gourPillars.listener.KnockBackEvent
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.arena.GameEvents
import org.gourmet.gourPillars.managers.arena.State
import org.gourmet.gourPillars.other.Utils
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import org.gourmet.gourPillars.task.game.GameFunctions
import org.gourmet.gourPillars.task.game.GameRandom
import kotlin.collections.forEach

class GameTask(private val arena: Arena, private val plugin: JavaPlugin): BukkitRunnable(){

    lateinit var alivePlayer: MutableMap<Player, Int>
    var running = false
    var secondsPassed = 300
    private var lastPlayer: Player? = null
    private val jsonManager = GourPillars.Companion.jsonManager
    private var lavaLevel = arena.minHeight
    private var currentEventHandler: GameHandler? = null
    private val prefix = "<bold><aqua>Game </bold><gray>|"

    override fun run(){
        running = true
        lavaLevel = arena.minHeight
        alivePlayer = mutableMapOf()
        setupEvent()
        removeAllGlass()
        preparePlayer()
        setTimeByVote()
        GameRandom.startRandomItemTask(alivePlayer, running)
        currentEventHandler?.onStart(arena)
        object : BukkitRunnable() {
            override fun run() {
                secondsPassed--
                if (!running) cancel()

                updateScoreBoard()

                if (alivePlayer.size <= 1 || secondsPassed == 0) {
                    handleEndGame()
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }


    //private fun setupEvent() {
    //    if (arena.borderEvent.isEmpty() && arena.lavaEvent.isEmpty()) return
    //    if (arena.borderEvent.size == arena.lavaEvent.size) return
    //    if(arena.noEventVote.size >= arena.borderEvent.size && arena.noEventVote.size >= arena.lavaEvent.size) return
    //
    //    arena.gameEvent = if (arena.borderEvent.size >= arena.lavaEvent.size) {
    //        GameEvents.KNOCKBACK
    //    } else if () {
    //        GameEvents.LAVA
    //    }
    //
    //    when (arena.gameEvent) {
    //        GameEvents.LAVA -> LavaHandler()
    //        GameEvents.BORDER -> BorderHandler()
    //        else -> null
    //    }
    //
    //}

    private fun setupEvent() {
        val voteCounts = mapOf(
            GameEvents.LAVA       to arena.lavaEvent.size,
            GameEvents.KNOCKBACK  to arena.knockbackVote.size,
            GameEvents.BORDER     to arena.borderEvent.size
        )

        val maxVotes = voteCounts.values.maxOrNull() ?: return

        if (maxVotes < 2) {
            return
        }

        val winners = voteCounts.filter { it.value == maxVotes }.keys
        if (winners.size > 1) {
            return
        }

        val winningEvent = winners.first()

        arena.gameEvent = winningEvent
        currentEventHandler = when (winningEvent) {
            GameEvents.LAVA -> LavaHandler()
            GameEvents.BORDER -> BorderHandler()
            GameEvents.KNOCKBACK -> null
        }

        arena.sendMessageToPlayerInGame("<gray>Evento: <yellow>$winningEvent")

    }

    private fun setTimeByVote() {
        val worldName = arena.spawnMap.keys.first().world.name
        val world = Bukkit.getWorld(worldName)
        if (world != null) {
            if (arena.nightVote.size <= arena.dayVote.size) {
                world.time = 6000
            } else {
                world.time = 18000
            }
        }
    }

    private fun updateScoreBoard(){
        arena.waitingPlayer.forEach{player ->
            arena.scoreboardManager.setGameScoreboard(player)
        }
    }

    private fun getWinner(): Player?{
        return when(alivePlayer.size){
            0 -> lastPlayer
            1 -> alivePlayer.keys.first()
            else -> alivePlayer.maxByOrNull { it.value }?.key
        }
    }

    private fun handleEndGame() {
        val winner = getWinner()
        if (winner != null) {
            arena.sendDynamicTitleToPlayerInGame(MessageData.ARENA_TITLE_END, MessageData.ARENA_SUBTITLE_END, "{winner}" to winner.name)
            arena.waitingPlayer.forEach { messagePlayer ->
                messagePlayer.sendDynamicMessage(MessageData.WIN_GAME, "{winner}" to winner.name)
            }
        }
        //Update the statistic only for the winner
        arena.nightVote.clear()
        arena.dayVote.clear()
        arena.knockbackVote.clear()
        arena.lavaEvent.clear()
        arena.noEventVote.clear()
        if (winner != null) {
            winner.isInvulnerable = true
            val winnerData = jsonManager.getPlayerData(winner) ?: PlayerData(winner.name, 0, 0, 0, 0, 0, 0, 0)
            winnerData.wins += 1
            winnerData.gamesPlayed += 1
            jsonManager.setPlayerData(winner, winnerData)
            jsonManager.savePlayerData()
            jsonManager.addXP(winner, 300)
            GameFunctions.playVictoryEffects(winner, arena)
        }

        currentEventHandler?.onStop(arena, winner)

        //Update the defeat statistic for all players
        arena.waitingPlayer.forEach { playersForData ->
            val playerData = GourPillars.Companion.jsonManager.getPlayerData(playersForData) ?: PlayerData(
                playersForData.name,
                0,
                0,
                0,
                0,
                0,
                0,
                0
            )
            playerData.defeats = playerData.gamesPlayed - playerData.wins
            GourPillars.Companion.jsonManager.setPlayerData(playersForData, playerData)
            GourPillars.Companion.jsonManager.savePlayerData()
        }
        //Arena reset
        object : BukkitRunnable(){
            override fun run(){
                running = false
                secondsPassed = 300
                arena.gameState = State.STOPPED
                arena.waitingPlayer.forEach { player ->
                    GourPillars.Companion.spawnManager.teleportPlayerToSpawn(player)
                    GourPillars.Companion.lobbyScoreboardManager.setScoreboard(player)
                    player.inventory.clear()
                    player.health = 20.0
                    player.foodLevel = 20
                }
                arena.spawnMap.forEach{ (location, _) ->
                    arena.spawnMap[location] = null
                }
                arena.waitingPlayer.clear()
                alivePlayer.clear()

                cancel()
                arena.resetArenaTask.run()
            }
        }.runTaskLater(plugin, 80L)
    }

    private fun removeAllGlass(){
        arena.spawnMap.forEach{ (location, _) ->
            Utils.setGlass(false, location)
        }
    }

    private fun preparePlayer(){
        val effect = PotionEffect(PotionEffectType.SLOW_FALLING, arena.slowFallingTime * 20, 0)
        arena.waitingPlayer.forEach { player: Player ->
            alivePlayer[player] = 0
        }
        alivePlayer.forEach{(player, _) ->
            player.isInvulnerable = false
            player.addPotionEffect(effect)
            player.inventory.clear()
            player.gameMode = GameMode.SURVIVAL
            player.health = 20.0
            player.closeInventory()
            arena.scoreboardManager.setGameScoreboard(player)
        }
    }

    private fun eliminationProcess(player: Player){
        val kills = alivePlayer[player]
        if(alivePlayer.size <= 1)
            lastPlayer = player
        alivePlayer.remove(player)
        player.gameMode = GameMode.SPECTATOR
        arena.reloadInGameScoreboard()
        player.teleport(arena.spawnMainLocation)
        arena.waitingPlayer.forEach { playerSound ->
            playerSound.playSound(playerSound.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f)
        }
        val playerData = GourPillars.Companion.jsonManager.getPlayerData(player) ?: PlayerData(
            player.name,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        )
        playerData.defeats += 1
        GourPillars.Companion.jsonManager.setPlayerData(player, playerData)
        GourPillars.Companion.jsonManager.savePlayerData()

        player.sendDynamicMessage(
            MessageData.END_GAME,
            "{time}" to getTimeFormatted(),
            "{kills}" to kills.toString(),
            "{map}" to arena.name)
    }

    fun playerEliminated(player: Player){
        eliminationProcess(player)
        arena.waitingPlayer.forEach { receiverPlayer ->
            if(receiverPlayer != player)
                receiverPlayer.sendDynamicMessage(MessageData.ARENA_PLAYER_ELIMINATED, "{player}" to player.name)
        }

    }

    //fall damage death message
    fun playerEliminatedFall(player: Player){
        eliminationProcess(player)
        arena.waitingPlayer.forEach { receiverPlayer ->
            if(receiverPlayer != player)
                receiverPlayer.sendDynamicMessage(MessageData.ARENA_PLAYER_ELIMINATED_FALL, "{player}" to player.name)
        }

    }

    //player kill by player death message
    fun playerEliminated(player: Player, killer: Player){
        if(alivePlayer.size <= 1)
            lastPlayer = player
        alivePlayer.remove(player)
        player.gameMode = GameMode.SPECTATOR
        arena.waitingPlayer.forEach { receiverPlayer ->
            if(receiverPlayer != player)
                receiverPlayer.sendDynamicMessage(
                    MessageData.ARENA_PLAYER_ELIMINATED_KILL,
                    "{player}" to player.name,
                    "{killer}" to killer.name)
        }
        if(alivePlayer.contains(killer)){
            val oldKills = alivePlayer[killer]!! + 1
            alivePlayer[killer] = oldKills
        }
        arena.reloadInGameScoreboard()

    }

    fun getTimeFormatted(): String{
        val minutes = secondsPassed / 60
        val remainingSeconds = secondsPassed % 60

        val minuteText = if (minutes == 1) "1 minuto" else "$minutes minuti"
        val secondText = if (remainingSeconds == 1) "1 secondo" else "$remainingSeconds secondi"

        return if (minutes > 0) "$minuteText $secondText" else secondText
    }


}