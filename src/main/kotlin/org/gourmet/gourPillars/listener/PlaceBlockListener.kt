package org.gourmet.gourPillars.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.toMini

class PlaceBlockListener : Listener{

    private val arenaManager = GourPillars.arenaManager

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {

        val player = event.player
        val blockY = event.blockPlaced.location.y
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        if (blockY >= arena.maxHeight) {
            event.isCancelled = true
            player.sendMessage("<red>Altezza massima raggiunta".toMini())
        }
    }
}