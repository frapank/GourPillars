package org.gourmet.gourPillars.api.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

// Fired once the vote closes and the match's event (or none) is picked.
// eventId is the id of the registered GameEventDefinition, or null for no event.
class GourPillarsEventSelectedEvent(
    val arenaName: String,
    val eventId: String?,
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
