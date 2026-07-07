package org.gourmet.gourPillars.task

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.ArenaManager

class ShowPlayerTask : BukkitRunnable() {
    val arenaManager: ArenaManager = GourPillars.arenaManager

    override fun run() {
        for (viewer in Bukkit.getOnlinePlayers()) {
            val worldScoped = arenaManager.isPlayerInArena(viewer) || arenaManager.isSpectating(viewer)
            for (other in Bukkit.getOnlinePlayers()) {
                if (viewer === other) continue

                if (worldScoped && viewer.world != other.world) {
                    viewer.hidePlayer(GourPillars.instance, other)
                } else {
                    viewer.showPlayer(GourPillars.instance, other)
                }
            }
        }
    }
}
