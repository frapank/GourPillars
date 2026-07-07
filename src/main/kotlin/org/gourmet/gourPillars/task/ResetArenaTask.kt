package org.gourmet.gourPillars.task

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.ZipManager
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.State

class ResetArenaTask(
    val arena: Arena,
) : BukkitRunnable() {
    private val zipManager = ZipManager()

    override fun run() {
        val arenaManager = GourPillars.arenaManager
        val arenaName = arena.name
        val worldName = arena.region.world.name

        // Reset arena
        zipManager.restoreBackup(worldName)

        // Here I will reset all the pointers
        object : BukkitRunnable() {
            override fun run() {
                arenaManager.onlineArenas.forEach { (name, arena) ->
                    if (name == arenaName) {
                        arena.spawnMap.forEach { (location, _) ->
                            location.world = Bukkit.getWorld(worldName)
                        }
                        arena.spawnMainLocation.world = Bukkit.getWorld(worldName)
                        arena.region.world = Bukkit.getWorld(worldName)!!
                    }
                }
                arena.gameState = State.WAITING
            }
        }.runTaskLater(GourPillars.instance, 100L)

        // Randomize arena order
        GourPillars.arenaManager.shuffleArenas()
    }
}
