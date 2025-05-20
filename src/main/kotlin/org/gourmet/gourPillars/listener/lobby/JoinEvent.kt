package org.gourmet.gourPillars.listener.lobby

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.LevelBarManager
import org.gourmet.gourPillars.other.Utils
import org.gourmet.gourPillars.other.toMini

class JoinEvent : Listener {


    @EventHandler
    fun joinEvent(event: PlayerJoinEvent){

        object : BukkitRunnable() {
            override fun run() {
                GourPillars.Companion.spawnManager.teleportPlayerToSpawn(event.player)
            }
        }.runTaskLater(GourPillars.instance, 1L)

        event.player.health to 20
        event.player.foodLevel to 20

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
        player.foodLevel to 20
    }

}