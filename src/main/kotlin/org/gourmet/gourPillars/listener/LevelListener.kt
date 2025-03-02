package org.gourmet.gourPillars.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerExpChangeEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.data.PlayerData

class LevelListener : Listener{

    private val jsonManager = GourPillars.jsonManager

    @EventHandler
    fun onXpGive(event: PlayerExpChangeEvent){

        val player = event.player
        val playerData = jsonManager.getPlayerData(player) ?: PlayerData(player.name, 0 , 0 ,0 ,0 ,0,0,0)

        event.amount = playerData.level
    }
}