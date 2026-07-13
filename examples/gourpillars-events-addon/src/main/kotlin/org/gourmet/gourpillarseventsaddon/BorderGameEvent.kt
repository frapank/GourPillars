package org.gourmet.gourpillarseventsaddon

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.gourmet.gourPillars.api.event.GameEventContext
import org.gourmet.gourPillars.api.event.GameEventDefinition
import org.gourmet.gourPillars.api.event.GameEventHandler
import org.gourmet.gourPillars.api.event.VoteItemSpec

// Shrinks the world border around the arena until it reaches finalSize, damaging
// players caught outside. The original border is restored when the match ends.
class BorderGameEvent(
    private val plugin: JavaPlugin,
    override val displayName: Component,
    private val lore: List<Component>,
    private val slot: Int,
    private val finalSize: Double,
    private val shrinkIntervalSeconds: Long,
    private val damageAmount: Double,
) : GameEventDefinition {
    override val id: String = "border"

    override val voteItem: VoteItemSpec
        get() =
            VoteItemSpec(
                material = Material.PLAYER_HEAD,
                headTexture = HEAD_TEXTURE,
                lore = lore,
                preferredSlot = slot,
            )

    override fun createHandler(context: GameEventContext): GameEventHandler = Handler(context)

    private inner class Handler(
        private val context: GameEventContext,
    ) : GameEventHandler {
        private var task: BukkitTask? = null
        private var originalCenter: Location? = null
        private var originalSize: Double = 0.0

        override fun onStart() {
            val world = context.world
            val border = world.worldBorder

            originalCenter = border.center
            originalSize = border.size

            val bounds = context.bounds
            border.center = Location(world, bounds.centerX, 0.0, bounds.centerZ)

            val initialSize =
                maxOf(
                    bounds.maxX - bounds.minX,
                    bounds.maxZ - bounds.minZ,
                ).toDouble() + 2.0
            border.size = initialSize
            border.damageBuffer = 0.0
            border.damageAmount = damageAmount

            task =
                object : BukkitRunnable() {
                    override fun run() {
                        if (!context.isMatchRunning || border.size <= finalSize) {
                            cancel()
                            return
                        }
                        border.size = border.size - 1.0
                    }
                }.runTaskTimer(plugin, 0L, shrinkIntervalSeconds * 20L)
        }

        override fun onStop(winner: Player?) {
            task?.cancel()
            task = null

            val border = context.world.worldBorder
            originalCenter?.let { center ->
                border.center = center
                border.size = originalSize
            }
            originalCenter = null
        }
    }

    private companion object {
        const val HEAD_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjk1MGNiNjVlNDFiMWQ2NjhjZjg4MmViODk4NWUwM2UxNzY2MTc5NTU2NjM2NDA3MTRmYzA4ZmJlZTFmMWE0NiJ9fX0="
    }
}
