package org.gourmet.gourPillars.listener.game

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.State

class QueueDamageListener : Listener {
    private val arenaManager = GourPillars.arenaManager

    // Damage only makes sense while the match is running, not while caged/counting down.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        if (arena.gameState != State.INGAME) {
            event.isCancelled = true
        }
    }
}
