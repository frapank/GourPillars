package org.gourmet.gourPillars.api.event

import net.kyori.adventure.text.Component

// A match event that other plugins register through GourPillarsAPI.registerEvent.
// One plugin can register any number of events; each shows up in the vote GUI and
// the pre-match selection animation, and can be picked (weighted by votes) when a
// match starts.
//
// The id must be unique across all plugins, 1-32 characters of [a-z0-9_-], and not
// one of the reserved vote ids ("no-event", "day-vote", "night-vote"). It is what
// GourPillarsAPI.getCurrentEvent and GourPillarsEventSelectedEvent report.
interface GameEventDefinition {
    val id: String

    // Shown in the vote GUI, the selection animation and chat messages.
    val displayName: Component

    // How the event appears in the vote GUI. The item name is always displayName.
    val voteItem: VoteItemSpec get() = VoteItemSpec()

    // Called once per match where this event is picked, right after the vote closes.
    // A new handler is created for every match, so multiple arenas can run the same
    // event at the same time, each with its own handler instance.
    //
    // Return GameEventHandler.EMPTY for passive events with no per-match state that
    // only react to Bukkit events (check GourPillarsAPI.getCurrentEvent, or
    // ArenaInfo.currentEvent, to know whether they're active in an arena).
    fun createHandler(context: GameEventContext): GameEventHandler
}
