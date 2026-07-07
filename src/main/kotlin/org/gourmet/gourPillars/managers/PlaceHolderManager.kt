package org.gourmet.gourPillars.managers

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.DatabaseManager.PlayerStats
import org.gourmet.gourPillars.other.Logger

class PlaceHolderManager : PlaceholderExpansion() {
    companion object {
        private const val IDENTIFIER = "pillars"
        private const val AUTHOR = "gourmet"
        private const val VERSION = "1.0.0"
    }

    private val arenaManager = GourPillars.arenaManager
    private val databaseManager = GourPillars.databaseManager

    override fun onRequest(
        player: OfflinePlayer?,
        params: String,
    ): String {
        if (player !is Player) return ""
        val gamePlayer: Player = player

        val stats: PlayerStats? = databaseManager.playersStats[gamePlayer]

        return when (params.lowercase()) {
            "minplayers" -> {
                arenaManager.getArenaByPlayer(gamePlayer)?.minPlayer?.toString() ?: "no-arena"
            }

            "maxplayers" -> {
                arenaManager.getArenaByPlayer(gamePlayer)?.maxPlayer?.toString() ?: "no-arena"
            }

            "waitingplayers" -> {
                arenaManager
                    .getArenaByPlayer(gamePlayer)
                    ?.inGamePlayer
                    ?.size
                    ?.toString() ?: "no-arena"
            }

            "arenaname" -> {
                arenaManager.getArenaByPlayer(gamePlayer)?.name ?: "no-arena"
            }

            "aliveplayers" -> {
                arenaManager
                    .getArenaByPlayer(gamePlayer)
                    ?.gameTask
                    ?.alivePlayer
                    ?.size
                    ?.toString() ?: "no-arena"
            }

            "time" -> {
                arenaManager.getArenaByPlayer(gamePlayer)?.gameTask?.getTimeFormatted() ?: "time-error"
            }

            "ingamekills" -> {
                arenaManager
                    .getArenaByPlayer(gamePlayer)
                    ?.gameTask
                    ?.playerKills
                    ?.get(gamePlayer)
                    ?.toString() ?: "0"
            }

            "arenacount" -> {
                arenaManager.onlineArenas.size.toString()
            }

            "playersinmatch" -> {
                arenaManager.onlineArenas.values
                    .sumOf { it.inGamePlayer.size }
                    .toString()
            }

            "kills" -> {
                stats?.kills?.toString() ?: "0"
            }

            "wins" -> {
                stats?.wins?.toString() ?: "0"
            }

            "defeats" -> {
                stats?.let { (it.playedGame - it.wins).toString() } ?: "0"
            }

            "xp" -> {
                stats?.xp?.toString() ?: "0"
            }

            "level" -> {
                stats?.level?.toString() ?: "0"
            }

            else -> {
                ""
            }
        }
    }

    override fun getIdentifier(): String = IDENTIFIER

    override fun getAuthor(): String = AUTHOR

    override fun getVersion(): String = VERSION
}
