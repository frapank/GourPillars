package org.gourmet.gourPillars.listener.lobby

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.block.BlockBreakEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.SpawnManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LobbyChecksTest {
    private lateinit var server: ServerMock

    @BeforeAll
    fun setUpAll() {
        server = MockBukkit.mock()
        MockBukkit.createMockPlugin("PlaceholderAPI")
        MockBukkit.load(GourPillars::class.java)
        val world = server.addSimpleWorld("lobby-world")
        GourPillars.spawnManager.setSpawn(world.spawnLocation)
    }

    @AfterAll
    fun tearDownAll() {
        MockBukkit.unmock()
    }

    @AfterEach
    fun tearDownEach() {
        GourPillars.instance.config.set("lobby.enabled", true)
        GourPillars.instance.config.set("lobby.checks.world-protection", true)
        GourPillars.instance.config.set("lobby.checks.void-teleport-to-spawn", true)
    }

    @Test
    fun `world protection cancels block breaking in the lobby world`() {
        val player = server.addPlayer()
        val world = GourPillars.spawnManager.getConfiguredWorld()!!
        val block = world.getBlockAt(0, 64, 0)
        block.type = Material.STONE

        val event = BlockBreakEvent(block, player)
        server.pluginManager.callEvent(event)

        assertTrue(event.isCancelled)
    }

    @Test
    fun `disabling world-protection lets blocks break in the lobby world`() {
        GourPillars.instance.config.set("lobby.checks.world-protection", false)

        val player = server.addPlayer()
        val world = GourPillars.spawnManager.getConfiguredWorld()!!
        val block = world.getBlockAt(1, 64, 0)
        block.type = Material.STONE

        val event = BlockBreakEvent(block, player)
        server.pluginManager.callEvent(event)

        assertFalse(event.isCancelled)
    }

    @Test
    fun `disabling lobby entirely lets blocks break too`() {
        GourPillars.instance.config.set("lobby.enabled", false)

        val player = server.addPlayer()
        val world = GourPillars.spawnManager.getConfiguredWorld()!!
        val block = world.getBlockAt(2, 64, 0)
        block.type = Material.STONE

        val event = BlockBreakEvent(block, player)
        server.pluginManager.callEvent(event)

        assertFalse(event.isCancelled)
    }

    @Test
    fun `void-teleport-to-spawn sends a lobby player who falls below the world back to spawn`() {
        val player = server.addPlayer()
        val world = GourPillars.spawnManager.getConfiguredWorld()!!
        val spawn = SpawnManager.spawn!!
        player.teleport(Location(world, 5.0, 10.0, 5.0))

        player.simulatePlayerMove(Location(world, 5.0, world.minHeight.toDouble(), 5.0))

        assertEquals(spawn.blockX, player.location.blockX)
        assertEquals(spawn.blockY, player.location.blockY)
        assertEquals(spawn.blockZ, player.location.blockZ)
    }

    @Test
    fun `disabling void-teleport-to-spawn leaves a falling lobby player alone`() {
        GourPillars.instance.config.set("lobby.checks.void-teleport-to-spawn", false)

        val player = server.addPlayer()
        val world = GourPillars.spawnManager.getConfiguredWorld()!!
        val target = Location(world, 6.0, world.minHeight.toDouble(), 6.0)
        player.teleport(Location(world, 6.0, 10.0, 6.0))

        player.simulatePlayerMove(target)

        assertEquals(target.blockY, player.location.blockY)
    }
}
