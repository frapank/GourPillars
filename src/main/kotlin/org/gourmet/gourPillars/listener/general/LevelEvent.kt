package org.gourmet.gourPillars.listener.general

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerExpChangeEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger

class LevelEvent : Listener {

    private val databaseManager = GourPillars.Companion.databaseManager

    //This will set the xp bar with the actual game level
    @EventHandler
    fun onXpGive(event: PlayerExpChangeEvent){
        try {
            val player = event.player
            val playerData = databaseManager.playersData.get(player)

            event.amount = playerData?.stats?.level!!
        } catch (e: Exception){
            Logger.warning("Error in onXpGive ${e.message}")
        }
    }
}