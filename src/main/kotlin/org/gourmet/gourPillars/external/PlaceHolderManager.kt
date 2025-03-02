package org.gourmet.gourPillars.external

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

    val arenaManager = GourPillars.arenaManager

    override fun onRequest(player: OfflinePlayer?, params: String): String {
        if (player == null) {
            return ""
        }

        val gamePlayer: Player = player as Player
        val playerData = GourPillars.jsonManager.getPlayerData(gamePlayer)
        if (params.equals("minplayers", ignoreCase = true)) {
            return arenaManager.getArenaByPlayer(gamePlayer)?.minPlayer.toString() ?: "no-arena"
        }
        if (params.equals("waitingplayers", ignoreCase = true)) {
            return arenaManager.getArenaByPlayer(gamePlayer)?.waitingPlayer?.size.toString() ?: "no-arena"
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
            return arenaManager.getArenaByPlayer(gamePlayer)?.gameTask?.alivePlayer?.get(player)?.toString() ?: "no arena"
        }
        if(params.equals("arenacount", ignoreCase = true)) {
            return arenaManager.onlineArenas.size.toString()
        }
        if (params.equals("playersinmatch", ignoreCase = true)) {
            var playersInGame = 0
            arenaManager.onlineArenas.forEach{ (_, arena) ->
                playersInGame += arena.waitingPlayer.size
            }
            return playersInGame.toString()
        }
        if (params.equals("deaths", ignoreCase = true)) {
            return playerData?.deaths.toString() ?: "-1"
        }
        if (params.equals("kills", ignoreCase = true)) {
            return playerData?.kills.toString() ?: "-1"
        }
        if (params.equals("wins", ignoreCase = true)) {
            return playerData?.wins.toString() ?: "-1"
        }
        if (params.equals("defeats", ignoreCase = true)) {
            return playerData?.defeats.toString() ?: "-1"
        }
        if (params.equals("gamesplayed", ignoreCase = true)) {
            return playerData?.gamesPlayed.toString() ?: "-1"
        }
        if (params.equals("xp", ignoreCase = true)) {
            return playerData?.xp.toString() ?: "-1"
        }
        if (params.equals("level", ignoreCase = true)) {
            return playerData?.level.toString() ?: "-1"
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
