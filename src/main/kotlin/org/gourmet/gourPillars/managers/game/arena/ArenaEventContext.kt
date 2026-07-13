package org.gourmet.gourPillars.managers.game.arena

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.gourmet.gourPillars.api.event.ArenaBounds
import org.gourmet.gourPillars.api.event.GameEventContext

// The GameEventContext handed to registered event handlers, backed by the live arena.
class ArenaEventContext(
    private val arena: Arena,
) : GameEventContext {
    override val arenaName: String get() = arena.name

    override val world: World get() = arena.region.world

    override val bounds: ArenaBounds
        get() =
            ArenaBounds(
                minX = arena.region.minX,
                minY = arena.region.minY,
                minZ = arena.region.minZ,
                maxX = arena.region.maxX,
                maxY = arena.region.maxY,
                maxZ = arena.region.maxZ,
            )

    override val minHeight: Int get() = arena.minHeight

    override val maxHeight: Int get() = arena.maxHeight

    override val spawnLocation: Location get() = arena.spawnMainLocation.clone()

    override val alivePlayers: List<Player> get() = arena.gameTask.alivePlayersSnapshot()

    override val playersInArena: List<Player> get() = arena.inGamePlayer.toList()

    override val isMatchRunning: Boolean get() = arena.gameTask.isMatchRunning()

    override fun broadcast(message: Component) {
        arena.inGamePlayer.forEach { it.sendMessage(message) }
    }
}
