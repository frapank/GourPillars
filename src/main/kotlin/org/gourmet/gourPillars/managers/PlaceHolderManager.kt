package org.gourmet.gourPillars.managers

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars

class PlaceHolderManager : PlaceholderExpansion() {

    companion object{
        private const val IDENTIFIER = "pillars"
        private const val AUTHOR = "gourmet"
        private const val VERSION = "1.0.0"
    }

    val arenaManager = GourPillars.Companion.arenaManager

    override fun onRequest(player: OfflinePlayer?, params: String): String {
        if (player == null) {
            return ""
        }

        val gamePlayer: Player = player as Player
        val playerData = GourPillars.Companion.databaseManager.playersData.get(player)
        if (params.equals("minplayers", ignoreCase = true)) {
            return arenaManager.getArenaByPlayer(gamePlayer)?.minPlayer.toString() ?: "no-arena"
        }
        if (params.equals("waitingplayers", ignoreCase = true)) {
            return arenaManager.getArenaByPlayer(gamePlayer)?.inGamePlayer?.size.toString() ?: "no-arena"
        }
        if (params.equals("arenaname", ignoreCase = true)) {
            return arenaManager.getArenaByPlayer(gamePlayer)?.name.toString() ?: "no arena"
        }
        if (params.equals("aliveplayers", ignoreCase = true)) {
            return arenaManager.getArenaByPlayer(gamePlayer)?.gameTask?.alivePlayer?.size.toString() ?: "no arena"
        }
       if (params.equals("time", ignoreCase = true)) {
            return arenaManager.getArenaByPlayer(gamePlayer)?.gameTask?.getTimeFormatted() ?: "time error"
        }
        if (params.equals("ingamekills", ignoreCase = true)) {
            return arenaManager.getArenaByPlayer(gamePlayer)?.gameTask?.playerKills?.get(player)?.toString() ?: "death"
        }
        if(params.equals("arenacount", ignoreCase = true)) {
            return arenaManager.onlineArenas.size.toString()
        }
        if (params.equals("playersinmatch", ignoreCase = true)) {
            var playersInGame = 0
            arenaManager.onlineArenas.forEach{ (_, arena) ->
                playersInGame += arena.inGamePlayer.size
            }
            return playersInGame.toString()
        }
        if (params.equals("kills", ignoreCase = true)) {
            return playerData?.stats?.kills.toString() ?: "-1"
        }
        if (params.equals("wins", ignoreCase = true)) {
            return playerData?.stats?.wins.toString() ?: "-1"
        }
        if (params.equals("defeats", ignoreCase = true)) {
            return playerData?.stats?.defeats.toString() ?: "-1"
        }
        if (params.equals("xp", ignoreCase = true)) {
            return playerData?.stats?.xp.toString() ?: "-1"
        }
        if (params.equals("level", ignoreCase = true)) {
            return playerData?.stats?.level.toString() ?: "-1"
        }

        return "no-arena-2"
    }

    override fun getIdentifier(): String {
        return IDENTIFIER
    }

    override fun getAuthor(): String {
        return AUTHOR
    }

    override fun getVersion(): String {
        return VERSION
    }
}