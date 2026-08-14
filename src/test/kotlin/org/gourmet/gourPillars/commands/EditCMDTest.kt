package org.gourmet.gourPillars.commands

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.gourmet.gourPillars.GourPillars
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.PlayerMock
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EditCMDTest {
    private lateinit var server: ServerMock
    private lateinit var arenaWorld: World
    private lateinit var player: PlayerMock
    private var counter = 0

    @BeforeAll
    fun setUpAll() {
        server = MockBukkit.mock()
        MockBukkit.createMockPlugin("PlaceholderAPI")
        MockBukkit.load(GourPillars::class.java)
        // The editor must never be standing in the lobby world while building an arena.
        GourPillars.spawnManager.setSpawn(server.addSimpleWorld("edit-lobby-world").spawnLocation)
    }

    @AfterAll
    fun tearDownAll() {
        MockBukkit.unmock()
    }

    @BeforeEach
    fun setUpEach() {
        arenaWorld = server.addSimpleWorld("edit-arena-world-${counter++}")
        player = server.addPlayer()
        player.teleport(Location(arenaWorld, 0.0, 65.0, 0.0))
    }

    @AfterEach
    fun tearDownEach() {
        // Also resets GourPillars.isEditing, so a leftover session can't leak into the next test.
        EditCMD.handleQuit(player)
        GourPillars.arenaManager.onlineArenas.clear()
        arenasFolder().listFiles()?.forEach { it.delete() }
    }

    private fun arenasFolder(): File = File(GourPillars.instance.dataFolder, "arenas").apply { mkdirs() }

    private fun arenaFile(name: String): File = File(arenasFolder(), "$name.yml")

    private fun at(
        x: Double,
        y: Double,
        z: Double,
    ) = player.teleport(Location(arenaWorld, x, y, z))

    /** Everything a save needs except the heights, which are what most of these tests are about. */
    private fun buildMinimalArena(name: String) {
        EditCMD.startEditing(player)
        EditCMD.setName(player, name)
        at(0.0, 65.0, 0.0)
        EditCMD.setSpawn(player, 1)
        at(5.0, 65.0, 0.0)
        EditCMD.setSpawn(player, 2)
        at(-20.0, 40.0, -20.0)
        EditCMD.setRegionOne(player)
        at(20.0, 120.0, 20.0)
        EditCMD.setRegionTwo(player)
        at(8.0, 70.0, 8.0)
        EditCMD.setDeathSpawn(player)
    }

    @Test
    fun `heights left unset are taken from the region instead of being dropped`() {
        buildMinimalArena("derived")

        EditCMD.saveArena(player)

        val saved = YamlConfiguration.loadConfiguration(arenaFile("derived"))
        assertEquals(40, saved.getInt("min-height"))
        assertEquals(120, saved.getInt("max-height"))
        assertTrue(saved.isSet("min-height"), "the void-kill level must always end up in the file")
    }

    @Test
    fun `moving a corner moves the heights with it`() {
        buildMinimalArena("moved")
        at(-20.0, 55.0, -20.0)
        EditCMD.setRegionOne(player)

        EditCMD.saveArena(player)

        val saved = YamlConfiguration.loadConfiguration(arenaFile("moved"))
        assertEquals(55, saved.getInt("min-height"))
        assertEquals(120, saved.getInt("max-height"))
    }

    @Test
    fun `the saved arena is loaded right away, heights included`() {
        buildMinimalArena("live")

        EditCMD.saveArena(player)

        val arena = GourPillars.arenaManager.getArenaByName("live")
        assertNotNull(arena)
        assertEquals(40, arena!!.minHeight)
        assertEquals(120, arena.maxHeight)
        assertEquals(arenaWorld.name, arena.spawnMainLocation.world.name)
    }

    @Test
    fun `a missing death spawn blocks the save entirely`() {
        EditCMD.startEditing(player)
        EditCMD.setName(player, "incomplete")
        EditCMD.setSpawn(player, 1)
        at(5.0, 65.0, 0.0)
        EditCMD.setSpawn(player, 2)
        at(-20.0, 40.0, -20.0)
        EditCMD.setRegionOne(player)
        at(20.0, 120.0, 20.0)
        EditCMD.setRegionTwo(player)

        EditCMD.saveArena(player)

        assertFalse(arenaFile("incomplete").exists(), "an arena without a death spawn must not be written")
    }

    @Test
    fun `min-players above the spawn count is refused`() {
        buildMinimalArena("too-few-spawns")
        EditCMD.setMinPlayers(player, 8)

        EditCMD.saveArena(player)

        assertFalse(arenaFile("too-few-spawns").exists())
    }

    @Test
    fun `locations from another world are refused instead of silently remapped`() {
        val otherWorld = server.addSimpleWorld("edit-other-world-${counter++}")
        buildMinimalArena("one-world")

        player.teleport(Location(otherWorld, 0.0, 65.0, 0.0))
        EditCMD.setDeathSpawn(player)
        EditCMD.setSpawn(player, 3)

        EditCMD.saveArena(player)

        val saved = YamlConfiguration.loadConfiguration(arenaFile("one-world"))
        assertEquals(arenaWorld.name, saved.getString("world"))
        assertEquals(8.5, saved.getDouble("main-spawn.x"), "the death spawn must be the one set in the arena world")
        assertFalse(saved.isSet("spawns.3"), "the spawn set in another world must not be stored")
    }

    @Test
    fun `an arena in the lobby world is refused`() {
        EditCMD.startEditing(player)
        EditCMD.setName(player, "lobby-arena")
        player.teleport(GourPillars.spawnManager.getConfiguredWorld()!!.spawnLocation)

        EditCMD.setSpawn(player, 1)
        EditCMD.setDeathSpawn(player)
        EditCMD.saveArena(player)

        assertFalse(arenaFile("lobby-arena").exists())
    }

    @Test
    fun `an existing arena can be reopened and keeps the values it already had`() {
        buildMinimalArena("reopen")
        EditCMD.setSpawnHeight(player, 3)
        EditCMD.setFallingTime(player, 5)
        EditCMD.saveArena(player)
        EditCMD.stopEditing(player)

        EditCMD.loadExisting(player, "reopen")
        at(9.0, 66.0, 9.0)
        EditCMD.setSpawn(player, 3)
        EditCMD.saveArena(player)

        val saved = YamlConfiguration.loadConfiguration(arenaFile("reopen"))
        assertEquals(3, saved.getInt("spawn-height"), "an option not touched this session must survive the re-save")
        assertEquals(5, saved.getInt("slow-falling-time"))
        assertEquals(40, saved.getInt("min-height"))
        assertTrue(saved.isSet("spawns.3"))
    }

    @Test
    fun `the heights keep following the corners after reopening the arena`() {
        buildMinimalArena("following")
        EditCMD.saveArena(player)
        EditCMD.stopEditing(player)

        EditCMD.loadExisting(player, "following")
        at(-20.0, 55.0, -20.0)
        EditCMD.setRegionOne(player)
        EditCMD.saveArena(player)

        val saved = YamlConfiguration.loadConfiguration(arenaFile("following"))
        assertEquals(55, saved.getInt("min-height"), "moving the lower corner must move the void-kill level with it")
        assertEquals(120, saved.getInt("max-height"))
    }

    @Test
    fun `heights hand-written in a file are replaced by the region ones on save`() {
        buildMinimalArena("hand-written")
        EditCMD.saveArena(player)
        EditCMD.stopEditing(player)
        // What someone editing arenas/hand-written.yml by hand could leave behind.
        val file = arenaFile("hand-written")
        val edited = YamlConfiguration.loadConfiguration(file)
        edited.set("min-height", 12)
        edited.save(file)

        EditCMD.loadExisting(player, "hand-written")
        EditCMD.saveArena(player)

        val saved = YamlConfiguration.loadConfiguration(file)
        assertEquals(40, saved.getInt("min-height"), "the region floor is the single source of the void-kill level")
    }

    @Test
    fun `an arena is savable without ever touching the heights`() {
        buildMinimalArena("no-heights")

        EditCMD.saveArena(player)

        val saved = YamlConfiguration.loadConfiguration(arenaFile("no-heights"))
        assertEquals(40, saved.getInt("min-height"))
        assertEquals(120, saved.getInt("max-height"))
    }

    @Test
    fun `an arena can be built end to end through the command dispatcher`() {
        player.isOp = true

        server.dispatchCommand(player, "edit startEditing")
        server.dispatchCommand(player, "edit setName dispatched")
        at(0.0, 65.0, 0.0)
        server.dispatchCommand(player, "edit setSpawn 1")
        at(5.0, 65.0, 0.0)
        // The short form of the same subcommand still works.
        server.dispatchCommand(player, "edit spawn 2")
        at(-20.0, 40.0, -20.0)
        server.dispatchCommand(player, "edit pos1")
        at(20.0, 120.0, 20.0)
        server.dispatchCommand(player, "edit setRegionTwo")
        at(8.0, 70.0, 8.0)
        server.dispatchCommand(player, "edit setdeathspawn")
        // Typed in a different case than the subcommand is declared with.
        server.dispatchCommand(player, "edit SHOWSTATUS")
        server.dispatchCommand(player, "edit saveArena")

        val saved = YamlConfiguration.loadConfiguration(arenaFile("dispatched"))
        assertEquals(arenaWorld.name, saved.getString("world"))
        assertEquals(40, saved.getInt("min-height"), "the lower corner is the void-kill level")
        assertEquals(120, saved.getInt("max-height"), "the upper corner is the ceiling")
        assertTrue(saved.isSet("spawns.1") && saved.isSet("spawns.2"))
        assertNotNull(GourPillars.arenaManager.getArenaByName("dispatched"))
    }

    @Test
    fun `a spawn is centred on its block and its view squared up`() {
        EditCMD.startEditing(player)
        EditCMD.setName(player, "aligned")
        player.teleport(Location(arenaWorld, 10.13, 65.0, 20.87, 84f, 3f))

        EditCMD.setSpawn(player, 1)
        at(5.0, 65.0, 0.0)
        EditCMD.setSpawn(player, 2)
        at(-20.0, 40.0, -20.0)
        EditCMD.setRegionOne(player)
        at(20.0, 120.0, 20.0)
        EditCMD.setRegionTwo(player)
        at(8.0, 70.0, 8.0)
        EditCMD.setDeathSpawn(player)
        EditCMD.saveArena(player)

        val saved = YamlConfiguration.loadConfiguration(arenaFile("aligned"))
        assertEquals(10.5, saved.getDouble("spawns.1.x"))
        assertEquals(20.5, saved.getDouble("spawns.1.z"))
        assertEquals(65.0, saved.getDouble("spawns.1.y"), "a whole-block height is kept as it is")
        assertEquals(90.0, saved.getDouble("spawns.1.yaw"), "84 degrees is close enough to west to be meant as west")
        assertEquals(0.0, saved.getDouble("spawns.1.pitch"))
    }

    @Test
    fun `with the assist off the spot is stored exactly as you stand`() {
        EditCMD.startEditing(player)
        EditCMD.setName(player, "raw")
        EditCMD.toggleAlignment(player)
        player.teleport(Location(arenaWorld, 10.13, 65.0, 20.87, 84f, 3f))

        EditCMD.setSpawn(player, 1)
        at(5.0, 65.0, 0.0)
        EditCMD.setSpawn(player, 2)
        at(-20.0, 40.0, -20.0)
        EditCMD.setRegionOne(player)
        at(20.0, 120.0, 20.0)
        EditCMD.setRegionTwo(player)
        at(8.0, 70.0, 8.0)
        EditCMD.setDeathSpawn(player)
        EditCMD.saveArena(player)

        val saved = YamlConfiguration.loadConfiguration(arenaFile("raw"))
        assertEquals(10.13, saved.getDouble("spawns.1.x"))
        assertEquals(84.0, saved.getDouble("spawns.1.yaw"))
        assertEquals(8.0, saved.getDouble("main-spawn.x"), "the assist is off for every spot, not just spawns")
    }

    @Test
    fun `loading an arena takes you to its world`() {
        buildMinimalArena("faraway")
        EditCMD.saveArena(player)
        EditCMD.stopEditing(player)
        player.teleport(GourPillars.spawnManager.getConfiguredWorld()!!.spawnLocation)

        EditCMD.loadExisting(player, "faraway")

        assertEquals(arenaWorld, player.world)
    }

    @Test
    fun `loading while already in the arena world leaves you where you are`() {
        buildMinimalArena("nearby")
        EditCMD.saveArena(player)
        EditCMD.stopEditing(player)
        val standingAt = Location(arenaWorld, 100.0, 90.0, 100.0)
        player.teleport(standingAt)

        EditCMD.loadExisting(player, "nearby")

        assertEquals(standingAt.x, player.location.x)
        assertEquals(standingAt.z, player.location.z)
    }

    @Test
    fun `an arena whose world is not loaded is reported instead of opening a broken session`() {
        val config = YamlConfiguration()
        config.set("world", "a-world-that-is-not-loaded")
        config.set("main-spawn.x", 0.0)
        config.set("main-spawn.y", 65.0)
        config.set("main-spawn.z", 0.0)
        config.save(arenaFile("unloaded"))

        EditCMD.loadExisting(player, "unloaded")

        assertFalse(GourPillars.isEditing, "no session may be opened for an arena nobody can edit")
    }

    @Test
    fun `a session puts you in creative and gives your mode back on stop`() {
        player.gameMode = GameMode.SURVIVAL

        EditCMD.startEditing(player)
        assertEquals(GameMode.CREATIVE, player.gameMode)

        EditCMD.stopEditing(player)
        assertEquals(GameMode.SURVIVAL, player.gameMode)
    }

    @Test
    fun `loading an arena also switches you to creative`() {
        buildMinimalArena("creative-load")
        EditCMD.saveArena(player)
        EditCMD.stopEditing(player)
        player.gameMode = GameMode.ADVENTURE

        EditCMD.loadExisting(player, "creative-load")

        assertEquals(GameMode.CREATIVE, player.gameMode)

        EditCMD.stopEditing(player)
        assertEquals(GameMode.ADVENTURE, player.gameMode, "the mode you had before the session is the one you get back")
    }

    @Test
    fun `stopping brings you back where the session started`() {
        val lobby = GourPillars.spawnManager.getConfiguredWorld()!!.spawnLocation
        player.teleport(lobby)
        EditCMD.startEditing(player)
        at(120.0, 90.0, 120.0)

        EditCMD.stopEditing(player)

        assertEquals(lobby.world, player.world)
        assertEquals(lobby.x, player.location.x)
        assertEquals(lobby.z, player.location.z)
    }

    @Test
    fun `a disconnected editor gets their gamemode back without a teleport`() {
        player.gameMode = GameMode.SURVIVAL
        EditCMD.startEditing(player)
        at(120.0, 90.0, 120.0)

        EditCMD.handleQuit(player)

        assertEquals(GameMode.SURVIVAL, player.gameMode)
        assertEquals(arenaWorld, player.world, "a teleport during a quit would not take effect anyway")
    }

    @Test
    fun `editing blocks joining and stops blocking it once the session ends`() {
        EditCMD.startEditing(player)
        assertTrue(GourPillars.isEditing)

        EditCMD.stopEditing(player)
        assertFalse(GourPillars.isEditing)
    }

    @Test
    fun `a disconnected editor does not leave the server locked in edit mode`() {
        EditCMD.startEditing(player)
        EditCMD.setName(player, "abandoned")
        assertTrue(GourPillars.isEditing)

        EditCMD.handleQuit(player)

        assertFalse(GourPillars.isEditing)
    }

    @Test
    fun `stopping with unsaved changes asks for a confirmation first`() {
        EditCMD.startEditing(player)
        EditCMD.setName(player, "unsaved")

        EditCMD.stopEditing(player)
        assertTrue(GourPillars.isEditing, "the first stop only warns about the unsaved changes")

        EditCMD.stopEditing(player)
        assertFalse(GourPillars.isEditing)
    }
}
