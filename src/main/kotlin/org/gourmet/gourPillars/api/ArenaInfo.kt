package org.gourmet.gourPillars.api

import org.gourmet.gourPillars.managers.game.arena.GameEvents
import org.gourmet.gourPillars.managers.game.arena.State

// alivePlayers/secondsRemaining/currentEvent are only set while state == INGAME.
data class ArenaInfo(
    val name: String,
    val state: State,
    val currentPlayers: Int,
    val maxPlayers: Int,
    val minPlayers: Int,
    val isPrivate: Boolean,
    val alivePlayers: Int? = null,
    val secondsRemaining: Int? = null,
    val currentEvent: GameEvents? = null,
) {
    val isFull: Boolean get() = currentPlayers >= maxPlayers

    val isJoinable: Boolean get() = !isPrivate && !isFull && (state == State.WAITING || state == State.STARTING)
}
