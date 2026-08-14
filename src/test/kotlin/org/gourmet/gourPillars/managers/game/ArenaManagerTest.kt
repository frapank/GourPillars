package org.gourmet.gourPillars.managers.game

import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.State
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArenaManagerTest {
    private lateinit var server: ServerMock
    private var worldCounter = 0

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

    @AfterEach
    fun tearDownEach() {
        GourPillars.arenaManager.onlineArenas.clear()
        arenasFolder().listFiles()?.forEach { it.delete() }
    }

    private fun arenasFolder(): File = File(GourPillars.instance.dataFolder, "arenas").apply { mkdirs() }

    /** Writes a minimal but valid arena file, letting the caller drop/override single options. */
    private fun writeArenaFile(
        name: String,
        world: World,
        configure: YamlConfiguration.() -> Unit = {},
    ) {
        val config = YamlConfiguration()
        config.set("world", world.name)
        config.set("private-arena", false)
        config.set("min-players", 2)
        config.set("min-height", 40)
        config.set("max-height", 120)
        config.set("slow-falling-time", 1)
        config.set("spawn-height", 0)

        config.set("main-spawn.x", 8.0)
        config.set("main-spawn.y", 70.0)
        config.set("main-spawn.z", 8.0)

        config.set("region.loc-1.x", -20.0)
        config.set("region.loc-1.y", 40.0)
        config.set("region.loc-1.z", -20.0)
        config.set("region.loc-2.x", 20.0)
        config.set("region.loc-2.y", 120.0)
        config.set("region.loc-2.z", 20.0)

        config.set("spawns.1.x", 0.0)
        config.set("spawns.1.y", 65.0)
        config.set("spawns.1.z", 0.0)
        config.set("spawns.2.x", 5.0)
        config.set("spawns.2.y", 65.0)
        config.set("spawns.2.z", 0.0)

        config.configure()
        config.save(File(arenasFolder(), "$name.yml"))
    }

    private fun newWorld(): World = server.addSimpleWorld("arena-world-${worldCounter++}")

    @Test
    fun `max-height is read from the arena file instead of being hardcoded`() {
        val world = newWorld()
        writeArenaFile("heights", world)

        assertEquals(ArenaReloadResult.RELOADED, GourPillars.arenaManager.reloadArena("heights"))

        val arena = GourPillars.arenaManager.getArenaByName("heights")
        assertNotNull(arena)
        assertEquals(120, arena!!.maxHeight)
        assertEquals(40, arena.minHeight)
    }

    @Test
    fun `a missing height falls back to the world limits, not to zero`() {
        val world = newWorld()
        writeArenaFile("no-heights", world) {
            set("min-height", null)
            set("max-height", null)
        }

        assertEquals(ArenaReloadResult.RELOADED, GourPillars.arenaManager.reloadArena("no-heights"))

        val arena = GourPillars.arenaManager.getArenaByName("no-heights")
        assertNotNull(arena)
        assertEquals(world.minHeight, arena!!.minHeight)
        assertEquals(world.maxHeight, arena.maxHeight)
    }

    @Test
    fun `reloading picks up the edited file without a restart`() {
        val world = newWorld()
        writeArenaFile("reloaded", world)
        GourPillars.arenaManager.reloadArena("reloaded")
        assertEquals(2, GourPillars.arenaManager.getArenaByName("reloaded")?.minPlayer)

        writeArenaFile("reloaded", world) { set("min-players", 4) }

        assertEquals(ArenaReloadResult.RELOADED, GourPillars.arenaManager.reloadArena("reloaded"))
        assertEquals(4, GourPillars.arenaManager.getArenaByName("reloaded")?.minPlayer)
    }

    @Test
    fun `an arena in use is not swapped from under its players`() {
        val world = newWorld()
        writeArenaFile("busy", world)
        GourPillars.arenaManager.reloadArena("busy")

        val arena = GourPillars.arenaManager.getArenaByName("busy")!!
        arena.inGamePlayer.add(server.addPlayer())

        assertEquals(ArenaReloadResult.BUSY, GourPillars.arenaManager.reloadArena("busy"))

        arena.inGamePlayer.clear()
        arena.gameState = State.INGAME
        assertEquals(ArenaReloadResult.BUSY, GourPillars.arenaManager.reloadArena("busy"))
    }

    @Test
    fun `reloading an unknown arena reports it instead of failing`() {
        assertEquals(ArenaReloadResult.NOT_FOUND, GourPillars.arenaManager.reloadArena("does-not-exist"))
        assertNull(GourPillars.arenaManager.getArenaByName("does-not-exist"))
    }

    @Test
    fun `a file without spawns is rejected rather than loaded half-broken`() {
        val world = newWorld()
        writeArenaFile("no-spawns", world) { set("spawns", null) }

        assertEquals(ArenaReloadResult.INVALID, GourPillars.arenaManager.reloadArena("no-spawns"))
        assertNull(GourPillars.arenaManager.getArenaByName("no-spawns"))
    }
}
