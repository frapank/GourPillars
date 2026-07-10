package org.gourmet.gourPillars.listener.game

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.Region
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueueDamageListenerTest {
    private lateinit var server: ServerMock
    private var worldCounter = 0

    @BeforeAll
    fun setUpAll() {
        server = MockBukkit.mock()
        MockBukkit.createMockPlugin("PlaceholderAPI")
        MockBukkit.load(GourPillars::class.java)
        // Keep the lobby world distinct from the default one, so the lobby
        // damage-protection listener can't interfere with these assertions.
        GourPillars.spawnManager.setSpawn(server.addSimpleWorld("queue-lobby-world").spawnLocation)
    }

    @AfterAll
    fun tearDownAll() {
        MockBukkit.unmock()
    }

    @AfterEach
    fun tearDownEach() {
        GourPillars.arenaManager.onlineArenas.clear()
    }

    private fun newArena(name: String): Arena {
        val world = server.addSimpleWorld("world_${name}_${worldCounter++}")
        val spawns =
            mutableMapOf<Location, Player?>(
                Location(world, 0.0, 65.0, 0.0) to null,
                Location(world, 5.0, 65.0, 0.0) to null,
            )
        val regionOne = Location(world, -10.0, 60.0, -10.0)
        val regionTwo = Location(world, 30.0, 70.0, 30.0)

        return Arena(
            spawnMap = spawns,
            spawnMainLocation = Location(world, 20.0, 65.0, 0.0),
            isPrivate = false,
            slowFallingTime = 1,
            maxPlayer = 2,
            minPlayer = 2,
            maxHeight = -1,
            minHeight = 60,
            regionLocOne = regionOne,
            regionLocTwo = regionTwo,
            region = Region.createRegion(regionOne, regionTwo),
            name = name,
        )
    }

    private fun damage(player: Player): EntityDamageEvent {
        val event =
            EntityDamageEvent(
                player,
                EntityDamageEvent.DamageCause.FALL,
                5.0,
            )
        server.pluginManager.callEvent(event)
        return event
    }

    @Test
    fun `damage is cancelled for a player queued in an arena`() {
        val arena = newArena("queue")
        GourPillars.arenaManager.onlineArenas[arena.name] = arena
        val player = server.addPlayer()
        player.teleport(arena.spawnMainLocation)
        arena.inGamePlayer.add(player)

        assertTrue(damage(player).isCancelled, "queued (WAITING) player should not take damage")

        arena.gameState = State.STARTING
        assertTrue(damage(player).isCancelled, "countdown (STARTING) player should not take damage")

        arena.gameState = State.STOPPED
        assertTrue(damage(player).isCancelled, "post-game (STOPPED) player should not take damage")
    }

    @Test
    fun `damage is untouched while the match is running or outside an arena`() {
        val arena = newArena("ingame")
        GourPillars.arenaManager.onlineArenas[arena.name] = arena
        val playerInGame = server.addPlayer()
        playerInGame.teleport(arena.spawnMainLocation)
        arena.inGamePlayer.add(playerInGame)
        arena.gameState = State.INGAME

        assertFalse(damage(playerInGame).isCancelled, "in-game damage must stay enabled")

        val strayPlayer = server.addPlayer()
        strayPlayer.teleport(Location(server.addSimpleWorld("stray_world_${worldCounter++}"), 0.0, 65.0, 0.0))
        assertFalse(damage(strayPlayer).isCancelled, "players outside any arena are not this listener's concern")
    }
}
