package org.gourmet.gourPillars.managers

import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.data.DatabaseManager
import org.gourmet.gourPillars.other.Logger

object LevelBarManager {

    private val databaseManager = GourPillars.databaseManager

    /*
        This function will update
        the level in the bar
     */
    fun updateLevelInBar(player: Player){
        object : BukkitRunnable(){
            override fun run() {
                try {
                    val playerData = databaseManager.playersData.get(player)
                    player.level = playerData?.stats?.level!!
                } catch (e: Exception){
                    Logger.warning("Error in updateLevelInBar ${e.message}")
                }
            }
        }.runTaskLaterAsynchronously(GourPillars.instance, 20 * 1)

    }
}