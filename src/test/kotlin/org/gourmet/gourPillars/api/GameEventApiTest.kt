package org.gourmet.gourPillars.api

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.inventory.InventoryView
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.api.event.GameEventContext
import org.gourmet.gourPillars.api.event.GameEventDefinition
import org.gourmet.gourPillars.api.event.GameEventHandler
import org.gourmet.gourPillars.guis.VoteInventory
import org.gourmet.gourPillars.managers.game.GameEventRegistry
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.EventSelector
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.Region
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GameEventApiTest {
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
        GourPillars.api.getRegisteredEvents().forEach { GourPillars.api.unregisterEvent(it.id) }
        GourPillars.arenaManager.onlineArenas.clear()
    }

    private fun newArena(
        name: String,
        maxPlayers: Int = 2,
        minPlayers: Int = 2,
    ): Arena {
        val world = server.addSimpleWorld("world_${name}_${worldCounter++}")
        val spawns =
            mutableMapOf<Location, Player?>(
                Location(world, 0.0, 65.0, 0.0) to null,
                Location(world, 5.0, 65.0, 0.0) to null,
            )
        val regionOne = Location(world, -10.0, 60.0, -10.0)
        val regionTwo = Location(world, 30.0, 70.0, 30.0)

        val arena =
            Arena(
                spawnMap = spawns,
                spawnMainLocation = Location(world, 20.0, 65.0, 0.0),
                isPrivate = false,
                slowFallingTime = 1,
                maxPlayer = maxPlayers,
                minPlayer = minPlayers,
                maxHeight = -1,
                minHeight = 60,
                regionLocOne = regionOne,
                regionLocTwo = regionTwo,
                region = Region.createRegion(regionOne, regionTwo),
                name = name,
            )
        GourPillars.arenaManager.onlineArenas[name] = arena
        return arena
    }

    private class RecordingHandler : GameEventHandler {
        var started = 0
        var stopped = 0
        var lastWinner: Player? = null

        override fun onStart() {
            started++
        }

        override fun onStop(winner: Player?) {
            stopped++
            lastWinner = winner
        }
    }

    private class TestEvent(
        override val id: String,
        private val handlerFactory: (GameEventContext) -> GameEventHandler = { GameEventHandler.EMPTY },
    ) : GameEventDefinition {
        override val displayName: Component = Component.text(id)

        override fun createHandler(context: GameEventContext): GameEventHandler = handlerFactory(context)
    }

    @Test
    fun `register, lookup and unregister work`() {
        val api = GourPillars.api
        assertTrue(api.registerEvent(GourPillars.instance, TestEvent("event-a")))
        assertTrue(api.registerEvent(GourPillars.instance, TestEvent("event-b")))

        // Registration order is kept, duplicates are rejected.
        assertEquals(listOf("event-a", "event-b"), api.getRegisteredEvents().map { it.id })
        assertFalse(api.registerEvent(GourPillars.instance, TestEvent("event-a")))

        assertEquals("event-a", api.getRegisteredEvent("event-a")?.id)
        assertNull(api.getRegisteredEvent("missing"))

        assertTrue(api.unregisterEvent("event-a"))
        assertFalse(api.unregisterEvent("event-a"))
        assertEquals(listOf("event-b"), api.getRegisteredEvents().map { it.id })
    }

    @Test
    fun `malformed and reserved ids are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            GourPillars.api.registerEvent(GourPillars.instance, TestEvent("Bad ID!"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            GourPillars.api.registerEvent(GourPillars.instance, TestEvent(""))
        }
        GameEventRegistry.RESERVED_IDS.forEach { reserved ->
            assertThrows(IllegalArgumentException::class.java) {
                GourPillars.api.registerEvent(GourPillars.instance, TestEvent(reserved))
            }
        }
    }

    @Test
    fun `unregisterEvents only removes that plugin's events`() {
        val otherPlugin = MockBukkit.createMockPlugin("SomeAddon")
        GourPillars.api.registerEvent(GourPillars.instance, TestEvent("mine"))
        GourPillars.api.registerEvent(otherPlugin, TestEvent("theirs-a"))
        GourPillars.api.registerEvent(otherPlugin, TestEvent("theirs-b"))

        assertEquals(2, GourPillars.api.unregisterEvents(otherPlugin))

        assertEquals(listOf("mine"), GourPillars.api.getRegisteredEvents().map { it.id })
        assertEquals(0, GourPillars.api.unregisterEvents(otherPlugin))
    }

    @Test
    fun `disabling a plugin unregisters its events automatically`() {
        val addon = MockBukkit.createMockPlugin("DisappearingAddon")
        GourPillars.api.registerEvent(addon, TestEvent("vanishing"))

        server.pluginManager.callEvent(PluginDisableEvent(addon))

        assertNull(GourPillars.api.getRegisteredEvent("vanishing"))
    }

    @Test
    fun `a full match drives the handler through its lifecycle`() {
        val handler = RecordingHandler()
        GourPillars.api.registerEvent(GourPillars.instance, TestEvent("lifecycle") { handler })

        val arena = newArena("handler-match")
        val winner = server.addPlayer("EventWinner")
        val loser = server.addPlayer("EventLoser")
        arena.inGamePlayer.add(winner)
        arena.inGamePlayer.add(loser)

        arena.gameTask.applyEvent("lifecycle")
        assertEquals("lifecycle", arena.gameEvent)
        assertEquals(0, handler.started)

        arena.gameState = State.INGAME
        arena.gameTask.run()

        assertEquals(1, handler.started)
        assertEquals("lifecycle", GourPillars.api.getCurrentEvent("handler-match"))
        assertEquals("lifecycle", GourPillars.api.getArena("handler-match")?.currentEvent)

        arena.gameTask.playerEliminated(loser, winner)

        assertEquals(1, handler.stopped)
        assertEquals(winner, handler.lastWinner)
        assertNull(arena.gameEvent)
        assertNull(GourPillars.api.getCurrentEvent("handler-match"))
    }

    @Test
    fun `applyEvent falls back to no event for an unregistered id`() {
        val arena = newArena("stale-event-match")

        arena.gameTask.applyEvent("was-unregistered")

        assertNull(arena.gameEvent)
    }

    @Test
    fun `a handler factory that throws leaves the match eventless`() {
        GourPillars.api.registerEvent(
            GourPillars.instance,
            TestEvent("broken-factory") { error("boom") },
        )
        val arena = newArena("broken-factory-match")

        arena.gameTask.applyEvent("broken-factory")

        assertNull(arena.gameEvent)
    }

    @Test
    fun `a handler that throws in onStart is detached without onStop`() {
        val handler =
            object : GameEventHandler {
                var stopped = 0

                override fun onStart(): Unit = error("boom")

                override fun onStop(winner: Player?) {
                    stopped++
                }
            }
        GourPillars.api.registerEvent(GourPillars.instance, TestEvent("broken-start") { handler })

        val arena = newArena("broken-start-match")
        val winner = server.addPlayer("BrokenWinner")
        val loser = server.addPlayer("BrokenLoser")
        arena.inGamePlayer.add(winner)
        arena.inGamePlayer.add(loser)

        arena.gameTask.applyEvent("broken-start")
        arena.gameState = State.INGAME
        arena.gameTask.run()

        assertNull(arena.gameEvent)

        arena.gameTask.playerEliminated(loser, winner)
        assertEquals(0, handler.stopped)
    }

    @Test
    fun `unregistering an active event stops its handler mid-match`() {
        val handler = RecordingHandler()
        GourPillars.api.registerEvent(GourPillars.instance, TestEvent("pulled") { handler })

        val arena = newArena("pulled-match")
        arena.inGamePlayer.add(server.addPlayer("PulledA"))
        arena.inGamePlayer.add(server.addPlayer("PulledB"))

        arena.gameTask.applyEvent("pulled")
        arena.gameState = State.INGAME
        arena.gameTask.run()
        assertEquals(1, handler.started)

        GourPillars.api.unregisterEvent("pulled")

        assertEquals(1, handler.stopped)
        assertNull(handler.lastWinner)
        assertNull(arena.gameEvent)
        assertNull(GourPillars.api.getCurrentEvent("pulled-match"))
    }

    @Test
    fun `unregistering drops pending votes for that event only`() {
        GourPillars.api.registerEvent(GourPillars.instance, TestEvent("voted-away"))
        val arena = newArena("vote-cleanup-match")
        val voterA = server.addPlayer("VoterA").uniqueId
        val voterB = server.addPlayer("VoterB").uniqueId
        arena.eventVotes[voterA] = "voted-away"
        arena.eventVotes[voterB] = GameEventRegistry.NO_EVENT_ID

        GourPillars.api.unregisterEvent("voted-away")

        // VoterA can vote again, VoterB's "no event" vote survives.
        assertEquals(mapOf(voterB to GameEventRegistry.NO_EVENT_ID), arena.eventVotes)
    }

    @Test
    fun `votes are cleared when the match ends`() {
        GourPillars.api.registerEvent(GourPillars.instance, TestEvent("sticky"))
        val arena = newArena("vote-reset-match")
        val winner = server.addPlayer("ResetWinner")
        val loser = server.addPlayer("ResetLoser")
        arena.inGamePlayer.add(winner)
        arena.inGamePlayer.add(loser)
        arena.eventVotes[winner.uniqueId] = "sticky"

        arena.gameState = State.INGAME
        arena.gameTask.run()
        arena.gameTask.playerEliminated(loser, winner)

        assertTrue(arena.eventVotes.isEmpty())
    }

    @Test
    fun `selectWinner follows the votes when the base weight is zero`() {
        val config = GourPillars.instance.config
        config.set("game.events.base-weight", 0.0)
        config.set("game.events.vote-weight", 1.0)
        try {
            GourPillars.api.registerEvent(GourPillars.instance, TestEvent("unpopular"))
            GourPillars.api.registerEvent(GourPillars.instance, TestEvent("popular"))
            val arena = newArena("weighted-match")
            arena.eventVotes[server.addPlayer("OnlyVoter").uniqueId] = "popular"

            repeat(20) {
                assertEquals("popular", EventSelector.selectWinner(arena))
            }
        } finally {
            config.set("game.events.base-weight", null)
            config.set("game.events.vote-weight", null)
        }
    }

    private fun plainName(
        view: InventoryView,
        slot: Int,
    ): String? =
        view.topInventory.getItem(slot)?.itemMeta?.displayName()?.let {
            PlainTextComponentSerializer.plainText().serialize(it)
        }

    private fun slotOf(
        view: InventoryView,
        name: String,
    ): Int = (0 until view.topInventory.size).first { plainName(view, it) == name }

    private fun clickSlot(
        view: InventoryView,
        slot: Int,
    ): InventoryClickEvent {
        val click = InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL)
        server.pluginManager.callEvent(click)
        return click
    }

    @Test
    fun `vote gui skips leftover legacy config items and adds registered events`() {
        val config = GourPillars.instance.config
        config.set("gui.vote.items.legacy-event.slot", 11)
        config.set("gui.vote.items.legacy-event.material", "STONE")
        config.set("gui.vote.items.legacy-event.name", "legacy item")
        try {
            GourPillars.api.registerEvent(GourPillars.instance, TestEvent("fresh"))
            val player = server.addPlayer("GuiViewer")
            VoteInventory.displayInventory(player)

            val view = player.openInventory
            val names = (0 until view.topInventory.size).mapNotNull { plainName(view, it) }
            assertFalse(names.contains("legacy item"), "stale config item must not be rendered")
            assertTrue(names.contains("fresh"), "registered event item must be rendered")
        } finally {
            config.set("gui.vote.items.legacy-event", null)
        }
    }

    @Test
    fun `clicking vote items registers event, no-event and time votes`() {
        GourPillars.api.registerEvent(GourPillars.instance, TestEvent("clickable"))
        val arena = newArena("click-arena")
        val player = server.addPlayer("Clicker")
        arena.inGamePlayer.add(player)

        VoteInventory.displayInventory(player)
        val view = player.openInventory

        val click = clickSlot(view, slotOf(view, "clickable"))
        assertTrue(click.isCancelled)
        assertEquals("clickable", arena.eventVotes[player.uniqueId])

        // A second event vote is rejected ("No event" comes from the bundled config).
        clickSlot(view, slotOf(view, "No event"))
        assertEquals("clickable", arena.eventVotes[player.uniqueId])

        // Time votes are independent from event votes.
        clickSlot(view, slotOf(view, "Day time"))
        assertTrue(arena.dayVote.contains(player))
    }

    @Test
    fun `built-in vote items appear even without config entries`() {
        val config = GourPillars.instance.config
        config.set("gui.vote.items", null)
        try {
            val player = server.addPlayer("FallbackViewer")
            VoteInventory.displayInventory(player)

            val view = player.openInventory
            val names = (0 until view.topInventory.size).mapNotNull { plainName(view, it) }
            assertTrue(names.contains("No event"))
            assertTrue(names.contains("Day time"))
            assertTrue(names.contains("Night event"))
        } finally {
            GourPillars.instance.reloadConfig()
        }
    }

    @Test
    fun `a built-in vote item can be hidden with enabled false`() {
        val config = GourPillars.instance.config
        config.set("gui.vote.items.day-vote.enabled", false)
        try {
            val player = server.addPlayer("HiddenViewer")
            VoteInventory.displayInventory(player)

            val view = player.openInventory
            val names = (0 until view.topInventory.size).mapNotNull { plainName(view, it) }
            assertFalse(names.contains("Day time"))
            assertTrue(names.contains("Night event"))
        } finally {
            GourPillars.instance.reloadConfig()
        }
    }

    @Test
    fun `an out-of-bounds configured slot falls back to a free one`() {
        val config = GourPillars.instance.config
        config.set("gui.vote.items.no-event.slot", 99)
        try {
            val player = server.addPlayer("SlotViewer")
            VoteInventory.displayInventory(player)

            val view = player.openInventory
            val names = (0 until view.topInventory.size).mapNotNull { plainName(view, it) }
            assertTrue(names.contains("No event"))
        } finally {
            GourPillars.instance.reloadConfig()
        }
    }

    @Test
    fun `candidates always include no event plus every registered event`() {
        assertEquals(listOf(null), EventSelector.candidates())

        GourPillars.api.registerEvent(GourPillars.instance, TestEvent("only-one"))
        assertEquals(listOf(null, "only-one"), EventSelector.candidates())
    }
}
