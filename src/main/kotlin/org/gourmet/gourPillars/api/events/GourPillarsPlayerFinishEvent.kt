package org.gourmet.gourPillars.api.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

// Fired per player once their match is over, win or lose.
class GourPillarsPlayerFinishEvent(
    val arenaName: String,
    val player: Player,
    val kills: Int,
    val won: Boolean,
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
