package org.gourmet.gourPillars.api.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

// Fired when a player joins an arena's waiting room.
class GourPillarsPlayerJoinArenaEvent(
    val arenaName: String,
    val player: Player,
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
