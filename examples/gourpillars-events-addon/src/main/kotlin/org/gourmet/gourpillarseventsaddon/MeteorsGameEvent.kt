package org.gourmet.gourpillarseventsaddon

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.gourmet.gourPillars.api.event.GameEventContext
import org.gourmet.gourPillars.api.event.GameEventDefinition
import org.gourmet.gourPillars.api.event.GameEventHandler
import org.gourmet.gourPillars.api.event.VoteItemSpec
import kotlin.random.Random

// Rains fireballs around random alive players on a fixed interval.
class MeteorsGameEvent(
    private val plugin: JavaPlugin,
    override val displayName: Component,
    private val lore: List<Component>,
    private val slot: Int,
    private val intervalSeconds: Long,
    private val meteorsPerWave: Int,
    private val explosionPower: Float,
    private val setFire: Boolean,
) : GameEventDefinition {
    override val id: String = "meteors"

    override val voteItem: VoteItemSpec
        get() =
            VoteItemSpec(
                material = Material.FIRE_CHARGE,
                lore = lore,
                preferredSlot = slot,
            )

    override fun createHandler(context: GameEventContext): GameEventHandler = Handler(context)

    private inner class Handler(
        private val context: GameEventContext,
    ) : GameEventHandler {
        private var task: BukkitTask? = null

        override fun onStart() {
            val intervalTicks = intervalSeconds * 20L
            task =
                object : BukkitRunnable() {
                    override fun run() {
                        if (!context.isMatchRunning) {
                            cancel()
                            return
                        }
                        val targets = context.alivePlayers
                        if (targets.isEmpty()) return

                        repeat(meteorsPerWave) {
                            spawnMeteorAbove(targets.random())
                        }
                    }
                }.runTaskTimer(plugin, intervalTicks, intervalTicks)
        }

        override fun onStop(winner: Player?) {
            task?.cancel()
            task = null
        }

        private fun spawnMeteorAbove(target: Player) {
            val spawn =
                target.location.add(
                    Random.nextDouble(-5.0, 5.0),
                    18.0,
                    Random.nextDouble(-5.0, 5.0),
                )
            spawn.y = spawn.y.coerceAtMost(context.world.maxHeight - 1.0)

            val meteor = context.world.spawn(spawn, Fireball::class.java)
            meteor.direction = Vector(0.0, -1.0, 0.0)
            meteor.yield = explosionPower
            meteor.setIsIncendiary(setFire)
        }
    }
}
