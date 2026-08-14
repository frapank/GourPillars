package org.gourmet.gourPillars.task

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.ZipManager
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.Logger

class ResetArenaTask(
    val arena: Arena,
) : BukkitRunnable() {
    private val zipManager = ZipManager()

    override fun run() {
        val arenaManager = GourPillars.arenaManager
        val arenaName = arena.name
        val worldName = arena.region.world.name

        zipManager.restoreBackup(worldName) {
            val restoredWorld = Bukkit.getWorld(worldName)
            if (restoredWorld == null) {
                Logger.warning("World '$worldName' is not loaded after the reset of arena '$arenaName': leaving it stopped")
                return@restoreBackup
            }

            // Every arena in that world points at the old, now unloaded World instance.
            arenaManager.onlineArenas.values
                .filter { it.name == arenaName || it.region.world.name == worldName }
                .forEach { it.rebindToWorld(restoredWorld) }

            arena.gameState = State.WAITING
        }

        // Randomize arena order
        GourPillars.arenaManager.shuffleArenas()
    }
}
