package org.gourmet.gourPillars.task.game.gametasks

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.api.EliminationCause
import org.gourmet.gourPillars.api.event.GameEventHandler
import org.gourmet.gourPillars.api.events.GourPillarsEventSelectedEvent
import org.gourmet.gourPillars.api.events.GourPillarsGameEndEvent
import org.gourmet.gourPillars.api.events.GourPillarsGameStartEvent
import org.gourmet.gourPillars.api.events.GourPillarsPlayerEliminatedEvent
import org.gourmet.gourPillars.api.events.GourPillarsPlayerFinishEvent
import org.gourmet.gourPillars.api.events.GourPillarsPlayerKillEvent
import org.gourmet.gourPillars.managers.XpSource
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.ArenaEventContext
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.Utils
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import org.gourmet.gourPillars.task.game.GameFunctions
import org.gourmet.gourPillars.task.game.GameRandom
import kotlin.collections.forEach

class GameTask(
    private val arena: Arena,
    private val plugin: JavaPlugin,
) : BukkitRunnable() {
    lateinit var alivePlayer: MutableSet<Player>
    lateinit var playerKills: MutableMap<Player, Int>
    var running = false
    private val matchDurationSeconds = GourPillars.instance.config.getInt("game.match-duration-seconds", 300)
    var secondsPassed = matchDurationSeconds
    private var currentEventHandler: GameEventHandler? = null
    private var eventHandlerStarted = false
    private var gameEnded = false

    // alivePlayer/playerKills are only set once the match has actually started (see run()).
    fun aliveCount(): Int? = if (::alivePlayer.isInitialized) alivePlayer.size else null

    fun killsOf(player: Player): Int? = if (::playerKills.isInitialized) playerKills[player] else null

    fun isAlive(player: Player): Boolean = ::alivePlayer.isInitialized && alivePlayer.contains(player)

    fun alivePlayersSnapshot(): List<Player> = if (::alivePlayer.isInitialized) alivePlayer.toList() else emptyList()

    fun isMatchRunning(): Boolean = running && !gameEnded

    override fun run() {
        if (running) return

        // Init game
        running = true
        gameEnded = false
        alivePlayer = mutableSetOf()
        playerKills = mutableMapOf()
        preparePlayer()

        alivePlayer.forEach { player ->
            arena.playedPlayerNames.add(player.name)
            StatsUpdater.updateGamesPlayed(player)
            StatsUpdater.addXp(player, XpSource.GAME_PLAYED)
        }

        removeAllGlass()
        setTimeByVote()
        GameRandom.startRandomItemTask(alivePlayer) { running }

        // Start event if present
        startActiveEvent()
        Bukkit.getPluginManager().callEvent(GourPillarsGameStartEvent(arena.name))

        object : BukkitRunnable() {
            override fun run() {
                secondsPassed--
                if (!running) cancel()

                updateScoreBoard()

                // End game
                if (gameEnded || alivePlayer.size <= 1 || secondsPassed == 0) {
                    handleEndGame()
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }

    // eventId must be a registered event id, or null for no event. An id that is no
    // longer registered (its plugin got disabled after the vote) falls back to null.
    fun applyEvent(eventId: String?) {
        val definition = eventId?.let { GourPillars.gameEventRegistry.get(it) }
        arena.gameEvent = definition?.id
        eventHandlerStarted = false
        currentEventHandler =
            definition?.let {
                try {
                    it.createHandler(ArenaEventContext(arena))
                } catch (e: Exception) {
                    Logger.warning("Game event '${it.id}' threw while creating its handler, the match continues without it: $e")
                    arena.gameEvent = null
                    null
                }
            }
        Bukkit.getPluginManager().callEvent(GourPillarsEventSelectedEvent(arena.name, arena.gameEvent))
    }

    private fun startActiveEvent() {
        val handler = currentEventHandler ?: return
        try {
            handler.onStart()
            eventHandlerStarted = true
        } catch (e: Exception) {
            Logger.warning("Game event '${arena.gameEvent}' threw in onStart, the match continues without it: $e")
            currentEventHandler = null
            arena.gameEvent = null
        }
    }

    private fun stopActiveEvent(winner: Player?) {
        val handler = currentEventHandler ?: return
        currentEventHandler = null
        arena.gameEvent = null
        // Per the GameEventHandler contract, onStop only fires after a successful onStart.
        if (!eventHandlerStarted) return
        eventHandlerStarted = false
        try {
            handler.onStop(winner)
        } catch (e: Exception) {
            Logger.warning("Game event handler threw in onStop: $e")
        }
    }

    // Detaches the active event mid-match, e.g. because its plugin got disabled.
    fun clearActiveEvent() {
        stopActiveEvent(null)
    }

    private fun setTimeByVote() {
        val worldName =
            arena.spawnMap.keys
                .first()
                .world.name
        val world = Bukkit.getWorld(worldName)
        if (world != null) {
            if (arena.nightVote.size <= arena.dayVote.size) {
                world.time = 6000
            } else {
                world.time = 18000
            }
        }
    }

    private fun updateScoreBoard() {
        arena.inGamePlayer.forEach { player ->
            arena.scoreboardManager.setGameScoreboard(player)
        }
    }

    private fun getWinner(): Player? {
        val aliveAndWithKills = playerKills.filter { (player, _) -> alivePlayer.contains(player) }

        return when {
            alivePlayer.size == 1 -> alivePlayer.first()
            aliveAndWithKills.isNotEmpty() -> aliveAndWithKills.maxByOrNull { it.value }?.key
            else -> alivePlayer.firstOrNull()
        }
    }

    private fun handleEndGame() {
        if (gameEnded) return
        gameEnded = true
        arena.gameState = State.STOPPED

        val winner = getWinner()
        if (winner != null) {
            arena.sendDynamicTitleToPlayerInGame(MessageData.ARENA_TITLE_END, MessageData.ARENA_SUBTITLE_END, "{winner}" to winner.name)
            arena.inGamePlayer.forEach { messagePlayer ->
                messagePlayer.sendDynamicMessage(MessageData.WIN_GAME, "{winner}" to winner.name)
            }
        }

        // Update the statistic only for the winner
        arena.nightVote.clear()
        arena.dayVote.clear()
        arena.eventVotes.clear()

        // Send end game message at the end of the game
        if (winner != null) {
            winner.isInvulnerable = true
            GameFunctions.playVictoryEffects(winner, arena)
            StatsUpdater.updateWins(winner)
            StatsUpdater.incrementStreak(winner)
            StatsUpdater.addXp(winner, XpSource.WIN)

            arena.inGamePlayer.forEach { player ->
                player.sendDynamicMessage(
                    MessageData.END_GAME,
                    "{time}" to getTimeFormatted(),
                    "{kills}" to (playerKills[player] ?: 0).toString(),
                    "{map}" to arena.name,
                )
            }

            Bukkit.getPluginManager().callEvent(
                GourPillarsPlayerFinishEvent(arena.name, winner, playerKills[winner] ?: 0, won = true),
            )
        }

        stopActiveEvent(winner)
        Bukkit.getPluginManager().callEvent(GourPillarsGameEndEvent(arena.name, winner))

        // Arena reset
        object : BukkitRunnable() {
            override fun run() {
                running = false
                secondsPassed = matchDurationSeconds
                arena.gameState = State.STOPPED

                arena.spectators.toList().forEach { spectator ->
                    arena.removeSpectator(spectator)
                    spectator.sendDynamicMessage(MessageData.SPECTATE_MATCH_ENDED)
                }

                // Teleport all play
                arena.inGamePlayer.forEach { player ->
                    GourPillars.spawnManager.teleportPlayerToSpawn(player)
                    GourPillars.lobbyScoreboardManager.setScoreboard(player)
                    player.inventory.clear()
                    Utils.resetPlayerState(player)
                    Utils.giveLobbyItems(player)
                }

                // Restore map pointer
                arena.spawnMap.forEach { (location, _) ->
                    arena.spawnMap[location] = null
                }

                arena.playedPlayerNames.clear()
                arena.lastDamagerMap.clear()
                arena.inGamePlayer.clear()
                alivePlayer.clear()

                cancel()
                arena.resetArenaTask.run()
            }
        }.runTaskLater(plugin, 80L)
    }

    private fun removeAllGlass() {
        arena.spawnMap.forEach { (location, _) ->
            Utils.setGlass(false, arena.cageLocation(location))
        }
    }

    private fun preparePlayer() {
        // Reset kills
        arena.inGamePlayer.forEach { player: Player ->
            alivePlayer.add(player)
            playerKills[player] = 0
        }

        // Reset player foot, level, health and apply slow falling level
        val effect = PotionEffect(PotionEffectType.SLOW_FALLING, arena.slowFallingTime * 20, 0)
        alivePlayer.forEach { player ->
            Utils.resetPlayerState(player)
            player.addPotionEffect(effect)
            player.inventory.clear()
            player.gameMode = GameMode.SURVIVAL
            player.closeInventory()
            arena.scoreboardManager.setGameScoreboard(player)
        }
    }

    private fun eliminationProcess(
        player: Player,
        cause: EliminationCause,
        source: Entity? = null,
    ) {
        // Remove player from arena
        val kills = playerKills[player]
        alivePlayer.remove(player)
        player.gameMode = GameMode.SPECTATOR
        arena.reloadInGameScoreboard()
        player.teleport(arena.spawnMainLocation)
        Bukkit.getPluginManager().callEvent(GourPillarsPlayerEliminatedEvent(arena.name, player, cause, source))
        Bukkit.getPluginManager().callEvent(GourPillarsPlayerFinishEvent(arena.name, player, kills ?: 0, won = false))

        // Play death sound to all players
        arena.inGamePlayer.forEach { playerSound ->
            playerSound.playSound(playerSound.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f)
        }
    }

    // general elimination
    fun playerEliminated(player: Player) {
        eliminationProcess(player, EliminationCause.OTHER)
        arena.inGamePlayer.forEach { receiverPlayer ->
            receiverPlayer.sendDynamicMessage(MessageData.ARENA_PLAYER_ELIMINATED, "{player}" to player.name)
        }
        checkForGameEnd()
    }

    // fall damage death message
    fun playerEliminatedFall(player: Player) {
        eliminationProcess(player, EliminationCause.FALL)
        arena.inGamePlayer.forEach { receiverPlayer ->
            receiverPlayer.sendDynamicMessage(MessageData.ARENA_PLAYER_ELIMINATED_FALL, "{player}" to player.name)
        }
        checkForGameEnd()
    }

    // fall void fall death message
    fun playerEliminatedVoid(player: Player) {
        eliminationProcess(player, EliminationCause.VOID)
        arena.inGamePlayer.forEach { receiverPlayer ->
            receiverPlayer.sendDynamicMessage(MessageData.ARENA_PLAYER_ELIMINATED_VOID, "{player}" to player.name)
        }
        checkForGameEnd()
    }

    // player void kill by player death message
    fun playerEliminatedVoid(
        player: Player,
        killer: Player,
    ) {
        // Update killer stats
        eliminationProcess(player, EliminationCause.VOID_KILL, killer)
        StatsUpdater.looseStreak(player)
        StatsUpdater.updateKill(killer)
        StatsUpdater.addXp(killer, XpSource.VOID_KILL)
        Bukkit.getPluginManager().callEvent(GourPillarsPlayerKillEvent(arena.name, killer, player))

        // Send eliminated message
        arena.inGamePlayer.forEach { receiverPlayer ->
            receiverPlayer.sendDynamicMessage(
                MessageData.ARENA_PLAYER_ELIMINATED_VOID_ATTACK,
                "{player}" to player.name,
                "{killer}" to killer.name,
            )
        }

        // Update in game kills
        if (alivePlayer.contains(killer)) {
            val oldKills = playerKills[killer]!! + 1
            playerKills[killer] = oldKills
        }

        arena.reloadInGameScoreboard()
        checkForGameEnd()
    }

    // fall mob kill death message
    fun playerEliminatedByMob(
        player: Player,
        damager: Entity,
    ) {
        eliminationProcess(player, EliminationCause.MOB, damager)
        arena.inGamePlayer.forEach { receiverPlayer ->
            receiverPlayer.sendDynamicMessage(
                MessageData.ARENA_PLAYER_ELIMINATED_MOB,
                "{player}" to player.name,
                "{killer}" to damager.name,
            )
        }
        checkForGameEnd()
    }

    // player kill by player death message
    fun playerEliminated(
        player: Player,
        killer: Player,
    ) {
        eliminationProcess(player, EliminationCause.KILL, killer)
        StatsUpdater.updateKill(killer)
        StatsUpdater.looseStreak(player)
        StatsUpdater.addXp(killer, XpSource.KILL)
        Bukkit.getPluginManager().callEvent(GourPillarsPlayerKillEvent(arena.name, killer, player))

        // Send eliminated message
        arena.inGamePlayer.forEach { receiverPlayer ->
            if (receiverPlayer != player) {
                receiverPlayer.sendDynamicMessage(
                    MessageData.ARENA_PLAYER_ELIMINATED_KILL,
                    "{player}" to player.name,
                    "{killer}" to killer.name,
                )
            }
        }

        // Update in game kills
        if (alivePlayer.contains(killer)) {
            val oldKills = playerKills[killer]!! + 1
            playerKills[killer] = oldKills
        }

        arena.reloadInGameScoreboard()
        checkForGameEnd()
    }

    private fun checkForGameEnd() {
        if (alivePlayer.size <= 1) {
            handleEndGame()
        }
    }

    fun getTimeFormatted(): String {
        val elapsedSeconds = matchDurationSeconds - secondsPassed
        val minutes = elapsedSeconds / 60
        val remainingSeconds = elapsedSeconds % 60

        val minuteText = if (minutes == 1) "1 minute" else "$minutes minutes"
        val secondText = if (remainingSeconds == 1) "1 second" else "$remainingSeconds seconds"

        return if (minutes > 0) "$minuteText $secondText" else secondText
    }
}
