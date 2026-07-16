package org.gourmet.gourPillars.managers.game

import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.Plugin
import org.gourmet.gourPillars.api.event.GameEventDefinition
import org.gourmet.gourPillars.api.event.VoteItemSpec
import org.gourmet.gourPillars.other.Logger

// Game events registered by other plugins through GourPillarsAPI. Registration order
// drives the vote GUI layout. Mutating calls must happen on the main thread; the map
// itself is synchronized so a read from another thread can't observe a broken state.
//
// displayName/voteItem are snapshotted at registration: the GUI and the selection
// animation never call back into addon code, so a misbehaving getter can only fail
// the registering plugin's own registerEvent call.
class GameEventRegistry(
    private val arenaManager: ArenaManager,
) : Listener {
    data class VoteEntry(
        val id: String,
        val displayName: Component,
        val voteItem: VoteItemSpec,
    )

    private data class RegisteredEvent(
        val owner: Plugin,
        val definition: GameEventDefinition,
        val displayName: Component,
        val voteItem: VoteItemSpec,
    )

    companion object {
        const val NO_EVENT_ID = "no-event"
        val RESERVED_IDS = setOf(NO_EVENT_ID, "day-vote", "night-vote")
        private val ID_PATTERN = Regex("[a-z0-9_-]{1,32}")
    }

    private val events = LinkedHashMap<String, RegisteredEvent>()

    // False if the id is already taken. Throws on a malformed or reserved id.
    fun register(
        owner: Plugin,
        definition: GameEventDefinition,
    ): Boolean {
        val id = definition.id
        require(ID_PATTERN.matches(id)) { "Invalid game event id '$id': must be 1-32 characters of [a-z0-9_-]" }
        require(id !in RESERVED_IDS) { "Game event id '$id' is reserved" }

        val registered = RegisteredEvent(owner, definition, definition.displayName, definition.voteItem)
        synchronized(events) {
            if (events.containsKey(id)) return false
            events[id] = registered
        }
        Logger.info("Registered game event '$id' (${owner.name})")
        return true
    }

    fun unregister(id: String): Boolean {
        val removed = synchronized(events) { events.remove(id) } ?: return false
        cleanupUnregistered(setOf(id))
        Logger.info("Unregistered game event '$id' (${removed.owner.name})")
        return true
    }

    fun unregisterAll(owner: Plugin): Int {
        val removedIds =
            synchronized(events) {
                val ids = events.filterValues { it.owner === owner }.keys.toSet()
                ids.forEach { events.remove(it) }
                ids
            }
        if (removedIds.isEmpty()) return 0
        cleanupUnregistered(removedIds)
        Logger.info("Unregistered ${removedIds.size} game event(s) of ${owner.name}: ${removedIds.joinToString()}")
        return removedIds.size
    }

    fun get(id: String): GameEventDefinition? = synchronized(events) { events[id]?.definition }

    fun displayNameOf(id: String): Component? = synchronized(events) { events[id]?.displayName }

    fun ids(): List<String> = synchronized(events) { events.keys.toList() }

    fun definitions(): List<GameEventDefinition> = synchronized(events) { events.values.map { it.definition } }

    fun voteEntries(): List<VoteEntry> =
        synchronized(events) {
            events.map { (id, event) -> VoteEntry(id, event.displayName, event.voteItem) }
        }

    // A vote for a gone event would keep blocking its voter from voting again, and an
    // active handler would keep running with its plugin disabled.
    private fun cleanupUnregistered(ids: Set<String>) {
        val arenas = synchronized(arenaManager.onlineArenas) { arenaManager.onlineArenas.values.toList() }
        arenas.forEach { arena ->
            arena.eventVotes.entries.removeIf { it.value in ids }
            if (arena.gameEvent in ids) {
                arena.gameTask.clearActiveEvent()
            }
        }
    }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        unregisterAll(event.plugin)
    }
}
