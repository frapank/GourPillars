package org.gourmet.gourPillars.listener.lobby

import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.commands.BuildCMD
import org.gourmet.gourPillars.managers.LevelBarManager
import org.gourmet.gourPillars.other.Utils

class JoinEvent : Listener {


    @EventHandler
    fun joinEvent(event: PlayerJoinEvent){

        object : BukkitRunnable() {
            override fun run() {
                GourPillars.Companion.spawnManager.teleportPlayerToSpawn(event.player)
            }
        }.runTaskLater(GourPillars.instance, 1L)

        event.player.health = 20.0
        event.player.foodLevel = 20
        event.joinMessage = ""

        Utils.giveLobbyItems(event.player)

        object : BukkitRunnable(){
            override fun run() {
                GourPillars.Companion.lobbyScoreboardManager.setScoreboard(event.player)
            }
        }.runTaskLater(GourPillars.Companion.instance, 20 * 1)

        LevelBarManager.updateLevelInBar(event.player)

    }

    @EventHandler
    fun onFoodChange(event: FoodLevelChangeEvent) {
        if(event.entity !is Player) return
        val player: Player = event.entity as Player
        if(!isSpawnWorld(player.world)) return
        event.foodLevel = 20
    }

    private fun isSpawnWorld(eventWorld: World): Boolean {
        return GourPillars.spawnManager.getConfiguredWorld() == eventWorld
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if(BuildCMD.buildSessionPlayers.contains(event.player)) {
            BuildCMD.buildSessionPlayers.remove(event.player)
        }
        event.quitMessage = ""
    }

}