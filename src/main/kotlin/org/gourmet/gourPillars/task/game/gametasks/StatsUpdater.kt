package org.gourmet.gourPillars.task.game.gametasks

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger

object StatsUpdater {

    private val databaseManager = GourPillars.databaseManager

    fun updateKill(player: Player){

        //Update in database
        val currentDatabaseStats = databaseManager.getStatistics(player.name);

        if(!databaseManager.isOnline){
            return
        }

        if(currentDatabaseStats == null) {
            Logger.warning("Can't update kills, $player is not in the database")
            return
        }

        databaseManager.updateStatistics(
            currentDatabaseStats.name,
            currentDatabaseStats.defeats,
            currentDatabaseStats.kills + 1,
            currentDatabaseStats.wins,
            currentDatabaseStats.xp,
            currentDatabaseStats.level
        )

        //Update localdatabase stats
        val currentLocalStats = databaseManager.playersData[player]?.stats

        if(currentLocalStats == null){
            Logger.warning("Can't update kills, $player is not in the local database")
            return
        }

        currentLocalStats.kills++

    }


    fun updateDefeats(playerName: String){
        //Update in database
        val currentDatabaseStats = databaseManager.getStatistics(playerName);

        if(!databaseManager.isOnline){
            return
        }

        if(currentDatabaseStats == null) {
            Logger.warning("Can't update kills, $playerName is not in the database")
            return
        }

        databaseManager.updateStatistics(
            currentDatabaseStats.name,
            currentDatabaseStats.defeats + 1,
            currentDatabaseStats.kills,
            currentDatabaseStats.wins,
            currentDatabaseStats.xp,
            currentDatabaseStats.level
        )

        //Update localdatabase stats
        val player: Player = Bukkit.getPlayer(playerName) ?: return;

        val currentLocalStats = databaseManager.playersData[player]?.stats

        if(currentLocalStats == null){
            Logger.warning("Can't update kills, $player is not in the local database")
            return
        }

        currentLocalStats.defeats++
    }

    fun updateWins(player: Player){

        //Update in database
        val currentDatabaseStats = databaseManager.getStatistics(player.name);

        if(!databaseManager.isOnline){
            return
        }

        if(currentDatabaseStats == null) {
            Logger.warning("Can't update wins, $player is not in the database")
            return
        }

        databaseManager.updateStatistics(
            currentDatabaseStats.name,
            currentDatabaseStats.defeats,
            currentDatabaseStats.kills,
            currentDatabaseStats.wins + 1,
            currentDatabaseStats.xp,
            currentDatabaseStats.level
        )

        //Update localdatabase stats
        val currentLocalStats = databaseManager.playersData[player]?.stats

        if(currentLocalStats == null){
            Logger.warning("Can't update kills, $player is not in the local database")
            return
        }

        currentLocalStats.wins++
    }
}
