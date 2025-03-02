@file:Suppress("DEPRECATION")

package org.gourmet.gourPillars.other.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.FileConfiguration
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.miniMessage
import org.gourmet.gourPillars.other.toMini
object MessageData {
    private lateinit var config: FileConfiguration
    private val miniMessage: MiniMessage = MiniMessage.miniMessage()

    // Prefix
    lateinit var PREFIX_GAME_STRING: String
    lateinit var PREFIX_PARTY_STRING: String

    // Game
    lateinit var WIN_GAME: Component
    lateinit var END_GAME: Component

    // Arena
    lateinit var ARENA_JOIN: Component
    lateinit var ARENA_LEAVE: Component
    lateinit var ARENA_PLAYER_NEEDED: Component
    lateinit var ARENA_TITLE_START: Component
    lateinit var ARENA_TITLE_END: Component
    lateinit var ARENA_TITLE_COUNTDOWN_OTHER: Component
    lateinit var ARENA_TITLE_COUNTDOWN_5: Component
    lateinit var ARENA_TITLE_COUNTDOWN_4: Component
    lateinit var ARENA_TITLE_COUNTDOWN_3: Component
    lateinit var ARENA_TITLE_COUNTDOWN_2: Component
    lateinit var ARENA_TITLE_COUNTDOWN_1: Component

    // Arena Errors
    lateinit var ARENA_ERRORS_ALREADY_IN_GAME: Component
    lateinit var ARENA_ERRORS_ARENA_NOT_READY: Component
    lateinit var ARENA_ERRORS_THE_GAME_IS_FULL: Component

    // Scoreboard
    lateinit var SCOREBOARD_LOBBY_TITLE: Component
    lateinit var SCOREBOARD_LOBBY_LINES: Component
    lateinit var SCOREBOARD_WAITING_TITLE: Component
    lateinit var SCOREBOARD_WAITING_LINES: Component
    lateinit var SCOREBOARD_PLAYING_TITLE: Component
    lateinit var SCOREBOARD_PLAYING_LINES: Component

    fun load() {
        config = GourPillars.languageManager.getLanguageConfig()

        // Prefix
        PREFIX_GAME_STRING = config.getString("prefix.game") ?: "<prefix.game error>"
        PREFIX_PARTY_STRING = config.getString("prefix.party") ?: "<prefix.party error>"

        // Game
        WIN_GAME = getMessage(config, "game.win-game")
        END_GAME = getMessage(config, "game.end-game")

        // Arena
        ARENA_JOIN = getMessage(config, "arena.join")
        ARENA_LEAVE = getMessage(config, "arena.leave")
        ARENA_PLAYER_NEEDED = getMessage(config, "arena.player-needed")

        // Arena Titles
        ARENA_TITLE_START = getMessage(config, "arena.title.start")
        ARENA_TITLE_END = getMessage(config, "arena.title.end")
        ARENA_TITLE_COUNTDOWN_OTHER = getMessage(config, "arena.title.countdown-other")
        ARENA_TITLE_COUNTDOWN_5 = getMessage(config, "arena.title.countdown-5")
        ARENA_TITLE_COUNTDOWN_4 = getMessage(config, "arena.title.countdown-4")
        ARENA_TITLE_COUNTDOWN_3 = getMessage(config, "arena.title.countdown-3")
        ARENA_TITLE_COUNTDOWN_2 = getMessage(config, "arena.title.countdown-2")
        ARENA_TITLE_COUNTDOWN_1 = getMessage(config, "arena.title.countdown-1")

        // Arena Errors
        ARENA_ERRORS_ALREADY_IN_GAME = getMessage(config, "arena.errors.already-in-game")
        ARENA_ERRORS_ARENA_NOT_READY = getMessage(config, "arena.errors.arena-not-ready")
        ARENA_ERRORS_THE_GAME_IS_FULL = getMessage(config, "arena.errors.the-game-is-full")

        // Scoreboard
        SCOREBOARD_LOBBY_TITLE = getMessage(config, "scoreboard.lobby.title")
        SCOREBOARD_LOBBY_LINES = getMessage(config, "scoreboard.lobby.lines")
        SCOREBOARD_WAITING_TITLE = getMessage(config, "scoreboard.waiting.title")
        SCOREBOARD_WAITING_LINES = getMessage(config, "scoreboard.waiting.lines")
        SCOREBOARD_PLAYING_TITLE = getMessage(config, "scoreboard.playing.title")
        SCOREBOARD_PLAYING_LINES = getMessage(config, "scoreboard.playing.lines")
    }

    private fun getMessage(config: FileConfiguration, path: String): Component {
        return when (val value = config.get(path)) {
            is String -> processString(value)
            is List<*> -> processList(value)
            else -> miniMessage.deserialize("<red>Messaggio non trovato!") // Sostituito §c con <red>
        }
    }

    private fun processString(value: String): Component {
        return miniMessage.deserialize(
            value.replace("{prefix_game}", PREFIX_GAME_STRING)
                .replace("{prefix_party}", PREFIX_PARTY_STRING)
        )
    }

    private fun processList(value: List<*>): Component {
        val processed = value.filterIsInstance<String>()
            .joinToString("\n") { line ->
                line.replace("{prefix_game}", PREFIX_GAME_STRING)
                    .replace("{prefix_party}", PREFIX_PARTY_STRING)
            }
        return if (processed.isEmpty()) Component.empty()
        else miniMessage.deserialize(processed)
    }
}