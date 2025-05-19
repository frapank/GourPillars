package org.gourmet.gourPillars.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.data.DatabaseManager

class DatabaseListeners : Listener{

    private val databaseManager = GourPillars.databaseManager

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onJoin(event: PlayerJoinEvent) {
        val player: Player = event.player
        if(!databaseManager.isOnline){
            return
        }

        databaseManager.createUser(player);
        val playerData: DatabaseManager.PlayerData = DatabaseManager.PlayerData(player, databaseManager)
        databaseManager.playersData.put(player, playerData)

    }

    @EventHandler
    fun onLeave(event: PlayerQuitEvent) {
        val player: Player = event.player

        if(!databaseManager.isOnline){
            return
        }

        databaseManager.playersData.remove(player)
    }
}
