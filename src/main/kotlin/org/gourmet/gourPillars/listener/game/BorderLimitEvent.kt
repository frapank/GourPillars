package org.gourmet.gourPillars.listener.game

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage

class BorderLimitEvent : Listener {

    private val arenaManager = GourPillars.Companion.arenaManager

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {

        val player = event.player
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        if (!arena.region.isInRegion(player.location)) {
            event.isCancelled = true
            player.sendDynamicMessage(MessageData.ARENA_ERRORS_LIMIT_REACHED)
        }
    }
}