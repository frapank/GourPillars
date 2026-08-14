package org.gourmet.gourPillars.managers.game.arena

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Region
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArenaSpawnTest {
    private lateinit var server: ServerMock
    private var counter = 0

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

    private fun newWorld(): World = server.addSimpleWorld("spawn-world-${counter++}")

    private fun newArena(world: World): Arena {
        val spawns =
            mutableMapOf<Location, Player?>(
                Location(world, 0.5, 65.0, 0.5) to null,
                Location(world, 5.5, 65.0, 0.5) to null,
                Location(world, 10.5, 65.0, 0.5) to null,
            )
        val regionOne = Location(world, -20.0, 40.0, -20.0)
        val regionTwo = Location(world, 30.0, 120.0, 30.0)

        return Arena(
            spawnMap = spawns,
            spawnMainLocation = Location(world, 20.0, 65.0, 0.0),
            isPrivate = false,
            slowFallingTime = 1,
            maxPlayer = 3,
            minPlayer = 2,
            maxHeight = 120,
            minHeight = 40,
            regionLocOne = regionOne,
            regionLocTwo = regionTwo,
            region = Region.createRegion(regionOne, regionTwo),
            name = "spawn-arena-${counter++}",
        )
    }

    @Test
    fun `every player claims a different spawn`() {
        val arena = newArena(newWorld())

        val claimed = (1..3).map { arena.claimFreeSpawn(server.addPlayer()) }

        assertTrue(claimed.all { it != null })
        assertEquals(3, claimed.toSet().size, "three players must end up on three different pillars")
        assertNull(arena.claimFreeSpawn(server.addPlayer()), "a full arena hands out no spawn")
    }

    /**
     * The reset unloads the arena world and creates it again, so the arena ends up holding a stale
     * World instance. Rebinding used to edit the world of each spawn *key in place*, which changes
     * its hash while it sits in the map: from the second match on, every player was handed the same
     * "still free" spawn and the whole game started stacked on one pillar.
     */
    @Test
    fun `spawns still work after the arena is rebound to a freshly reset world`() {
        val arena = newArena(newWorld())
        arena.claimFreeSpawn(server.addPlayer())
        arena.claimFreeSpawn(server.addPlayer())
        arena.releaseAllSpawns()

        val restoredWorld = newWorld()
        arena.rebindToWorld(restoredWorld)

        val first = arena.claimFreeSpawn(server.addPlayer())
        val second = arena.claimFreeSpawn(server.addPlayer())
        val third = arena.claimFreeSpawn(server.addPlayer())

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(3, setOf(first, second, third).size, "each player must still get their own pillar")
        assertEquals(3, arena.spawnMap.size, "the rebind must not leave duplicated spawn entries behind")
        assertTrue(arena.spawnMap.keys.all { it.world == restoredWorld }, "every spawn must point at the new world")
    }

    @Test
    fun `rebinding moves the whole arena to the new world and frees every spawn`() {
        val arena = newArena(newWorld())
        arena.claimFreeSpawn(server.addPlayer())

        val restoredWorld = newWorld()
        arena.rebindToWorld(restoredWorld)

        assertTrue(arena.spawnMap.values.all { it == null }, "a reset arena starts with every spawn free")
        assertEquals(restoredWorld, arena.spawnMainLocation.world)
        assertEquals(restoredWorld, arena.regionLocOne.world)
        assertEquals(restoredWorld, arena.regionLocTwo.world)
        assertEquals(restoredWorld, arena.region.world)
    }

    @Test
    fun `the spawn coordinates survive the rebind`() {
        val arena = newArena(newWorld())
        val before =
            arena.spawnMap.keys
                .map { Triple(it.x, it.y, it.z) }
                .toSet()

        arena.rebindToWorld(newWorld())

        assertEquals(
            before,
            arena.spawnMap.keys
                .map { Triple(it.x, it.y, it.z) }
                .toSet(),
        )
    }

    @Test
    fun `leaving frees exactly the spawn that player held`() {
        val arena = newArena(newWorld())
        val staying = server.addPlayer()
        val leaving = server.addPlayer()
        val stayingSpawn = arena.claimFreeSpawn(staying)
        arena.claimFreeSpawn(leaving)

        arena.releaseSpawn(leaving)

        assertEquals(1, arena.spawnMap.values.count { it == staying })
        assertEquals(0, arena.spawnMap.values.count { it == leaving })
        assertEquals(
            stayingSpawn,
            arena.spawnMap.entries
                .first { it.value == staying }
                .key
                .let { arena.cageLocation(it) },
        )
    }
}
