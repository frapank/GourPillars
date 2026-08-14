package org.gourmet.gourPillars.other

import org.bukkit.GameMode
import org.gourmet.gourPillars.GourPillars
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtilsTest {
    private lateinit var server: ServerMock

    @BeforeAll
    fun setUpAll() {
        server = MockBukkit.mock()
        MockBukkit.createMockPlugin("PlaceholderAPI")
        MockBukkit.load(GourPillars::class.java)
    }

    @AfterAll
    fun tearDownAll() {
        MockBukkit.unmock()
    }

    @Test
    fun `flight is granted and taken back`() {
        val player = server.addPlayer()

        Utils.setFlight(player, true)
        assertTrue(player.allowFlight)
        assertTrue(player.isFlying)

        Utils.setFlight(player, false)
        assertFalse(player.allowFlight)
        assertFalse(player.isFlying)
    }

    @Test
    fun `resetting a survival player clears the flight the end of a game gave them`() {
        val player = server.addPlayer()
        player.gameMode = GameMode.SURVIVAL
        Utils.setFlight(player, true)

        Utils.resetPlayerState(player)

        assertFalse(player.allowFlight, "flight must not follow a player back into the lobby")
        assertFalse(player.isFlying)
    }

    @Test
    fun `resetting leaves the flight of creative and spectator alone`() {
        val creative = server.addPlayer()
        creative.gameMode = GameMode.CREATIVE
        Utils.setFlight(creative, true)

        val spectator = server.addPlayer()
        spectator.gameMode = GameMode.SPECTATOR
        Utils.setFlight(spectator, true)

        Utils.resetPlayerState(creative)
        Utils.resetPlayerState(spectator)

        assertTrue(creative.allowFlight, "creative flight comes from the gamemode, not from us")
        assertTrue(spectator.allowFlight)
    }
}
