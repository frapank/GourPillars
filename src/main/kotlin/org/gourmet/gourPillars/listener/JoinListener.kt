package org.gourmet.gourPillars.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.data.DatabaseManager
import org.gourmet.gourPillars.managers.LevelBarManager

class JoinListener : Listener{


    @EventHandler
    fun joinEvent(event: PlayerJoinEvent){

        GourPillars.spawnManager.teleportPlayerToSpawn(event.player)

        object : BukkitRunnable(){
            override fun run() {
                GourPillars.lobbyScoreboardManager.setScoreboard(event.player)
            }
        }.runTaskLater(GourPillars.instance, 20 * 1)

        LevelBarManager.updateLevelInBar(event.player)

    }
}
