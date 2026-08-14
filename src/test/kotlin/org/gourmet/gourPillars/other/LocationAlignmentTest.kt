package org.gourmet.gourPillars.other

import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocationAlignmentTest {
    private lateinit var server: ServerMock
    private lateinit var world: World

    @BeforeAll
    fun setUpAll() {
        server = MockBukkit.mock()
        world = server.addSimpleWorld("alignment-world")
    }

    @AfterAll
    fun tearDownAll() {
        MockBukkit.unmock()
    }

    private fun at(
        x: Double,
        y: Double,
        z: Double,
        yaw: Float = 0f,
        pitch: Float = 0f,
    ) = Location(world, x, y, z, yaw, pitch)

    @Test
    fun `the position is centred on the block it stands on`() {
        val aligned = LocationAlignment.align(at(10.13, 65.0, 20.87))

        assertEquals(10.5, aligned.location.x)
        assertEquals(20.5, aligned.location.z)
        assertEquals(65.0, aligned.location.y)
        assertTrue(aligned.changes.any { it.contains("centred") })
    }

    @Test
    fun `a view already close to a cardinal direction is squared up`() {
        val aligned = LocationAlignment.align(at(0.5, 65.0, 0.5, yaw = 84f))

        assertEquals(90f, aligned.location.yaw)
        assertTrue(aligned.changes.any { it.contains("west") }, "90 degrees of yaw is west")
    }

    @Test
    fun `a deliberately odd angle is left exactly as it is`() {
        val aligned = LocationAlignment.align(at(0.5, 65.0, 0.5, yaw = 63f, pitch = 25f))

        assertEquals(63f, aligned.location.yaw, "63 is too far from both 45 and 90 to be a slip")
        assertEquals(25f, aligned.location.pitch)
        assertTrue(aligned.changes.none { it.contains("face") || it.contains("tilted") })
    }

    @Test
    fun `a nearly level view is levelled, a steep one is kept`() {
        assertEquals(0f, LocationAlignment.align(at(0.5, 65.0, 0.5, pitch = 4f)).location.pitch)
        assertEquals(45f, LocationAlignment.align(at(0.5, 65.0, 0.5, pitch = 40f)).location.pitch)
        assertEquals(90f, LocationAlignment.align(at(0.5, 65.0, 0.5, pitch = 87f)).location.pitch)
        assertEquals(20f, LocationAlignment.align(at(0.5, 65.0, 0.5, pitch = 20f)).location.pitch)
    }

    @Test
    fun `a height a hair off a block is rounded, a deliberate one is not`() {
        assertEquals(65.0, LocationAlignment.align(at(0.5, 65.12, 0.5)).location.y)
        assertEquals(65.5, LocationAlignment.align(at(0.5, 65.5, 0.5)).location.y, "half a block up is a choice")
    }

    @Test
    fun `yaw is reported with the compass direction it snapped to`() {
        assertEquals("south", LocationAlignment.facingName(0f))
        assertEquals("west", LocationAlignment.facingName(90f))
        assertEquals("north", LocationAlignment.facingName(180f))
        assertEquals("east", LocationAlignment.facingName(-90f))
        assertEquals("south", LocationAlignment.facingName(720f))
    }

    @Test
    fun `an already aligned location is left untouched`() {
        val tidy = LocationAlignment.align(at(10.13, 65.02, 20.87, yaw = 359.4f, pitch = 1f)).location

        val again = LocationAlignment.align(tidy)

        assertEquals(tidy, again.location)
        assertTrue(again.changes.isEmpty(), "aligning twice must not report a second round of changes")
    }

    @Test
    fun `the original location is never mutated`() {
        val original = at(10.13, 65.02, 20.87, yaw = 88f)

        LocationAlignment.align(original)

        assertEquals(10.13, original.x)
        assertEquals(88f, original.yaw)
    }
}
