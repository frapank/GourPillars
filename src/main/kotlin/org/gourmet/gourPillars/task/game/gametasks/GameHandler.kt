package org.gourmet.gourPillars.task.game.gametasks

import org.bukkit.entity.Player
import org.gourmet.gourPillars.managers.arena.Arena

interface GameHandler {
    fun onStart(arena: Arena)
    fun onStop(arena: Arena, winner: Player?)
}
