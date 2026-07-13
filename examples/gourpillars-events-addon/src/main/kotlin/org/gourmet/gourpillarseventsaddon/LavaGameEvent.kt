package org.gourmet.gourpillarseventsaddon

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.gourmet.gourPillars.api.event.GameEventContext
import org.gourmet.gourPillars.api.event.GameEventDefinition
import org.gourmet.gourPillars.api.event.GameEventHandler
import org.gourmet.gourPillars.api.event.VoteItemSpec

// Fills the arena with a lava floor that rises by one block on a fixed interval.
class LavaGameEvent(
    private val plugin: JavaPlugin,
    override val displayName: Component,
    private val lore: List<Component>,
    private val slot: Int,
    private val riseIntervalSeconds: Long,
) : GameEventDefinition {
    override val id: String = "lava"

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

        override fun onStart() {
            var lavaLevel = context.minHeight
            task =
                object : BukkitRunnable() {
                    override fun run() {
                        if (!context.isMatchRunning) {
                            cancel()
                            return
                        }
                        fillLavaLayer(lavaLevel)
                        lavaLevel++
                    }
                }.runTaskTimer(plugin, 0L, riseIntervalSeconds * 20L)
        }

        override fun onStop(winner: Player?) {
            task?.cancel()
            task = null
        }

        // GourPillars resets the arena blocks after every match, so the lava doesn't
        // need to be cleaned up here.
        private fun fillLavaLayer(y: Int) {
            val world = context.world
            if (y < world.minHeight || y > world.maxHeight) return

            val bounds = context.bounds
            for (x in bounds.minX..bounds.maxX) {
                for (z in bounds.minZ..bounds.maxZ) {
                    world.getBlockAt(x, y, z).type = Material.LAVA
                }
            }
        }
    }

    private companion object {
        const val HEAD_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjY5YTMyY2ZmZjAzMTU1YjlmODEwOTg1OGQ4MzAzYjA2ZmU3MGQwYjUzNWJhNjRiNTFkMDMwMmZmMzM5ZTBjYiJ9fX0="
    }
}
