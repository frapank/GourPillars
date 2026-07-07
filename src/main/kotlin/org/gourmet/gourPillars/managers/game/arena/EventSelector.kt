package org.gourmet.gourPillars.managers.game.arena

import org.gourmet.gourPillars.GourPillars
import kotlin.random.Random

object EventSelector {
    fun isEnabled(event: GameEvents): Boolean {
        val path =
            when (event) {
                GameEvents.LAVA -> "game.events.lava.enabled"
                GameEvents.KNOCKBACK -> "game.events.knockback.enabled"
                GameEvents.BORDER -> "game.events.border.enabled"
            }
        return GourPillars.instance.config.getBoolean(path, true)
    }

    fun candidates(): List<GameEvents?> = listOf(null) + GameEvents.entries.filter { isEnabled(it) }

    fun selectWinner(arena: Arena): GameEvents? {
        val config = GourPillars.instance.config
        val baseWeight = config.getDouble("game.events.base-weight", 1.0).coerceAtLeast(0.0)
        val voteWeight = config.getDouble("game.events.vote-weight", 1.0).coerceAtLeast(0.0)

        fun votes(event: GameEvents?): Int =
            when (event) {
                null -> arena.noEventVote.size
                GameEvents.LAVA -> arena.lavaEvent.size
                GameEvents.KNOCKBACK -> arena.knockbackVote.size
                GameEvents.BORDER -> arena.borderEvent.size
            }

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
