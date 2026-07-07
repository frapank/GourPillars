package org.gourmet.gourPillars.managers.game.arena

import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.commands.BuildCMD
import org.gourmet.gourPillars.managers.GameScoreboardManager
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.Region
import org.gourmet.gourPillars.other.Utils
import org.gourmet.gourPillars.other.messages.DynamicMessage
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import org.gourmet.gourPillars.other.messages.sendDynamicTitle
import org.gourmet.gourPillars.other.toMini
import org.gourmet.gourPillars.task.CountDownTask
import org.gourmet.gourPillars.task.ResetArenaTask
import org.gourmet.gourPillars.task.game.gametasks.GameTask
import java.util.*
import kotlin.collections.ArrayList

class Arena(
    val spawnMap: MutableMap<Location, Player?>,
    val spawnMainLocation: Location,
    val isPrivate: Boolean,
    val slowFallingTime: Int,
    val maxPlayer: Int,
    val minPlayer: Int,
    val maxHeight: Int,
    val minHeight: Int,
    val regionLocOne: Location,
    val regionLocTwo: Location,
    var region: Region,
    val name: String,
) {
    val scoreboardManager: GameScoreboardManager = GameScoreboardManager(this)
    val gameTask: GameTask = GameTask(this, GourPillars.instance)
    val resetArenaTask = ResetArenaTask(this)
    val inGamePlayer: MutableSet<Player> = mutableSetOf() // Death and alive ( You can find alivePlayer in GameTask.java )
    val playedPlayerNames: MutableSet<String> = mutableSetOf() // general played ( Death, quitted, etc... ), used in databases
    var gameState: State = State.WAITING
    val spawnManager = GourPillars.spawnManager
    val lastDamagerMap = mutableMapOf<UUID, UUID>()

    var gameEvent: GameEvents? = null
    val noEventVote: ArrayList<Player> = ArrayList()
    val knockbackVote: ArrayList<Player> = ArrayList()
    val lavaEvent: ArrayList<Player> = ArrayList()
    val borderEvent: ArrayList<Player> = ArrayList()
    val dayVote: ArrayList<Player> = ArrayList()
    val nightVote: ArrayList<Player> = ArrayList()

    private fun startArena() {
        CountDownTask(this).runTaskTimer(GourPillars.instance, 0L, 20L)
    }

    // Utils
    fun addPlayer(player: Player) {
        if (isPrivate) {
            player.sendMessage("you can't join")
            return
        }
        if (inGamePlayer.contains(player)) {
            player.sendDynamicMessage(MessageData.ARENA_ERRORS_ALREADY_IN_GAME)
            return
        }

        if (gameState == State.INGAME || gameState == State.STOPPED) {
            player.sendDynamicMessage(MessageData.ARENA_ERRORS_ARENA_NOT_READY)
            return
        }

        if (inGamePlayer.size < maxPlayer) {
            BuildCMD.buildSessionPlayers.remove(player)
            inGamePlayer.add(player)
            scoreboardManager.setWaitingBoard(player)
            reloadWaitingScoreboard()
            player.gameMode = GameMode.SURVIVAL
            player.inventory.clear()
            Utils.resetPlayerState(player)
            giveWaitingItems(player)
            sendDynamicMessageToPlayerInGame(
                MessageData.ARENA_JOIN,
                "{player}" to player.name,
                "{on}" to inGamePlayer.size.toString(),
                "{max}" to maxPlayer.toString(),
            )

            // Teleport and set glass pannel
            for ((location, playerInSpawn) in spawnMap) {
                if (playerInSpawn == null) {
                    Utils.setGlass(true, location)
                    player.teleport(location)
                    spawnMap[location] = player
                    break
                }
            }

            // Start arena if player is enoght
            if (inGamePlayer.size >= minPlayer && gameState == State.WAITING) {
                startArena()
                this.gameState = State.STARTING
            }
            return
        } else {
            player.sendDynamicMessage(MessageData.ARENA_ERRORS_THE_GAME_IS_FULL)
            return
        }
    }

    private fun giveWaitingItems(player: Player) {
        val config = GourPillars.instance.config

        val voteSlot = config.getInt("waiting-items.vote.slot", 0)
        val voteMaterial = config.getString("waiting-items.vote.material", "PAPER")!!
        val leaveSlot = config.getInt("waiting-items.leave.slot", 8)
        val leaveMaterial = config.getString("waiting-items.leave.material", "RED_DYE")!!

        val voteItem =
            createWaitingItem(voteMaterial, MessageData.WAITING_ITEMS_VOTE_NAME, MessageData.WAITING_ITEMS_VOTE_LORE, "vote-item")
        val leaveItem =
            createWaitingItem(leaveMaterial, MessageData.WAITING_ITEMS_LEAVE_NAME, MessageData.WAITING_ITEMS_LEAVE_LORE, "leave-item")

        player.inventory.setItem(voteSlot, voteItem)
        player.inventory.setItem(leaveSlot, leaveItem)
    }

    private fun createWaitingItem(
        materialName: String,
        name: Component,
        lore: Component,
        tag: String,
    ): ItemStack {
        val material =
            Material.matchMaterial(materialName) ?: run {
                Logger.warning("Invalid material '$materialName' for the $tag waiting item, using PAPER")
                Material.PAPER
            }
        val item = ItemStack(material, 1)
        val meta = item.itemMeta

        meta.displayName(name)
        meta.lore(lore.children())

        val key = NamespacedKey(GourPillars.instance, tag)
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, "true")

        item.itemMeta = meta
        return item
    }

    fun removePlayer(player: Player) {
        // Remove spawn and glass
        spawnMap.forEach { (location, playerInSpawn) ->
            if (playerInSpawn == player) {
                Utils.setGlass(false, location)
                spawnMap[location] = null
            }
        }

        // Clear Events vote
        noEventVote.remove(player)
        lavaEvent.remove(player)
        knockbackVote.remove(player)
        borderEvent.remove(player)
        dayVote.remove(player)
        nightVote.remove(player)
        inGamePlayer.remove(player)

        // clear player
        Utils.resetPlayerState(player)

        // Customization
        player.sendTitle("", "")
        player.sendDynamicMessage(MessageData.ARENA_LEAVE)
        reloadWaitingScoreboard()

        // Stop cooldown if player is not enought
        if (inGamePlayer.size < minPlayer && gameState != State.INGAME) {
            gameState = State.WAITING
            val playerRequired = maxPlayer - inGamePlayer.size
            sendDynamicMessageToPlayerInGame(MessageData.ARENA_PLAYER_NEEDED, "{playerRequired}" to playerRequired.toString())
            return
        }
    }

    private fun reloadWaitingScoreboard() {
        inGamePlayer.forEach { player ->
            scoreboardManager.setWaitingBoard(player)
        }
    }

    fun reloadInGameScoreboard() {
        inGamePlayer.forEach { player ->
            scoreboardManager.setGameScoreboard(player)
        }
    }

    // Getter and Settere
    fun containPlayer(player: Player): Boolean = inGamePlayer.contains(player)

    fun sendMessageToPlayerInGame(message: String) {
        inGamePlayer.forEach { player: Player -> player.sendMessage(message.toMini()) }
    }

    fun sendDynamicMessageToPlayerInGame(
        message: DynamicMessage,
        vararg pairs: Pair<String, String>,
    ) {
        inGamePlayer.forEach { player: Player -> player.sendDynamicMessage(message, *pairs) }
    }

    fun sendDynamicTitleToPlayerInGame(
        title: DynamicMessage,
        subtitle: DynamicMessage,
        vararg pairs: Pair<String, String>,
    ) {
        inGamePlayer.forEach { player: Player ->
            player.sendDynamicTitle(title, subtitle, *pairs)
        }
    }

    fun sendTitleToPlayerInGame(
        title: String,
        subtitle: String,
    ) {
        inGamePlayer.forEach { player: Player ->
            player.sendTitle(title.replace("&", "§"), subtitle.replace("&", "§"))
        }
    }
}
