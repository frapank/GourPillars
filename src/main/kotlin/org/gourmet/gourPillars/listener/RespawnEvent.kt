package org.gourmet.gourPillars.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent




class RespawnEvent : Listener {
    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        //event.setRespawnLocation(null)
    }
}