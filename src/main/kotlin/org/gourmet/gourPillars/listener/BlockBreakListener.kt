package org.gourmet.gourPillars.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.State

class BlockBreakListener : Listener{

    private val arenaManager = GourPillars.arenaManager
    private val prefix = "<bold><aqua>Game </bold><gray>|"

    @EventHandler
    fun breakListener(event: BlockBreakEvent){
        val player = event.player
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        if (arena.gameState != State.INGAME){
            event.isCancelled = true
            return
        }

    }
}