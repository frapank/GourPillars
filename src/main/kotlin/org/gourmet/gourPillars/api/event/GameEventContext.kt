package org.gourmet.gourPillars.api.event

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player

// Read-only view of the arena a GameEventHandler runs in. Main thread only.
// The context stays valid for the whole match; player lists are snapshots taken
// on every access.
interface GameEventContext {
    val arenaName: String

    val world: World

    // The arena's build region (where the pillars are).
    val bounds: ArenaBounds

    // Configured minimum height of the arena (where the floor/kill zone is).
    val minHeight: Int

    // Configured maximum build height of the arena.
    val maxHeight: Int

    // The arena's main spawn (where spectators and eliminated players go).
    val spawnLocation: Location

    // Players still alive in the current match. Empty before the match starts.
    val alivePlayers: List<Player>

    // Everyone in the arena, dead or alive.
    val playersInArena: List<Player>

    // True from match start until it ends. Repeating tasks should stop when this
    // turns false (onStop is also called at that point).
    val isMatchRunning: Boolean

    // Sends a message to every player in the arena.
    fun broadcast(message: Component)
}
