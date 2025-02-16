package org.gourmet.gourPillars.task

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.arena.State
import org.gourmet.gourPillars.managers.ZipManager

class ResetArenaTask(val arena: Arena) : BukkitRunnable(){

    private val zipManager = ZipManager()

    override fun run() {
        val arenaManager = GourPillars.arenaManager
        val arenaName = arena.name
        val worldName = GourPillars.instance.config.getString("Arenas.${arena.name}.world").toString()
        zipManager.restoreBackup(worldName)
        object : BukkitRunnable(){
            override fun run(){
                arenaManager.onlineArenas.forEach{ (name, arena) ->
                    if(name == arenaName){
                        arena.spawnMap.forEach{(location, _) ->
                            location.world = Bukkit.getWorld(worldName)
                        }
                        arena.spawnMainLocation.world = Bukkit.getWorld(worldName)
                    }
                }
                arena.gameState = State.WAITING
            }
        }.runTaskLater(GourPillars.instance, 100L)
        GourPillars.arenaManager.shuffleArenas()
    }

}
