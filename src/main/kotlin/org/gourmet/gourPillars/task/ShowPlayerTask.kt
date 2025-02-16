package org.gourmet.gourPillars.task

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars

class ShowPlayerTask : BukkitRunnable() {
    override fun run() {
        for (player in Bukkit.getOnlinePlayers()) {
            for (other in Bukkit.getOnlinePlayers()) {
                if (player === other) continue

                if (player.world == other.world) {
                    player.showPlayer(GourPillars.instance, other)
                } else {
                    player.hidePlayer(GourPillars.instance, other)
                }
            }
        }
    }
}