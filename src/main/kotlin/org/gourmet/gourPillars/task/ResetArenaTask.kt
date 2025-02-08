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
        zipManager.restoreBackup("arena")
        object : BukkitRunnable(){
            override fun run(){

                arenaManager.onlineArenas.forEach{ (name, arena) ->
                    if(name == arenaName){
                        arena.spawnMap.forEach{(location, boolean) ->
                            location.world = Bukkit.getWorld(name)
                        }
                    }
                }
                arena.gameState = State.WAITING
            }
        }.runTaskLater(GourPillars.instance, 100L)
    }

    fun clearEntities(world: World?) {
        if(world == null) return
        for (entity in world.entities) {
            if (entity !is Player) {
                entity.remove()
            }
        }
    }
}
