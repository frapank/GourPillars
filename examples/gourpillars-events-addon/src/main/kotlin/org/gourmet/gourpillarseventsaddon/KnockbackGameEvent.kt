package org.gourmet.gourpillarseventsaddon

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gourmet.gourPillars.api.GourPillarsAPI
import org.gourmet.gourPillars.api.event.GameEventContext
import org.gourmet.gourPillars.api.event.GameEventDefinition
import org.gourmet.gourPillars.api.event.GameEventHandler
import org.gourmet.gourPillars.api.event.VoteItemSpec

// Passive event: no per-match state, the boost is applied by KnockbackListener
// whenever this event is the active one in the attacker's arena.
class KnockbackGameEvent(
    override val displayName: Component,
    private val lore: List<Component>,
    private val slot: Int,
) : GameEventDefinition {
    override val id: String = ID

    override val voteItem: VoteItemSpec
        get() =
            VoteItemSpec(
                material = Material.PLAYER_HEAD,
                headTexture = HEAD_TEXTURE,
                lore = lore,
                preferredSlot = slot,
            )

    override fun createHandler(context: GameEventContext): GameEventHandler = GameEventHandler.EMPTY

    companion object {
        const val ID = "knockback"
        private const val HEAD_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjA2MWY5OGFhZmYxZTQwNmUwNmY2ZjEzZmZlMDYwMDU4NzNmM2QxZWFkZGIxYjU5ZTE5ZGRhMGVkOWZmYjI3MCJ9fX0="
    }
}

class KnockbackListener(
    private val api: GourPillarsAPI,
    private val multiplier: Double,
) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onPlayerHit(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val victim = event.entity as? Player ?: return

        if (api.getCurrentEventOfPlayer(attacker) != KnockbackGameEvent.ID) return

        val knockbackDirection =
            victim.location
                .toVector()
                .subtract(attacker.location.toVector())
                .normalize()
        victim.velocity = knockbackDirection.multiply(multiplier)
    }
}
