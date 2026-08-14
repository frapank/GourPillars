package org.gourmet.gourPillars.task.game

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.Region
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GameEndFlightTest {
    private lateinit var server: ServerMock
    private var counter = 0

    @BeforeAll
    fun setUpAll() {
        server = MockBukkit.mock()
        MockBukkit.createMockPlugin("PlaceholderAPI")
        MockBukkit.load(GourPillars::class.java)
        GourPillars.spawnManager.setSpawn(server.addSimpleWorld("end-lobby-world").spawnLocation)
    }

    @AfterAll
    fun tearDownAll() {
        MockBukkit.unmock()
    }

    private fun newArena(): Arena {
        val world: World = server.addSimpleWorld("end-arena-world-${counter++}")
        val spawns =
            mutableMapOf<Location, Player?>(
                Location(world, 0.5, 65.0, 0.5) to null,
                Location(world, 5.5, 65.0, 0.5) to null,
            )
        val regionOne = Location(world, -20.0, 40.0, -20.0)
        val regionTwo = Location(world, 30.0, 120.0, 30.0)

        return Arena(
            spawnMap = spawns,
            spawnMainLocation = Location(world, 20.0, 65.0, 0.0),
            isPrivate = false,
            slowFallingTime = 1,
            maxPlayer = 2,
            minPlayer = 2,
            maxHeight = 120,
            minHeight = 40,
            regionLocOne = regionOne,
            regionLocTwo = regionTwo,
            region = Region.createRegion(regionOne, regionTwo),
            name = "end-arena-${counter++}",
        )
    }

    @Test
    fun `everyone can hover for the seconds between the last elimination and the lobby`() {
        val arena = newArena()
        val winner = server.addPlayer()
        val loser = server.addPlayer()
        arena.inGamePlayer.addAll(listOf(winner, loser))
        arena.gameTask.alivePlayer = mutableSetOf(winner, loser)
        arena.gameTask.playerKills = mutableMapOf(winner to 1, loser to 0)
        arena.gameState = State.INGAME

        arena.gameTask.playerEliminated(loser)

        assertTrue(winner.allowFlight, "the winner must not drop off their pillar while the match wraps up")

        // ...and it is taken back when they are sent to the lobby, four seconds later.
        // The very last step of that runnable is the world reset, which MockBukkit cannot do
        // (no world container): everything asserted below happens before it, so the failure of
        // that last step is swallowed here instead of aborting the test.
        runCatching { server.scheduler.performTicks(81) }

        assertFalse(winner.allowFlight, "flight must not follow the winner into the lobby")
        assertFalse(winner.isFlying)
    }
}
