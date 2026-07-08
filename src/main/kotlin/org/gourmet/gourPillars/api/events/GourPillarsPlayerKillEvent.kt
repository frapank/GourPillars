package org.gourmet.gourPillars.api.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

// Fired when a player eliminates another.
class GourPillarsPlayerKillEvent(
    val arenaName: String,
    val killer: Player,
    val victim: Player,
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
