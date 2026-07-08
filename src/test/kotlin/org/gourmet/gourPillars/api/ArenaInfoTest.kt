package org.gourmet.gourPillars.api

import org.gourmet.gourPillars.managers.game.arena.State
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArenaInfoTest {
    private fun info(
        state: State = State.WAITING,
        current: Int = 0,
        max: Int = 4,
        isPrivate: Boolean = false,
    ) = ArenaInfo(name = "test", state = state, currentPlayers = current, maxPlayers = max, minPlayers = 2, isPrivate = isPrivate)

    @Test
    fun `isFull is true once currentPlayers reaches maxPlayers`() {
        assertFalse(info(current = 3, max = 4).isFull)
        assertTrue(info(current = 4, max = 4).isFull)
    }

    @Test
    fun `isJoinable requires public, not full and waiting or starting`() {
        assertTrue(info(state = State.WAITING).isJoinable)
        assertTrue(info(state = State.STARTING).isJoinable)
        assertFalse(info(state = State.INGAME).isJoinable)
        assertFalse(info(state = State.STOPPED).isJoinable)
        assertFalse(info(current = 4, max = 4).isJoinable)
        assertFalse(info(isPrivate = true).isJoinable)
    }
}
