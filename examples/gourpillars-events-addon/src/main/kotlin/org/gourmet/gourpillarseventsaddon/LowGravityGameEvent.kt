package org.gourmet.gourpillarseventsaddon

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.gourmet.gourPillars.api.event.GameEventContext
import org.gourmet.gourPillars.api.event.GameEventDefinition
import org.gourmet.gourPillars.api.event.GameEventHandler
import org.gourmet.gourPillars.api.event.VoteItemSpec

// Gives every player slow falling and a jump boost for the whole match.
class LowGravityGameEvent(
    override val displayName: Component,
    private val lore: List<Component>,
    private val slot: Int,
    private val jumpBoostLevel: Int,
) : GameEventDefinition {
    override val id: String = "low-gravity"

    override val voteItem: VoteItemSpec
        get() =
            VoteItemSpec(
                material = Material.FEATHER,
                lore = lore,
                preferredSlot = slot,
            )

    override fun createHandler(context: GameEventContext): GameEventHandler = Handler(context)

    private inner class Handler(
        private val context: GameEventContext,
    ) : GameEventHandler {
        override fun onStart() {
            context.alivePlayers.forEach { player ->
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, PotionEffect.INFINITE_DURATION, 0, false, false))
                if (jumpBoostLevel > 0) {
                    player.addPotionEffect(
                        PotionEffect(PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, jumpBoostLevel - 1, false, false),
                    )
                }
            }
        }

        override fun onStop(winner: Player?) {
            context.playersInArena.forEach { player ->
                player.removePotionEffect(PotionEffectType.SLOW_FALLING)
                player.removePotionEffect(PotionEffectType.JUMP_BOOST)
            }
        }
    }
}
