package org.gourmet.gourPillars.managers

import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.data.PlayerData

object LevelBarManager {

    private val jsonManager = GourPillars.jsonManager

    fun updateLevelInBar(player: Player){
        val playerData = jsonManager.getPlayerData(player) ?: PlayerData(player.name, 0 , 0 ,0 ,0 ,0, 0, 0)
        player.level = playerData.level
    }
}