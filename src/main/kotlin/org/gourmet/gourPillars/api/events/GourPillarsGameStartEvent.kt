package org.gourmet.gourPillars.api.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

// Fired when a match starts.
class GourPillarsGameStartEvent(
    val arenaName: String,
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
