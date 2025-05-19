package org.gourmet.gourPillars.task.game.gametasks

import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena

class LavaHandler : GameHandler {

    /* Every 4 seconds the lava will raise */
    override fun onStart(arena: Arena) {
        var lavaLevel = arena.minHeight
        object : BukkitRunnable(){
            override fun run() {
                if(!arena.gameTask.running) cancel()
                arena.region.replaceYLevelWithLava(lavaLevel)
                lavaLevel++

            }

        }.runTaskTimer(GourPillars.instance, 0L, 4 * 20)
    }

    override fun onStop(arena: Arena, winner: Player?) {}

}
