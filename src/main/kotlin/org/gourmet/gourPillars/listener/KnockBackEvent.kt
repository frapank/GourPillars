package org.gourmet.gourPillars.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.metadata.FixedMetadataValue
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.GameEvents

class KnockBackEvent : Listener {

    val arenaManager = GourPillars.arenaManager
    private val knockbackMultiplier = GourPillars.instance.config.getDouble("game.knockback-multiplier", 2.0)

    @EventHandler
    fun onPlayerHit(event: EntityDamageByEntityEvent) {
        val attacker = event.damager
        val victim = event.entity

        if (attacker !is Player || victim !is Player) return

        val arena = arenaManager.getArenaByPlayer(attacker) ?: return
        val gameEvent = arena.gameEvent

        if (gameEvent != GameEvents.KNOCKBACK) return

        val hitCountKey = "knockback_hits"
        val hitCount = attacker.getMetadata(hitCountKey).firstOrNull()?.asInt() ?: 0

        val knockbackDirection = victim.location.toVector().subtract(attacker.location.toVector()).normalize()
        victim.velocity = knockbackDirection.multiply(knockbackMultiplier)

        attacker.setMetadata(hitCountKey, FixedMetadataValue(GourPillars.instance, hitCount + 1))
    }
}