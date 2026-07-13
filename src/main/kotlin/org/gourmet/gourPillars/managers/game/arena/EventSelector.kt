package org.gourmet.gourPillars.managers.game.arena

import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.GameEventRegistry
import kotlin.random.Random

object EventSelector {
    // Every option a match can land on: "no event" (null) plus each registered event id.
    fun candidates(): List<String?> = listOf(null) + GourPillars.gameEventRegistry.ids()

    fun selectWinner(arena: Arena): String? {
        val config = GourPillars.instance.config
        val baseWeight = config.getDouble("game.events.base-weight", 1.0).coerceAtLeast(0.0)
        val voteWeight = config.getDouble("game.events.vote-weight", 1.0).coerceAtLeast(0.0)

        // Votes for an id that is no longer registered simply don't count.
        val voteCounts =
            arena.eventVotes.values
                .groupingBy { it }
                .eachCount()

        fun votes(eventId: String?): Int = voteCounts[eventId ?: GameEventRegistry.NO_EVENT_ID] ?: 0

        val weights = candidates().associateWith { baseWeight + votes(it) * voteWeight }
        val totalWeight = weights.values.sum()
        if (totalWeight <= 0.0) return null

        var roll = Random.nextDouble() * totalWeight
        for ((event, weight) in weights) {
            if (roll < weight) return event
            roll -= weight
        }

        return weights.keys.last()
    }
}
