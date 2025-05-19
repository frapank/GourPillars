package org.gourmet.gourPillars.task.game.gametasks

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.Arena

class BorderHandler : GameHandler {

    private data class BorderState(val centerX: Double, val centerZ: Double, val size: Double)
    private val originalStates = mutableMapOf<Arena, BorderState>()
    private val tasks = mutableMapOf<Arena, BukkitTask>()
    private val plugin = GourPillars.instance
    private val finalSize: Double = 10.0
    private val shrinkIntervalSec: Long = 7L

    override fun onStart(arena: Arena) {
        val world = arena.region.world
        val border = world.worldBorder

        originalStates[arena] = BorderState(
            border.center.x, border.center.z, border.size
        )

        val cx = (arena.region.minX + arena.region.maxX) / 2.0
        val cz = (arena.region.minZ + arena.region.maxZ) / 2.0
        border.center = Location(world, cx, 0.0, cz)

        val initialSize = maxOf(
            arena.region.maxX - arena.region.minX,
            arena.region.maxZ - arena.region.minZ
        ).toDouble() + 2.0
        border.size = initialSize
        border.damageBuffer = 0.0
        border.damageAmount = 3.0

        val task = object : BukkitRunnable() {
            override fun run() {
                if (!arena.gameTask.running) {
                    cancel()
                    return
                }
                if (border.size <= finalSize) {
                    cancel()
                    return
                }

                border.size = border.size - 1.0
            }
        }.runTaskTimer(plugin, 0L, shrinkIntervalSec * 20L)

        tasks[arena] = task
    }

    override fun onStop(arena: Arena, winner: Player?) {
        tasks.remove(arena)?.cancel()

        val world = arena.region.world
        val border = world.worldBorder
        originalStates.remove(arena)?.let { orig ->
            border.center = Location(world, orig.centerX, 0.0, orig.centerZ)
            border.size = orig.size
        }
    }

}
