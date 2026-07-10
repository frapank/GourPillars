package org.gourmet.gourPillars.listener.game

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.GameEvents

class KnockbackListener : Listener {
    val arenaManager = GourPillars.arenaManager
    private val knockbackMultiplier = GourPillars.instance.config.getDouble("game.knockback-multiplier", 2.0)

    @EventHandler(ignoreCancelled = true)
    fun onPlayerHit(event: EntityDamageByEntityEvent) {
        val attacker = event.damager
        val victim = event.entity

        if (attacker !is Player || victim !is Player) return

        val arena = arenaManager.getArenaByPlayer(attacker) ?: return
        val gameEvent = arena.gameEvent

        if (gameEvent != GameEvents.KNOCKBACK) return

        val knockbackDirection =
            victim.location
                .toVector()
                .subtract(attacker.location.toVector())
                .normalize()
        victim.velocity = knockbackDirection.multiply(knockbackMultiplier)
    }
}
