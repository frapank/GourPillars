package org.gourmet.gourPillars.managers.arena

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.GameScoreboardManager
import org.gourmet.gourPillars.other.Region
import org.gourmet.gourPillars.other.Utils
import org.gourmet.gourPillars.other.messages.DynamicMessage
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import org.gourmet.gourPillars.other.messages.sendDynamicTitle
import org.gourmet.gourPillars.other.toMini
import org.gourmet.gourPillars.task.GameTask
import org.gourmet.gourPillars.task.ResetArenaTask
import org.gourmet.gourPillars.task.CountDownTask

class Arena(
    val spawnMap: MutableMap<Location, Player?>,
    val spawnMainLocation: Location,
    val slowFallingTime: Int,
    val maxPlayer: Int,
    val minPlayer: Int,
    val maxHeight: Int,
    val minHeight: Int,
    val regionLocOne: Location,
    val regionLocTwo: Location,
    var region: Region,
    val name: String)
{

    val scoreboardManager: GameScoreboardManager = GameScoreboardManager(this)
    val gameTask: GameTask = GameTask(this, GourPillars.instance)
    val resetArenaTask = ResetArenaTask(this)
    val waitingPlayer: MutableSet<Player> = mutableSetOf()
    var gameState: State = State.WAITING
    val spawnManager = GourPillars.spawnManager
    private val prefix = "<bold><aqua>Game </bold><gray>|"

    var gameEvent: GameEvents? = null
    val noEventVote: ArrayList<Player> = ArrayList()
    val knockbackVote: ArrayList<Player> = ArrayList()
    val lavaEvent: ArrayList<Player> = ArrayList()
    val dayVote: ArrayList<Player> = ArrayList()
    val nightVote: ArrayList<Player> = ArrayList()

    private fun startArena(){
        CountDownTask(this).runTaskTimer(GourPillars.instance, 0L, 20L)
    }


    /* Utils */
    fun addPlayer(player: Player){
        if(waitingPlayer.contains(player)){
            player.sendDynamicMessage(MessageData.ARENA_ERRORS_ALREADY_IN_GAME)
            return
        }

        if(gameState == State.INGAME || gameState == State.STOPPED){
            player.sendDynamicMessage(MessageData.ARENA_ERRORS_ARENA_NOT_READY)
            return
        }

        if(waitingPlayer.size < maxPlayer){
            waitingPlayer.add(player)
            scoreboardManager.setWaitingBoard(player)
            reloadWaitingScoreboard()
            player.inventory.clear()
            player.health = 20.0
            player.foodLevel = 20
            giveWaitingItems(player)
            //sendMessageToPlayerInGame("$prefix <white>${player.name} <yellow>e' entrato (<green>${waitingPlayer.size}<white>/<green>$maxPlayer<yellow>)")
            sendDynamicMessageToPlayerInGame(MessageData.ARENA_JOIN,
                "{player}" to player.name,
                "{on}" to waitingPlayer.size.toString(),
                "{max}" to maxPlayer.toString())

            for ((location, playerInSpawn) in spawnMap) {
                if (playerInSpawn == null) {
                    Utils.setGlass(true, location)
                    player.gameMode = GameMode.SURVIVAL
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
            player.sendDynamicMessage(MessageData.ARENA_ERRORS_THE_GAME_IS_FULL)
            return
        }
    }

    private fun giveWaitingItems(player: Player){
        val leaveMaterial = ItemStack(Material.RED_DYE)
        val eventMaterial = ItemStack(Material.PAPER)

        val leaveMeta = leaveMaterial.itemMeta.apply {
            displayName("<red>Esci".toMini())
            lore()

            val key = NamespacedKey(GourPillars.instance, "leave-item")

            persistentDataContainer.set(key, PersistentDataType.STRING, "true")
        }
        leaveMaterial.itemMeta = leaveMeta

        val eventMeta = eventMaterial.itemMeta.apply {
            displayName("<green>Vota!".toMini())

            val key = NamespacedKey(GourPillars.instance, "vote-item")

            persistentDataContainer.set(key, PersistentDataType.STRING, "true")
        }
        eventMaterial.itemMeta = eventMeta

        player.inventory.setItem(8, leaveMaterial)
        player.inventory.setItem(0, eventMaterial)
    }

    fun removePlayer(player: Player){
        spawnMap.forEach{ (location, playerInSpawn) ->
            if(playerInSpawn == player){
                Utils.setGlass(false, location)
                spawnMap[location] = null
            }
        }
        lavaEvent.remove(player)
        knockbackVote.remove(player)
        dayVote.remove(player)
        nightVote.remove(player)
        waitingPlayer.remove(player)
        player.sendTitle("", "")
        player.sendDynamicMessage(MessageData.ARENA_LEAVE)
        reloadWaitingScoreboard()
        if(waitingPlayer.size < minPlayer && gameState != State.INGAME){
            gameState = State.WAITING
            val playerRequired = maxPlayer - waitingPlayer.size
            //sendMessageToPlayerInGame("$prefix <green>Mancano <yellow>$playerRequired<green> player per cominciare")
            sendDynamicMessageToPlayerInGame(MessageData.ARENA_PLAYER_NEEDED, "{playerRequired}" to playerRequired.toString())
            return
        }
    }

    private fun reloadWaitingScoreboard(){
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

    fun sendDynamicMessageToPlayerInGame(message: DynamicMessage, vararg pairs: Pair<String, String>) {
        waitingPlayer.forEach{ player: Player ->  player.sendDynamicMessage(message, *pairs)}
    }

    fun sendDynamicTitleToPlayerInGame(title: DynamicMessage, subtitle: DynamicMessage, vararg pairs: Pair<String, String>) {
        waitingPlayer.forEach { player: Player ->
            player.sendDynamicTitle(title, subtitle, *pairs)
        }
    }

    fun sendTitleToPlayerInGame(title: String, subtitle: String){
        waitingPlayer.forEach{ player: Player ->
            player.sendTitle(title.replace("&", "§"), subtitle.replace("&", "§"))
        }

    }

}