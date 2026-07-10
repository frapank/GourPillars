package org.gourmet.gourPillars.api

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.api.events.GourPillarsArenaStateChangeEvent
import org.gourmet.gourPillars.api.events.GourPillarsEventSelectedEvent
import org.gourmet.gourPillars.api.events.GourPillarsGameEndEvent
import org.gourmet.gourPillars.api.events.GourPillarsGameStartEvent
import org.gourmet.gourPillars.api.events.GourPillarsPlayerEliminatedEvent
import org.gourmet.gourPillars.api.events.GourPillarsPlayerFinishEvent
import org.gourmet.gourPillars.api.events.GourPillarsPlayerJoinArenaEvent
import org.gourmet.gourPillars.api.events.GourPillarsPlayerKillEvent
import org.gourmet.gourPillars.api.events.GourPillarsPlayerLeaveArenaEvent
import org.gourmet.gourPillars.api.events.GourPillarsSpectateStartEvent
import org.gourmet.gourPillars.api.events.GourPillarsSpectateStopEvent
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.GameEvents
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.Region
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GourPillarsAPITest {
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
    }

    private fun newArena(
        name: String,
        maxPlayers: Int = 2,
        minPlayers: Int = 2,
        isPrivate: Boolean = false,
    ): Arena {
        val world = server.addSimpleWorld("world_${name}_${worldCounter++}")
        val spawns =
            mutableMapOf<Location, Player?>(
                Location(world, 0.0, 65.0, 0.0) to null,
                Location(world, 5.0, 65.0, 0.0) to null,
                Location(world, 10.0, 65.0, 0.0) to null,
            )
        val main = Location(world, 20.0, 65.0, 0.0)
        val regionOne = Location(world, -10.0, 60.0, -10.0)
        val regionTwo = Location(world, 30.0, 70.0, 30.0)
        val region = Region.createRegion(regionOne, regionTwo)

        return Arena(
            spawnMap = spawns,
            spawnMainLocation = main,
            isPrivate = isPrivate,
            slowFallingTime = 1,
            maxPlayer = maxPlayers,
            minPlayer = minPlayers,
            maxHeight = -1,
            minHeight = 60,
            regionLocOne = regionOne,
            regionLocTwo = regionTwo,
            region = region,
            name = name,
        )
    }

    private fun register(arena: Arena) {
        GourPillars.arenaManager.onlineArenas[arena.name] = arena
    }

    @Test
    fun `getArena returns a snapshot matching the arena`() {
        val arena = newArena("alpha", maxPlayers = 3, minPlayers = 2)
        register(arena)

        val info = GourPillars.api.getArena("alpha")

        assertEquals("alpha", info?.name)
        assertEquals(State.WAITING, info?.state)
        assertEquals(0, info?.currentPlayers)
        assertEquals(3, info?.maxPlayers)
        assertFalse(info!!.isFull)
        assertTrue(info.isJoinable)
    }

    @Test
    fun `getArena is null for an unknown name`() {
        assertNull(GourPillars.api.getArena("does-not-exist"))
        assertNull(GourPillars.api.isArenaFull("does-not-exist"))
    }

    @Test
    fun `isArenaFull and getAvailableArenas reflect player count`() {
        val arena = newArena("beta", maxPlayers = 2, minPlayers = 2)
        register(arena)

        assertEquals(false, GourPillars.api.isArenaFull("beta"))
        assertTrue(GourPillars.api.getAvailableArenas().any { it.name == "beta" })

        arena.inGamePlayer.add(server.addPlayer("filler1"))
        arena.inGamePlayer.add(server.addPlayer("filler2"))

        assertEquals(true, GourPillars.api.isArenaFull("beta"))
        assertFalse(GourPillars.api.getAvailableArenas().any { it.name == "beta" })
    }

    @Test
    fun `private arenas never show up as available`() {
        val arena = newArena("secret", isPrivate = true)
        register(arena)

        assertFalse(GourPillars.api.getAvailableArenas().any { it.name == "secret" })
    }

    @Test
    fun `sendPlayerToArena reports arena not found`() {
        val player = server.addPlayer("Nowhere")
        assertEquals(ArenaJoinResult.ARENA_NOT_FOUND, GourPillars.api.sendPlayerToArena(player, "ghost"))
    }

    @Test
    fun `sendPlayerToArena rejects joining a private arena`() {
        val arena = newArena("private-arena", isPrivate = true)
        register(arena)
        val player = server.addPlayer("Priv")

        assertEquals(ArenaJoinResult.ARENA_PRIVATE, GourPillars.api.sendPlayerToArena(player, "private-arena"))
    }

    @Test
    fun `sendPlayerToArena rejects a player already in the arena`() {
        val arena = newArena("dup")
        register(arena)
        val player = server.addPlayer("Dup")
        arena.inGamePlayer.add(player)

        assertEquals(ArenaJoinResult.ALREADY_IN_GAME, GourPillars.api.sendPlayerToArena(player, "dup"))
    }

    @Test
    fun `sendPlayerToArena rejects a match that is not ready`() {
        val arena = newArena("running")
        register(arena)
        arena.gameState = State.INGAME
        val player = server.addPlayer("Late")

        assertEquals(ArenaJoinResult.ARENA_NOT_READY, GourPillars.api.sendPlayerToArena(player, "running"))
    }

    @Test
    fun `sendPlayerToArena rejects joins during the event selection lock`() {
        val arena = newArena("locking")
        register(arena)
        arena.gameState = State.STARTING
        val player = server.addPlayer("Late")

        assertEquals(ArenaJoinResult.ARENA_NOT_READY, GourPillars.api.sendPlayerToArena(player, "locking"))
    }

    @Test
    fun `sendPlayerToArena still accepts joins while the countdown is running`() {
        val arena = newArena("countdown", maxPlayers = 3, minPlayers = 2)
        register(arena)

        assertEquals(ArenaJoinResult.SUCCESS, GourPillars.api.sendPlayerToArena(server.addPlayer("First"), "countdown"))
        assertEquals(ArenaJoinResult.SUCCESS, GourPillars.api.sendPlayerToArena(server.addPlayer("Second"), "countdown"))
        assertEquals(State.STARTING, arena.gameState)

        assertEquals(ArenaJoinResult.SUCCESS, GourPillars.api.sendPlayerToArena(server.addPlayer("Third"), "countdown"))
    }

    @Test
    fun `sendPlayerToArena rejects a full arena`() {
        val arena = newArena("packed", maxPlayers = 2, minPlayers = 2)
        register(arena)
        arena.inGamePlayer.add(server.addPlayer("filler1"))
        arena.inGamePlayer.add(server.addPlayer("filler2"))
        val player = server.addPlayer("Latecomer")

        assertEquals(ArenaJoinResult.ARENA_FULL, GourPillars.api.sendPlayerToArena(player, "packed"))
    }

    @Test
    fun `sendPlayerToArena rejects a player who is currently spectating`() {
        val spectated = newArena("spectated-match")
        register(spectated)
        val target = newArena("target-match")
        register(target)

        val player = server.addPlayer("Ghost")
        spectated.spectators.add(player)

        assertEquals(ArenaJoinResult.ALREADY_SPECTATING, GourPillars.api.sendPlayerToArena(player, "target-match"))
    }

    @Test
    fun `player is caged while waiting and no longer caged once the match starts`() {
        val arena = newArena("cage-test", maxPlayers = 2, minPlayers = 2)
        register(arena)
        val playerA = server.addPlayer("CageA")
        val playerB = server.addPlayer("CageB")

        assertEquals(ArenaJoinResult.SUCCESS, GourPillars.api.sendPlayerToArena(playerA, "cage-test"))
        assertTrue(GourPillars.api.isPlayerCaged(playerA))

        assertEquals(ArenaJoinResult.SUCCESS, GourPillars.api.sendPlayerToArena(playerB, "cage-test"))
        assertEquals(State.STARTING, arena.gameState)
        assertTrue(GourPillars.api.isPlayerCaged(playerA))

        // Mirrors what CountDownTask does once the countdown reaches zero.
        arena.gameState = State.INGAME
        arena.gameTask.run()

        assertFalse(GourPillars.api.isPlayerCaged(playerA))
    }

    @Test
    fun `spectator helpers reflect spectate state`() {
        val arena = newArena("spec-arena")
        register(arena)
        val player = server.addPlayer("Spec")

        assertFalse(GourPillars.api.isSpectating(player))
        assertNull(GourPillars.api.getSpectatedArena(player))

        arena.addSpectator(player)

        assertTrue(GourPillars.api.isSpectating(player))
        assertEquals("spec-arena", GourPillars.api.getSpectatedArena(player)?.name)
        assertEquals(listOf(player), GourPillars.api.getSpectators("spec-arena"))

        arena.removeSpectator(player)

        assertFalse(GourPillars.api.isSpectating(player))
        assertEquals(emptyList<Any>(), GourPillars.api.getSpectators("spec-arena"))
    }

    @Test
    fun `getPlayersInArena returns a snapshot, not a live view`() {
        val arena = newArena("copy-test")
        register(arena)
        val player = server.addPlayer("Copy")
        arena.inGamePlayer.add(player)

        val snapshot = GourPillars.api.getPlayersInArena("copy-test")
        assertEquals(listOf(player), snapshot)

        // Later mutations of the arena's live player set must not retroactively change the snapshot.
        arena.inGamePlayer.add(server.addPlayer("CopyLate"))
        assertEquals(1, snapshot?.size)
    }

    @Test
    fun `alive players, kills, time remaining and current event are null outside an active match`() {
        val arena = newArena("idle-arena")
        register(arena)

        assertNull(GourPillars.api.getAlivePlayers("idle-arena"))
        assertNull(GourPillars.api.getTimeRemainingSeconds("idle-arena"))
        assertNull(GourPillars.api.getCurrentEvent("idle-arena"))
        assertNull(GourPillars.api.getMatchKills(server.addPlayer("NotInMatch")))
    }

    @Test
    fun `alive players, kills and time remaining are populated once a match starts`() {
        val arena = newArena("live-arena", maxPlayers = 2, minPlayers = 2)
        register(arena)
        val playerA = server.addPlayer("LiveA")
        val playerB = server.addPlayer("LiveB")
        arena.inGamePlayer.add(playerA)
        arena.inGamePlayer.add(playerB)

        arena.gameState = State.INGAME
        arena.gameTask.run()

        assertEquals(setOf(playerA, playerB), GourPillars.api.getAlivePlayers("live-arena")?.toSet())
        assertEquals(0, GourPillars.api.getMatchKills(playerA))
        assertNotNull(GourPillars.api.getTimeRemainingSeconds("live-arena"))

        arena.gameTask.playerEliminated(playerB, playerA)

        assertEquals(1, GourPillars.api.getMatchKills(playerA))
        assertEquals(listOf(playerA), GourPillars.api.getAlivePlayers("live-arena"))
    }

    @Test
    fun `getArena reflects live match data while in-game`() {
        val arena = newArena("snapshot-arena", maxPlayers = 2, minPlayers = 2)
        register(arena)
        arena.inGamePlayer.add(server.addPlayer("SnapA"))
        arena.inGamePlayer.add(server.addPlayer("SnapB"))

        arena.gameState = State.INGAME
        arena.gameTask.run()

        val info = GourPillars.api.getArena("snapshot-arena")
        assertEquals(2, info?.alivePlayers)
        assertNotNull(info?.secondsRemaining)
    }

    @Test
    fun `removePlayerFromArena returns false when the player is nowhere`() {
        assertFalse(GourPillars.api.removePlayerFromArena(server.addPlayer("Loner")))
    }

    @Test
    fun `removePlayerFromArena removes a waiting player`() {
        val arena = newArena("leave-waiting")
        register(arena)
        val player = server.addPlayer("LeaveWaiting")
        arena.inGamePlayer.add(player)

        assertTrue(GourPillars.api.removePlayerFromArena(player))
        assertFalse(arena.inGamePlayer.contains(player))
    }

    @Test
    fun `removePlayerFromArena eliminates a mid-match player`() {
        val arena = newArena("leave-ingame", maxPlayers = 2, minPlayers = 2)
        register(arena)
        val playerA = server.addPlayer("LeaveA")
        val playerB = server.addPlayer("LeaveB")
        arena.inGamePlayer.add(playerA)
        arena.inGamePlayer.add(playerB)
        arena.gameState = State.INGAME
        arena.gameTask.run()

        assertTrue(GourPillars.api.removePlayerFromArena(playerA))
        assertFalse(arena.gameTask.alivePlayer.contains(playerA))
    }

    private class LifecycleListener : Listener {
        val joins = mutableListOf<GourPillarsPlayerJoinArenaEvent>()
        val leaves = mutableListOf<GourPillarsPlayerLeaveArenaEvent>()
        val spectateStarts = mutableListOf<GourPillarsSpectateStartEvent>()
        val spectateStops = mutableListOf<GourPillarsSpectateStopEvent>()
        val stateChanges = mutableListOf<GourPillarsArenaStateChangeEvent>()

        @EventHandler
        fun onJoin(event: GourPillarsPlayerJoinArenaEvent) {
            joins.add(event)
        }

        @EventHandler
        fun onLeave(event: GourPillarsPlayerLeaveArenaEvent) {
            leaves.add(event)
        }

        @EventHandler
        fun onSpectateStart(event: GourPillarsSpectateStartEvent) {
            spectateStarts.add(event)
        }

        @EventHandler
        fun onSpectateStop(event: GourPillarsSpectateStopEvent) {
            spectateStops.add(event)
        }

        @EventHandler
        fun onStateChange(event: GourPillarsArenaStateChangeEvent) {
            stateChanges.add(event)
        }
    }

    @Test
    fun `join, leave, spectate and state-change events fire correctly`() {
        val arena = newArena("lifecycle-arena", maxPlayers = 2, minPlayers = 2)
        register(arena)
        val player = server.addPlayer("Lifecycle")
        val spectator = server.addPlayer("LifecycleSpec")

        val listener = LifecycleListener()
        server.pluginManager.registerEvents(listener, GourPillars.instance)

        assertEquals(ArenaJoinResult.SUCCESS, GourPillars.api.sendPlayerToArena(player, "lifecycle-arena"))
        assertEquals(1, listener.joins.size)
        assertEquals(player, listener.joins[0].player)
        assertEquals("lifecycle-arena", listener.joins[0].arenaName)

        arena.removePlayer(player)
        assertEquals(1, listener.leaves.size)
        assertEquals(player, listener.leaves[0].player)

        arena.addSpectator(spectator)
        assertEquals(1, listener.spectateStarts.size)
        assertEquals(spectator, listener.spectateStarts[0].player)

        arena.removeSpectator(spectator)
        assertEquals(1, listener.spectateStops.size)
        assertEquals(spectator, listener.spectateStops[0].player)

        // Redundant assignment to the same state must not fire a spurious event.
        val changesBeforeNoop = listener.stateChanges.size
        arena.gameState = arena.gameState
        assertEquals(changesBeforeNoop, listener.stateChanges.size)

        arena.gameState = State.INGAME
        val lastChange = listener.stateChanges.last()
        assertEquals(State.INGAME, lastChange.newState)
        assertEquals("lifecycle-arena", lastChange.arenaName)

        HandlerList.unregisterAll(listener)
    }

    private class RecordingListener : Listener {
        val starts = mutableListOf<GourPillarsGameStartEvent>()
        val ends = mutableListOf<GourPillarsGameEndEvent>()
        val finishes = mutableListOf<GourPillarsPlayerFinishEvent>()
        val kills = mutableListOf<GourPillarsPlayerKillEvent>()
        val eliminations = mutableListOf<GourPillarsPlayerEliminatedEvent>()
        val eventSelections = mutableListOf<GourPillarsEventSelectedEvent>()

        @EventHandler
        fun onStart(event: GourPillarsGameStartEvent) {
            starts.add(event)
        }

        @EventHandler
        fun onEnd(event: GourPillarsGameEndEvent) {
            ends.add(event)
        }

        @EventHandler
        fun onFinish(event: GourPillarsPlayerFinishEvent) {
            finishes.add(event)
        }

        @EventHandler
        fun onKill(event: GourPillarsPlayerKillEvent) {
            kills.add(event)
        }

        @EventHandler
        fun onEliminated(event: GourPillarsPlayerEliminatedEvent) {
            eliminations.add(event)
        }

        @EventHandler
        fun onEventSelected(event: GourPillarsEventSelectedEvent) {
            eventSelections.add(event)
        }
    }

    @Test
    fun `a full match fires start, kill, finish and end events`() {
        val arena = newArena("events-match", maxPlayers = 2, minPlayers = 2)
        register(arena)
        val winner = server.addPlayer("Winner")
        val loser = server.addPlayer("Loser")
        arena.inGamePlayer.add(winner)
        arena.inGamePlayer.add(loser)

        val listener = RecordingListener()
        server.pluginManager.registerEvents(listener, GourPillars.instance)

        arena.gameState = State.INGAME
        arena.gameTask.run()

        assertEquals(1, listener.starts.size)
        assertEquals("events-match", listener.starts[0].arenaName)

        arena.gameTask.playerEliminated(loser, winner)

        assertEquals(1, listener.kills.size)
        assertEquals(winner, listener.kills[0].killer)
        assertEquals(loser, listener.kills[0].victim)

        assertEquals(1, listener.eliminations.size)
        assertEquals(loser, listener.eliminations[0].player)
        assertEquals(EliminationCause.KILL, listener.eliminations[0].cause)
        assertEquals(winner, listener.eliminations[0].source)

        assertEquals(2, listener.finishes.size)
        val loserFinish = listener.finishes.first { it.player == loser }
        val winnerFinish = listener.finishes.first { it.player == winner }
        assertFalse(loserFinish.won)
        assertTrue(winnerFinish.won)
        assertEquals(1, winnerFinish.kills)

        assertEquals(1, listener.ends.size)
        assertEquals(winner, listener.ends[0].winner)
        assertEquals("events-match", listener.ends[0].arenaName)

        HandlerList.unregisterAll(listener)
    }

    @Test
    fun `void, fall and mob eliminations fire the eliminated event with the right cause`() {
        val arena = newArena("cause-match", maxPlayers = 4, minPlayers = 4)
        register(arena)
        val voidVictim = server.addPlayer("VoidVictim")
        val voidKillerVictim = server.addPlayer("VoidKillerVictim")
        val voidKiller = server.addPlayer("VoidKiller")
        val fallVictim = server.addPlayer("FallVictim")
        val mobVictim = server.addPlayer("MobVictim")
        listOf(voidVictim, voidKillerVictim, voidKiller, fallVictim, mobVictim).forEach { arena.inGamePlayer.add(it) }

        val listener = RecordingListener()
        server.pluginManager.registerEvents(listener, GourPillars.instance)

        arena.gameState = State.INGAME
        arena.gameTask.run()

        arena.gameTask.playerEliminatedVoid(voidVictim)
        arena.gameTask.playerEliminatedVoid(voidKillerVictim, voidKiller)
        arena.gameTask.playerEliminatedFall(fallVictim)
        val zombie = mobVictim.world.spawn(mobVictim.location, Zombie::class.java)
        arena.gameTask.playerEliminatedByMob(mobVictim, zombie)

        val byPlayer = listener.eliminations.associateBy { it.player }
        assertEquals(EliminationCause.VOID, byPlayer[voidVictim]?.cause)
        assertEquals(EliminationCause.VOID_KILL, byPlayer[voidKillerVictim]?.cause)
        assertEquals(voidKiller, byPlayer[voidKillerVictim]?.source)
        assertEquals(EliminationCause.FALL, byPlayer[fallVictim]?.cause)
        assertEquals(EliminationCause.MOB, byPlayer[mobVictim]?.cause)
        assertEquals(zombie, byPlayer[mobVictim]?.source)

        HandlerList.unregisterAll(listener)
    }

    @Test
    fun `applyEvent fires the event-selected event, including no event`() {
        val arena = newArena("vote-match")
        register(arena)

        val listener = RecordingListener()
        server.pluginManager.registerEvents(listener, GourPillars.instance)

        arena.gameTask.applyEvent(GameEvents.LAVA)
        arena.gameTask.applyEvent(null)

        assertEquals(2, listener.eventSelections.size)
        assertEquals(GameEvents.LAVA, listener.eventSelections[0].event)
        assertEquals(null, listener.eventSelections[1].event)
        assertEquals("vote-match", listener.eventSelections[0].arenaName)

        HandlerList.unregisterAll(listener)
    }
}
