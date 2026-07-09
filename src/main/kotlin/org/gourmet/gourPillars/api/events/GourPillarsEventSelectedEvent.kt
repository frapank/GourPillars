package org.gourmet.gourPillars.api.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.gourmet.gourPillars.managers.game.arena.GameEvents

// Fired once the vote closes and the match's event (or none) is picked.
class GourPillarsEventSelectedEvent(
    val arenaName: String,
    val event: GameEvents?,
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
