package org.gourmet.gourPillars.task

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.EventSelector
import org.gourmet.gourPillars.managers.game.arena.GameEvents
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.messages.MessageData
import java.time.Duration
import kotlin.random.Random

object EventSelectionAnimationTask {
    fun run(
        arena: Arena,
        winner: GameEvents?,
        onComplete: () -> Unit,
    ) {
        val config = GourPillars.instance.config
        val plugin = GourPillars.instance

        if (!config.getBoolean("game.event-selection-animation.enabled", true)) {
            arena.gameTask.applyEvent(winner)
            onComplete()
            return
        }

        val scrollCount = config.getInt("game.event-selection-animation.scroll-count", 16).coerceAtLeast(1)
        val startIntervalTicks = config.getLong("game.event-selection-animation.scroll-interval-start-ticks", 2L).coerceAtLeast(1L)
        val endIntervalTicks =
            config
                .getLong("game.event-selection-animation.scroll-interval-end-ticks", 7L)
                .coerceAtLeast(startIntervalTicks)
        val revealHoldTicks = config.getLong("game.event-selection-animation.reveal-hold-ticks", 40L).coerceAtLeast(1L)

        val scrollSound = readSound(config.getString("game.event-selection-animation.scroll-sound"), Sound.BLOCK_NOTE_BLOCK_PLING)
        val scrollVolume = config.getDouble("game.event-selection-animation.scroll-sound-volume", 0.6).toFloat()
        val scrollPitch = config.getDouble("game.event-selection-animation.scroll-sound-pitch", 1.4).toFloat()

        val revealSound = readSound(config.getString("game.event-selection-animation.reveal-sound"), Sound.BLOCK_NOTE_BLOCK_PLING)
        val revealVolume = config.getDouble("game.event-selection-animation.reveal-sound-volume", 1.0).toFloat()
        val revealPitch = config.getDouble("game.event-selection-animation.reveal-sound-pitch", 2.0).toFloat()

        val pool = EventSelector.candidates()
        if (pool.size <= 1) {
            arena.gameTask.applyEvent(winner)
            onComplete()
            return
        }

        val sequence = MutableList(scrollCount) { pool[Random.nextInt(pool.size)] }
        sequence[sequence.lastIndex] = winner

        fun stepIntervalTicks(step: Int): Long {
            if (scrollCount <= 1) return endIntervalTicks
            val progress = step.toDouble() / (scrollCount - 1)
            return (startIntervalTicks + (endIntervalTicks - startIntervalTicks) * progress).toLong().coerceAtLeast(1L)
        }

        fun isArenaStillStarting(): Boolean = arena.gameState == State.STARTING && arena.inGamePlayer.isNotEmpty()

        fun abort() {
            arena.inGamePlayer.forEach { player -> player.sendTitle("", "") }
        }

        fun playStep(step: Int) {
            if (!isArenaStillStarting()) {
                abort()
                return
            }

            val delay = stepIntervalTicks(step)
            val candidate = sequence[step]
            val title =
                Title.title(
                    eventTitle(candidate),
                    MessageData.ARENA_TITLE_EVENT_SELECT_SUBTITLE.build(),
                    // Stay covers until the next step overrides it, with a small buffer against scheduler jitter
                    Title.Times.times(Duration.ZERO, Duration.ofMillis((delay + 2) * 50L), Duration.ZERO),
                )

            arena.inGamePlayer.forEach { player ->
                player.showTitle(title)
                player.playSound(player.location, scrollSound, scrollVolume, scrollPitch)
            }
            if (step == sequence.lastIndex) {
                object : BukkitRunnable() {
                    override fun run() {
                        if (!isArenaStillStarting()) {
                            abort()
                            return
                        }

                        val revealTitle =
                            Title.title(
                                eventTitle(winner),
                                MessageData.ARENA_TITLE_EVENT_REVEAL_SUBTITLE.build(),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis((revealHoldTicks + 2) * 50L), Duration.ZERO),
                            )
                        arena.inGamePlayer.forEach { player ->
                            player.showTitle(revealTitle)
                            player.playSound(player.location, revealSound, revealVolume, revealPitch)
                        }
                        arena.sendDynamicMessageToPlayerInGame(MessageData.ARENA_EVENT_SELECTED, "{event}" to plainName(winner))
                        arena.gameTask.applyEvent(winner)

                        object : BukkitRunnable() {
                            override fun run() {
                                if (!isArenaStillStarting()) {
                                    abort()
                                    return
                                }
                                onComplete()
                            }
                        }.runTaskLater(plugin, revealHoldTicks)
                    }
                }.runTaskLater(plugin, delay)
                return
            }

            object : BukkitRunnable() {
                override fun run() {
                    playStep(step + 1)
                }
            }.runTaskLater(plugin, delay)
        }

        playStep(0)
    }

    private fun eventTitle(event: GameEvents?): Component =
        when (event) {
            null -> MessageData.GUI_CLASSIC_VOTE_NAME
            GameEvents.LAVA -> MessageData.GUI_LAVA_VOTE_NAME
            GameEvents.KNOCKBACK -> MessageData.GUI_KNOCKBACK_VOTE_NAME
            GameEvents.BORDER -> MessageData.GUI_BORDER_VOTE_NAME
        }

    private fun plainName(event: GameEvents?): String =
        when (event) {
            null -> "No Event"
            GameEvents.LAVA -> "Lava"
            GameEvents.KNOCKBACK -> "Knockback"
            GameEvents.BORDER -> "Border"
        }

    private fun readSound(
        name: String?,
        default: Sound,
    ): Sound {
        if (name == null) return default
        return try {
            Sound.valueOf(name.uppercase())
        } catch (e: IllegalArgumentException) {
            Logger.warning("Invalid sound '$name' in event-selection-animation config, using default")
            default
        }
    }
}
