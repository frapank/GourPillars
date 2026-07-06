package org.gourmet.gourPillars.task.game.gametasks

import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.Arena

class LavaHandler : GameHandler {

    override fun onStart(arena: Arena) {
        val riseIntervalTicks = GourPillars.instance.config.getLong("game.lava-rise-interval-seconds", 4) * 20
        var lavaLevel = arena.minHeight
        object : BukkitRunnable(){
            override fun run() {
                if(!arena.gameTask.running) cancel()
                arena.region.replaceYLevelWithLava(lavaLevel)
                lavaLevel++

            }

        }.runTaskTimer(GourPillars.instance, 0L, riseIntervalTicks)
    }

    override fun onStop(arena: Arena, winner: Player?) {}

}
