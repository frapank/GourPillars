package org.gourmet.gourPillars.api.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.gourmet.gourPillars.managers.game.arena.State

// Fired when an arena's state changes.
class GourPillarsArenaStateChangeEvent(
    val arenaName: String,
    val oldState: State,
    val newState: State,
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
