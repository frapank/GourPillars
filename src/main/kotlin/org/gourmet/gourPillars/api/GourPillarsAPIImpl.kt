package org.gourmet.gourPillars.api

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.api.event.GameEventDefinition
import org.gourmet.gourPillars.database.PlayerStats
import org.gourmet.gourPillars.managers.game.ArenaManager
import org.gourmet.gourPillars.managers.game.GameEventRegistry
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.Utils
import java.util.concurrent.CompletableFuture

class GourPillarsAPIImpl(
    private val arenaManager: ArenaManager,
    private val eventRegistry: GameEventRegistry,
) : GourPillarsAPI {
    override fun getArenas(): List<ArenaInfo> = arenaManager.onlineArenas.values.map { it.toInfo() }

    override fun getAvailableArenas(): List<ArenaInfo> = getArenas().filter { it.isJoinable }

    override fun getArena(name: String): ArenaInfo? = arenaManager.getArenaByName(name)?.toInfo()

    override fun isArenaFull(name: String): Boolean? = arenaManager.getArenaByName(name)?.let { it.inGamePlayer.size >= it.maxPlayer }

    override fun getArenaOfPlayer(player: Player): ArenaInfo? = arenaManager.getArenaByPlayer(player)?.toInfo()

    override fun sendPlayerToArena(
        player: Player,
        arenaName: String,
    ): ArenaJoinResult {
        val arena = arenaManager.getArenaByName(arenaName) ?: return ArenaJoinResult.ARENA_NOT_FOUND
        return arena.addPlayer(player)
    }

    override fun removePlayerFromArena(player: Player): Boolean {
        val arena = arenaManager.getArenaByPlayer(player) ?: return false
        if (arena.gameState == State.INGAME) {
            if (arena.gameTask.isAlive(player)) {
                arena.gameTask.playerEliminated(player)
            }
            arena.inGamePlayer.remove(player)
        } else {
            arena.removePlayer(player)
        }
        arena.spawnManager.teleportPlayerToSpawn(player)
        player.inventory.clear()
        Utils.resetPlayerState(player)
        GourPillars.lobbyScoreboardManager.setScoreboard(player)
        Utils.giveLobbyItems(player)
        return true
    }

    override fun isPlayerCaged(player: Player): Boolean = arenaManager.getArenaByPlayer(player)?.isPlayerCaged(player) ?: false

    override fun getPlayerLocation(player: Player): Location = player.location

    override fun isSpectating(player: Player): Boolean = arenaManager.isSpectating(player)

    override fun getSpectatedArena(player: Player): ArenaInfo? = arenaManager.getArenaBySpectator(player)?.toInfo()

    override fun getSpectators(arenaName: String): List<Player>? = arenaManager.getArenaByName(arenaName)?.spectators?.toList()

    override fun getPlayersInArena(arenaName: String): List<Player>? = arenaManager.getArenaByName(arenaName)?.inGamePlayer?.toList()

    override fun getAlivePlayers(arenaName: String): List<Player>? {
        val arena = arenaManager.getArenaByName(arenaName) ?: return null
        if (arena.gameTask.aliveCount() == null) return null
        return arena.gameTask.alivePlayer.toList()
    }

    override fun getMatchKills(player: Player): Int? = arenaManager.getArenaByPlayer(player)?.gameTask?.killsOf(player)

    override fun getTimeRemainingSeconds(arenaName: String): Int? =
        arenaManager
            .getArenaByName(arenaName)
            ?.takeIf { it.gameState == State.INGAME }
            ?.gameTask
            ?.secondsPassed

    override fun getCurrentEvent(arenaName: String): String? = arenaManager.getArenaByName(arenaName)?.gameEvent

    override fun getCurrentEventOfPlayer(player: Player): String? = arenaManager.getArenaByPlayer(player)?.gameEvent

    override fun registerEvent(
        owner: Plugin,
        event: GameEventDefinition,
    ): Boolean = eventRegistry.register(owner, event)

    override fun unregisterEvent(eventId: String): Boolean = eventRegistry.unregister(eventId)

    override fun unregisterEvents(owner: Plugin): Int = eventRegistry.unregisterAll(owner)

    override fun getRegisteredEvents(): List<GameEventDefinition> = eventRegistry.definitions()

    override fun getRegisteredEvent(eventId: String): GameEventDefinition? = eventRegistry.get(eventId)

    override fun getPlayerStatistics(playerName: String): CompletableFuture<PlayerStats?> = GourPillars.database.getStatistics(playerName)

    override fun getCachedPlayerStatistics(player: Player): PlayerStats? = GourPillars.playersStats[player]

    private fun Arena.toInfo() =
        ArenaInfo(
            name = name,
            state = gameState,
            currentPlayers = inGamePlayer.size,
            maxPlayers = maxPlayer,
            minPlayers = minPlayer,
            isPrivate = isPrivate,
            alivePlayers = gameTask.aliveCount(),
            secondsRemaining = if (gameState == State.INGAME) gameTask.secondsPassed else null,
            currentEvent = gameEvent,
        )
}
