package org.gourmet.gourPillars.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.data.PlayerData
import org.gourmet.gourPillars.managers.LevelBarManager

class JoinListener : Listener{

    val jsonManager = GourPillars.jsonManager

    @EventHandler
    fun joinEvent(event: PlayerJoinEvent){

        GourPillars.spawnManager.teleportPlayerToSpawn(event.player)
        GourPillars.lobbyScoreboardManager.setScoreboard(event.player)

        val player = event.player
        val playerData = PlayerData(player.name, 0, 0, 0, 0, 0,0 ,0)

        jsonManager.setPlayerDataIfAbsent(player, playerData)
        jsonManager.savePlayerData()

        LevelBarManager.updateLevelInBar(player)

    }
}
