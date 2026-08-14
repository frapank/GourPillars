package org.gourmet.gourPillars.task

import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.World
import org.gourmet.gourPillars.commands.ArenaEdit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArenaEditVisualizerTest {
    private lateinit var server: ServerMock
    private lateinit var world: World

    // The visualizer's own budget (300) plus the small fixed-size markers it adds on top:
    // at most 64 spawn pillars and one death-spawn pillar, ~10 points each.
    private val hardCap = 300 + 65 * 10

    @BeforeAll
    fun setUpAll() {
        server = MockBukkit.mock()
        world = server.addSimpleWorld("visualizer-world")
    }

    @AfterAll
    fun tearDownAll() {
        MockBukkit.unmock()
    }

    private fun sessionWithRegion(
        size: Int,
        height: Int = 80,
    ): ArenaEdit =
        ArenaEdit(
            editorId = UUID.randomUUID(),
            regionLocationOne = Location(world, 0.0, 40.0, 0.0),
            regionLocationSecond = Location(world, size.toDouble(), (40 + height).toDouble(), size.toDouble()),
        )

    @Test
    fun `an empty session draws nothing`() {
        assertTrue(ArenaEditVisualizer.markers(ArenaEdit(UUID.randomUUID())).isEmpty())
    }

    @Test
    fun `a huge region costs no more than a small one`() {
        val small = ArenaEditVisualizer.markers(sessionWithRegion(20)).size
        val huge = ArenaEditVisualizer.markers(sessionWithRegion(5_000, height = 300)).size

        assertTrue(small <= hardCap, "small region drew $small markers")
        assertTrue(huge <= hardCap, "huge region drew $huge markers")
    }

    @Test
    fun `even an absurd spawn count stays bounded`() {
        val session = sessionWithRegion(200)
        repeat(500) { index ->
            session.locations[index] = Location(world, index.toDouble(), 65.0, 0.0)
        }

        assertTrue(ArenaEditVisualizer.markers(session).size <= hardCap)
    }

    @Test
    fun `the height outlines sit on the region corners, which is what gets saved`() {
        val session = sessionWithRegion(40, height = 80)

        val heights = ArenaEditVisualizer.markers(session).map { it.location.y }.toSet()

        assertTrue(heights.contains(40.0), "the red outline marks the floor players die below")
        assertTrue(heights.contains(120.0), "the amber outline marks the ceiling")
    }

    @Test
    fun `spawns and the death spawn get their own colours`() {
        val session = sessionWithRegion(40)
        session.locations[1] = Location(world, 5.0, 65.0, 5.0)
        session.deathSpawn = Location(world, 10.0, 70.0, 10.0)

        val colors = ArenaEditVisualizer.markers(session).map { it.color }.toSet()

        assertTrue(colors.contains(Color.fromRGB(0x69F0AE)), "spawns are green")
        assertTrue(colors.contains(Color.fromRGB(0xE040FB)), "the death spawn is purple")
    }

    @Test
    fun `a single region corner is still shown on its own`() {
        val session =
            ArenaEdit(
                editorId = UUID.randomUUID(),
                regionLocationOne = Location(world, 3.0, 64.0, 3.0),
            )

        val markers = ArenaEditVisualizer.markers(session)

        assertTrue(markers.isNotEmpty())
        assertTrue(markers.all { it.color == Color.fromRGB(0xFFFFFF) })
    }

    @Test
    fun `rendering to the editor works and skips an editor in another world`() {
        val session = sessionWithRegion(40)
        session.deathSpawn = Location(world, 10.0, 70.0, 10.0)
        val editor = server.addPlayer()

        editor.teleport(Location(world, 10.0, 65.0, 10.0))
        ArenaEditVisualizer.render(editor, session)

        // Nothing is sent (and nothing blows up) when the editor walks off to another world.
        editor.teleport(server.addSimpleWorld("visualizer-elsewhere").spawnLocation)
        ArenaEditVisualizer.render(editor, session)
    }

    @Test
    fun `a stray location from another world is never drawn`() {
        val otherWorld = server.addSimpleWorld("visualizer-other-world")
        val session = sessionWithRegion(40)
        // The first spawn decides the arena's world; the second one is the stray.
        session.locations[1] = Location(world, 5.0, 65.0, 5.0)
        session.locations[2] = Location(otherWorld, 5.0, 65.0, 5.0)

        val markers = ArenaEditVisualizer.markers(session)

        assertEquals(listOf(world), markers.map { it.location.world }.distinct())
    }
}
