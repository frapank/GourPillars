package org.gourmet.gourPillars.listener.lobby

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.LevelBarManager

class JoinEvent : Listener {


    @EventHandler
    fun joinEvent(event: PlayerJoinEvent){

        GourPillars.Companion.spawnManager.teleportPlayerToSpawn(event.player)

        object : BukkitRunnable(){
            override fun run() {
                GourPillars.Companion.lobbyScoreboardManager.setScoreboard(event.player)
            }
        }.runTaskLater(GourPillars.Companion.instance, 20 * 1)

        LevelBarManager.updateLevelInBar(event.player)

    }
}