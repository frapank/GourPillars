package org.gourmet.gourPillars.api.event

import org.bukkit.entity.Player

// Per-match state of a registered GameEventDefinition. Both callbacks run on the
// main server thread. A handler that throws is detached from the match: the match
// keeps going without the event.
interface GameEventHandler {
    // The match actually started (players released from their cages).
    fun onStart() {}

    // The match ended (winner is null when there is none), or the owning plugin got
    // disabled mid-match. Cancel your tasks and undo any world changes here. Called
    // at most once, and only after onStart.
    fun onStop(winner: Player?) {}

    companion object {
        // For passive events that don't need per-match state.
        @JvmField
        val EMPTY: GameEventHandler = object : GameEventHandler {}
    }
}
