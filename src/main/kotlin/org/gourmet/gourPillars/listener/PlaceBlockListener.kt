package org.gourmet.gourPillars.listener

import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.toMini

class PlaceBlockListener : Listener{

    private val arenaManager = GourPillars.arenaManager

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {

        val player = event.player
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        if (!arena.region.isInRegion(player.location)) {
            event.isCancelled = true
            player.sendMessage("<red>Limite raggiunto".toMini())
        }
    }
}