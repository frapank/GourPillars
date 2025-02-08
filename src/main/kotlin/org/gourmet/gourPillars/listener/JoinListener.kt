package org.gourmet.gourPillars.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.gourmet.gourPillars.GourPillars

class JoinListener : Listener{

    @EventHandler
    fun joinEvent(event: PlayerJoinEvent){
        GourPillars.spawnManager.teleportPlayerToSpawn(event.player)
    }
}
