package org.gourmet.gourPillars.api.events

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.gourmet.gourPillars.api.EliminationCause

// Fired for every elimination, win or lose, with the cause and whoever/whatever caused it.
class GourPillarsPlayerEliminatedEvent(
    val arenaName: String,
    val player: Player,
    val cause: EliminationCause,
    val source: Entity?,
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
