package org.gourmet.gourPillars.managers.game.arena

enum class GameEvents(
    val voteItemId: String,
) {
    LAVA("lava-event"),
    KNOCKBACK("knockback-event"),
    BORDER("border-event"),
    ;

    companion object {
        const val NO_EVENT_ID = "no-event"

        fun voteItemId(event: GameEvents?): String = event?.voteItemId ?: NO_EVENT_ID

        fun fromVoteItemId(id: String): GameEvents? = entries.find { it.voteItemId == id }
    }
}
