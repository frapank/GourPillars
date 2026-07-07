@file:Suppress("DEPRECATION")

package org.gourmet.gourPillars.other.messages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.miniMessage

class DynamicMessage(
    private val rawMessage: String,
) {
    private val replacements = mutableMapOf<String, String>()

    fun replace(
        placeholder: String,
        value: String,
    ): DynamicMessage {
        replacements[placeholder] = value
        return this
    }

    fun build(vararg pairs: Pair<String, String>): Component {
        var processed = rawMessage
        replacements.forEach { (k, v) -> processed = processed.replace(k, v) }
        pairs.forEach { (k, v) -> processed = processed.replace(k, v) }
        return miniMessage.deserialize(processed)
    }
}

object MessageData {
    private lateinit var config: FileConfiguration
    private val miniMessage: MiniMessage = MiniMessage.miniMessage()

    // Prefix
    lateinit var PREFIX_GAME_STRING: String
    lateinit var PREFIX_PARTY_STRING: String

    // Game
    lateinit var WIN_GAME: DynamicMessage
    lateinit var END_GAME: DynamicMessage

    // Party
    lateinit var PARTY_PARTY_CREATED: DynamicMessage
    lateinit var PARTY_INVITE: DynamicMessage
    lateinit var PARTY_INVITE_RECEIVE: DynamicMessage
    lateinit var PARTY_PARTY_DISBAND: DynamicMessage
    lateinit var PARTY_PARTY_LEAVE: DynamicMessage
    lateinit var PARTY_PLAYER_JOINED: DynamicMessage
    lateinit var PARTY_PLAYER_JOINED_BROADCAST: DynamicMessage
    lateinit var PARTY_PARTY_PROMOTE: DynamicMessage
    lateinit var PARTY_USER_LEFT_PARTY: DynamicMessage
    lateinit var PARTY_PARTY_PROMOTE_BROADCAST: DynamicMessage
    lateinit var PARTY_PARTY_INFO: DynamicMessage
    lateinit var PARTY_PARTY_INFO_NO_MEMBERS: DynamicMessage
    lateinit var PARTY_PARTY_COMMAND_HELP: DynamicMessage

    // Party Errors
    lateinit var PARTY_ERRORS_USER_ALREADY_IN_PARTY: DynamicMessage
    lateinit var PARTY_ERRORS_CANT_INVITE_YOURSELF: DynamicMessage
    lateinit var PARTY_ERRORS_NO_PARTY_REQUEST: DynamicMessage
    lateinit var PARTY_ERRORS_PLAYER_NOT_IN_PARTY: DynamicMessage
    lateinit var PARTY_ERRORS_MAX_PARTY_MEMBER: DynamicMessage
    lateinit var PARTY_ERRORS_NOT_PARTY_ADMIN: DynamicMessage
    lateinit var PARTY_ERRORS_USER_IN_PARTY: DynamicMessage
    lateinit var PARTY_ERRORS_NOT_IN_PARTY: DynamicMessage
    lateinit var PARTY_ERRORS_NOT_THE_OWNER: DynamicMessage
    lateinit var PARTY_ERRORS_ALREADY_IN_PARTY: DynamicMessage
    lateinit var PARTY_ERRORS_TARGET_NOT_IN_PARTY: DynamicMessage
    lateinit var PARTY_ERRORS_INVITE_EXPIRED: DynamicMessage

    // Arena
    lateinit var ARENA_JOIN: DynamicMessage
    lateinit var ARENA_LEAVE: DynamicMessage
    lateinit var ARENA_PLAYER_LEFT: DynamicMessage
    lateinit var ARENA_PLAYER_NEEDED: DynamicMessage
    lateinit var ARENA_TITLE_START: DynamicMessage
    lateinit var ARENA_SUBTITLE_START: DynamicMessage
    lateinit var ARENA_TITLE_END: DynamicMessage
    lateinit var ARENA_SUBTITLE_END: DynamicMessage
    lateinit var ARENA_PLAYER_ELIMINATED: DynamicMessage
    lateinit var ARENA_PLAYER_ELIMINATED_FALL: DynamicMessage
    lateinit var ARENA_PLAYER_ELIMINATED_KILL: DynamicMessage
    lateinit var ARENA_PLAYER_ELIMINATED_VOID_ATTACK: DynamicMessage
    lateinit var ARENA_PLAYER_ELIMINATED_VOID: DynamicMessage
    lateinit var ARENA_PLAYER_ELIMINATED_MOB: DynamicMessage

    lateinit var ARENA_TITLE_COUNTDOWN_OTHER: DynamicMessage
    lateinit var ARENA_TITLE_COUNTDOWN_5: DynamicMessage
    lateinit var ARENA_TITLE_COUNTDOWN_4: DynamicMessage
    lateinit var ARENA_TITLE_COUNTDOWN_3: DynamicMessage
    lateinit var ARENA_TITLE_COUNTDOWN_2: DynamicMessage
    lateinit var ARENA_TITLE_COUNTDOWN_1: DynamicMessage
    lateinit var ARENA_TITLE_EVENT_SELECT_SUBTITLE: DynamicMessage
    lateinit var ARENA_TITLE_EVENT_REVEAL_SUBTITLE: DynamicMessage

    lateinit var ARENA_EVENT_SELECTED: DynamicMessage

    // Arena Errors
    lateinit var ARENA_ERRORS_ALREADY_IN_GAME: DynamicMessage
    lateinit var ARENA_ERRORS_ARENA_NOT_READY: DynamicMessage
    lateinit var ARENA_ERRORS_THE_GAME_IS_FULL: DynamicMessage
    lateinit var ARENA_ERRORS_LIMIT_REACHED: DynamicMessage

    // Arena Vote
    lateinit var ARENA_VOTE_CLASSIC_VOTED: DynamicMessage
    lateinit var ARENA_VOTE_LAVA_VOTED: DynamicMessage
    lateinit var ARENA_VOTE_KNOCKBACK_VOTED: DynamicMessage
    lateinit var ARENA_VOTE_BORDER_VOTED: DynamicMessage
    lateinit var ARENA_VOTE_NIGHT_VOTED: DynamicMessage
    lateinit var ARENA_VOTE_DAY_VOTED: DynamicMessage
    lateinit var ARENA_VOTE_ALREADY_VOTED_EVENT: DynamicMessage
    lateinit var ARENA_VOTE_ALREADY_VOTED_TIME: DynamicMessage
    lateinit var ARENA_VOTE_EVENT_DISABLED: DynamicMessage

    // Gui
    lateinit var GUI_VOTE_TITLE: Component
    lateinit var GUI_ITEM_FILLER_NAME: Component
    lateinit var GUI_CLASSIC_VOTE_NAME: Component
    lateinit var GUI_CLASSIC_VOTE_LORE: Component
    lateinit var GUI_KNOCKBACK_VOTE_NAME: Component
    lateinit var GUI_KNOCKBACK_VOTE_LORE: Component
    lateinit var GUI_BORDER_VOTE_NAME: Component
    lateinit var GUI_BORDER_VOTE_LORE: Component
    lateinit var GUI_LAVA_VOTE_NAME: Component
    lateinit var GUI_LAVA_VOTE_LORE: Component
    lateinit var GUI_DAY_VOTE_NAME: Component
    lateinit var GUI_DAY_VOTE_LORE: Component
    lateinit var GUI_NIGHT_VOTE_NAME: Component
    lateinit var GUI_NIGHT_VOTE_LORE: Component

    lateinit var GUI_STATS_TITLE: Component
    lateinit var GUI_STATS_FILLER_NAME: Component
    lateinit var GUI_STATS_WINS_NAME: Component
    lateinit var GUI_STATS_WINS_LORE: Component
    lateinit var GUI_STATS_KILLS_NAME: Component
    lateinit var GUI_STATS_KILLS_LORE: Component
    lateinit var GUI_STATS_DEFEATS_NAME: Component
    lateinit var GUI_STATS_DEFEATS_LORE: Component
    lateinit var GUI_STATS_GAMESPLAYED_NAME: Component
    lateinit var GUI_STATS_GAMESPLAYED_LORE: Component
    lateinit var GUI_STATS_WINSTREAK_NAME: Component
    lateinit var GUI_STATS_WINSTREAK_LORE: Component

    // Items
    lateinit var WAITING_ITEMS_VOTE_NAME: Component
    lateinit var WAITING_ITEMS_VOTE_LORE: Component
    lateinit var WAITING_ITEMS_LEAVE_NAME: Component
    lateinit var WAITING_ITEMS_LEAVE_LORE: Component

    // Scoreboard
    lateinit var SCOREBOARD_LOBBY_TITLE: Component
    lateinit var SCOREBOARD_LOBBY_LINES: Component
    lateinit var SCOREBOARD_WAITING_TITLE: DynamicMessage
    lateinit var SCOREBOARD_WAITING_LINES: DynamicMessage
    lateinit var SCOREBOARD_PLAYING_TITLE: DynamicMessage
    lateinit var SCOREBOARD_PLAYING_LINES: DynamicMessage

    // stats
    lateinit var STATS_USER: DynamicMessage
    lateinit var STATS_TARGET: DynamicMessage

    // stats-leave cmd
    lateinit var JOIN_LEAVE_ERRORS_ARENA_EDIT: DynamicMessage
    lateinit var JOIN_LEAVE_ERRORS_ARENA_NOT_EXIST: DynamicMessage
    lateinit var JOIN_LEAVE_ERRORS_ARENA_NOT_AVAILABLE: DynamicMessage
    lateinit var JOIN_LEAVE_ERRORS_NOT_IN_ARENA: DynamicMessage
    lateinit var JOIN_LEAVE_ERRORS_USER_IN_PARTY: DynamicMessage
    lateinit var JOIN_LEAVE_ERRORS_ALREADY_BEST_ARENA: DynamicMessage
    lateinit var JOIN_LEAVE_ERRORS_WAIT: DynamicMessage

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
        ARENA_PLAYER_LEFT = getMessage(config, "arena.player-left")
        ARENA_PLAYER_NEEDED = getMessage(config, "arena.player-needed")
        ARENA_PLAYER_ELIMINATED = getMessage(config, "arena.player-eliminated")
        ARENA_PLAYER_ELIMINATED_FALL = getMessage(config, "arena.player-eliminated-fall")
        ARENA_PLAYER_ELIMINATED_KILL = getMessage(config, "arena.player-eliminated-kill")
        ARENA_PLAYER_ELIMINATED_VOID_ATTACK = getMessage(config, "arena.player-eliminated-void-attack")
        ARENA_PLAYER_ELIMINATED_VOID = getMessage(config, "arena.player-eliminated-void")
        ARENA_PLAYER_ELIMINATED_MOB = getMessage(config, "arena.player-eliminated-mob")

        // Arena Titles
        ARENA_TITLE_START = getMessage(config, "arena.title.start-title")
        ARENA_SUBTITLE_START = getMessage(config, "arena.title.start-subtitle")
        ARENA_TITLE_END = getMessage(config, "arena.title.end-title")
        ARENA_SUBTITLE_END = getMessage(config, "arena.title.end-subtitle")
        ARENA_TITLE_COUNTDOWN_OTHER = getMessage(config, "arena.title.countdown-other")
        ARENA_TITLE_COUNTDOWN_5 = getMessage(config, "arena.title.countdown-5")
        ARENA_TITLE_COUNTDOWN_4 = getMessage(config, "arena.title.countdown-4")
        ARENA_TITLE_COUNTDOWN_3 = getMessage(config, "arena.title.countdown-3")
        ARENA_TITLE_COUNTDOWN_2 = getMessage(config, "arena.title.countdown-2")
        ARENA_TITLE_COUNTDOWN_1 = getMessage(config, "arena.title.countdown-1")
        ARENA_TITLE_EVENT_SELECT_SUBTITLE = getMessage(config, "arena.title.event-select-subtitle")
        ARENA_TITLE_EVENT_REVEAL_SUBTITLE = getMessage(config, "arena.title.event-reveal-subtitle")

        ARENA_EVENT_SELECTED = getMessage(config, "arena.event-selected")

        // Arena Vote
        ARENA_VOTE_CLASSIC_VOTED = getMessage(config, "arena.vote.classic-voted")
        ARENA_VOTE_LAVA_VOTED = getMessage(config, "arena.vote.lava-voted")
        ARENA_VOTE_KNOCKBACK_VOTED = getMessage(config, "arena.vote.knockback-voted")
        ARENA_VOTE_BORDER_VOTED = getMessage(config, "arena.vote.border-voted")
        ARENA_VOTE_NIGHT_VOTED = getMessage(config, "arena.vote.night-voted")
        ARENA_VOTE_DAY_VOTED = getMessage(config, "arena.vote.day-voted")
        ARENA_VOTE_ALREADY_VOTED_EVENT = getMessage(config, "arena.vote.already-voted-event")
        ARENA_VOTE_ALREADY_VOTED_TIME = getMessage(config, "arena.vote.already-voted-time")
        ARENA_VOTE_EVENT_DISABLED = getMessage(config, "arena.vote.event-disabled")

        // Party
        PARTY_PARTY_CREATED = getMessage(config, "party.party-created")
        PARTY_INVITE = getMessage(config, "party.party-invited")
        PARTY_INVITE_RECEIVE = getMessage(config, "party.party-invite-receive")
        PARTY_PARTY_DISBAND = getMessage(config, "party.party-disband")
        PARTY_PARTY_LEAVE = getMessage(config, "party.party-leave")
        PARTY_PLAYER_JOINED = getMessage(config, "party.player-joined")
        PARTY_PLAYER_JOINED_BROADCAST = getMessage(config, "party.player-joined-broadcast")
        PARTY_PARTY_PROMOTE = getMessage(config, "party.party-promote")
        PARTY_USER_LEFT_PARTY = getMessage(config, "party.user-left-party")
        PARTY_PARTY_PROMOTE_BROADCAST = getMessage(config, "party.party-promote-broadcast")
        PARTY_PARTY_INFO = getMessage(config, "party.party-info")
        PARTY_PARTY_INFO_NO_MEMBERS = getMessage(config, "party.party-info-no-members")
        PARTY_PARTY_COMMAND_HELP = getMessage(config, "party.command-help")

        // Party Errors
        PARTY_ERRORS_USER_ALREADY_IN_PARTY = getMessage(config, "party.errors.user-already-in-party")
        PARTY_ERRORS_CANT_INVITE_YOURSELF = getMessage(config, "party.errors.cant-invite-yourself")
        PARTY_ERRORS_NO_PARTY_REQUEST = getMessage(config, "party.errors.no-party-request")
        PARTY_ERRORS_PLAYER_NOT_IN_PARTY = getMessage(config, "party.errors.player-not-in-party")
        PARTY_ERRORS_MAX_PARTY_MEMBER = getMessage(config, "party.errors.max-party-member")
        PARTY_ERRORS_NOT_PARTY_ADMIN = getMessage(config, "party.errors.not-party-admin")
        PARTY_ERRORS_USER_IN_PARTY = getMessage(config, "party.errors.user-in-party")
        PARTY_ERRORS_NOT_IN_PARTY = getMessage(config, "party.errors.not-in-party")
        PARTY_ERRORS_NOT_THE_OWNER = getMessage(config, "party.errors.not-the-owner")
        PARTY_ERRORS_ALREADY_IN_PARTY = getMessage(config, "party.errors.already-in-party")
        PARTY_ERRORS_TARGET_NOT_IN_PARTY = getMessage(config, "party.errors.target-not-in-party")
        PARTY_ERRORS_INVITE_EXPIRED = getMessage(config, "party.errors.party-invite-expired")

        // Arena Errors
        ARENA_ERRORS_ALREADY_IN_GAME = getMessage(config, "arena.errors.already-in-game")
        ARENA_ERRORS_ARENA_NOT_READY = getMessage(config, "arena.errors.arena-not-ready")
        ARENA_ERRORS_THE_GAME_IS_FULL = getMessage(config, "arena.errors.the-game-is-full")
        ARENA_ERRORS_LIMIT_REACHED = getMessage(config, "arena.errors.limit-reached")

        // Gui
        GUI_VOTE_TITLE = getMessageComponent(config, "gui.vote.title")
        GUI_ITEM_FILLER_NAME = getMessageComponent(config, "gui.vote.item-filler-name")
        GUI_CLASSIC_VOTE_NAME = getMessageComponent(config, "gui.vote.classic-vote-name")
        GUI_CLASSIC_VOTE_LORE = getMessageComponent(config, "gui.vote.classic-vote-lore")
        GUI_KNOCKBACK_VOTE_NAME = getMessageComponent(config, "gui.vote.knockback-vote-name")
        GUI_KNOCKBACK_VOTE_LORE = getMessageComponent(config, "gui.vote.knockback-vote-lore")
        GUI_BORDER_VOTE_NAME = getMessageComponent(config, "gui.vote.border-vote-name")
        GUI_BORDER_VOTE_LORE = getMessageComponent(config, "gui.vote.border-vote-lore")
        GUI_LAVA_VOTE_NAME = getMessageComponent(config, "gui.vote.lava-vote-name")
        GUI_LAVA_VOTE_LORE = getMessageComponent(config, "gui.vote.lava-vote-lore")
        GUI_DAY_VOTE_NAME = getMessageComponent(config, "gui.vote.day-vote-name")
        GUI_DAY_VOTE_LORE = getMessageComponent(config, "gui.vote.day-vote-lore")
        GUI_NIGHT_VOTE_NAME = getMessageComponent(config, "gui.vote.night-vote-name")
        GUI_NIGHT_VOTE_LORE = getMessageComponent(config, "gui.vote.night-vote-lore")

        GUI_STATS_TITLE = getMessageComponent(config, "gui.stats.title")
        GUI_STATS_FILLER_NAME = getMessageComponent(config, "gui.stats.item-filler-name")
        GUI_STATS_WINS_NAME = getMessageComponent(config, "gui.stats.wins-name")
        GUI_STATS_WINS_LORE = getMessageComponent(config, "gui.stats.wins-lore")
        GUI_STATS_KILLS_NAME = getMessageComponent(config, "gui.stats.kills-name")
        GUI_STATS_KILLS_LORE = getMessageComponent(config, "gui.stats.kills-lore")
        GUI_STATS_DEFEATS_NAME = getMessageComponent(config, "gui.stats.defeats-name")
        GUI_STATS_DEFEATS_LORE = getMessageComponent(config, "gui.stats.defeats-lore")
        GUI_STATS_GAMESPLAYED_NAME = getMessageComponent(config, "gui.stats.gamesplayed-name")
        GUI_STATS_GAMESPLAYED_LORE = getMessageComponent(config, "gui.stats.gamesplayed-lore")
        GUI_STATS_WINSTREAK_NAME = getMessageComponent(config, "gui.stats.winstreak-name")
        GUI_STATS_WINSTREAK_LORE = getMessageComponent(config, "gui.stats.winstreak-lore")

        // Items
        WAITING_ITEMS_VOTE_NAME = getMessageComponent(config, "items.waiting.vote-name")
        WAITING_ITEMS_VOTE_LORE = getMessageComponent(config, "items.waiting.vote-lore")
        WAITING_ITEMS_LEAVE_NAME = getMessageComponent(config, "items.waiting.leave-name")
        WAITING_ITEMS_LEAVE_LORE = getMessageComponent(config, "items.waiting.leave-name")

        // Scoreboard
        SCOREBOARD_LOBBY_TITLE = getMessageComponent(config, "scoreboard.lobby.title")
        SCOREBOARD_LOBBY_LINES = getMessageComponent(config, "scoreboard.lobby.lines")
        SCOREBOARD_WAITING_TITLE = getMessage(config, "scoreboard.waiting.title")
        SCOREBOARD_WAITING_LINES = getMessage(config, "scoreboard.waiting.lines")
        SCOREBOARD_PLAYING_TITLE = getMessage(config, "scoreboard.playing.title")
        SCOREBOARD_PLAYING_LINES = getMessage(config, "scoreboard.playing.lines")

        // stats
        STATS_USER = getMessage(config, "stats.stats-user")
        STATS_TARGET = getMessage(config, "stats.stats-target")

        // join-leave cmd
        JOIN_LEAVE_ERRORS_ARENA_EDIT = getMessage(config, "join-leave-cmd.errors.arena-editing")
        JOIN_LEAVE_ERRORS_ARENA_NOT_EXIST = getMessage(config, "join-leave-cmd.errors.arena-not-exist")
        JOIN_LEAVE_ERRORS_ARENA_NOT_AVAILABLE = getMessage(config, "join-leave-cmd.errors.arena-not-available")
        JOIN_LEAVE_ERRORS_NOT_IN_ARENA = getMessage(config, "join-leave-cmd.errors.not-in-arena")
        JOIN_LEAVE_ERRORS_USER_IN_PARTY = getMessage(config, "join-leave-cmd.errors.user-in-party")
        JOIN_LEAVE_ERRORS_ALREADY_BEST_ARENA = getMessage(config, "join-leave-cmd.errors.already-best-arena")
        JOIN_LEAVE_ERRORS_WAIT = getMessage(config, "join-leave-cmd.errors.wait")
    }

    private fun getMessage(
        config: FileConfiguration,
        path: String,
    ): DynamicMessage =
        when (val value = config.get(path)) {
            is String -> DynamicMessage(processString(value))
            is List<*> -> DynamicMessage(processList(value))
            else -> DynamicMessage("<red>Messaggio non trovato!")
        }

    private fun getMessageComponent(
        config: FileConfiguration,
        path: String,
    ): Component =
        when (val value = config.get(path)) {
            is String -> {
                miniMessage.deserialize(processString(value))
            }

            is List<*> -> {
                val components = (value as List<String>).map { miniMessage.deserialize(processString(it)) }
                Component.text().append(components).build()
            }

            else -> {
                miniMessage.deserialize("<red>Messaggio non trovato!")
            }
        }

    private fun processString(value: String): String =
        value
            .replace("{prefix_game}", PREFIX_GAME_STRING)
            .replace("{prefix_party}", PREFIX_PARTY_STRING)

    private fun processList(value: List<*>): String =
        value
            .filterIsInstance<String>()
            .joinToString("\n") { line ->
                line
                    .replace("{prefix_game}", PREFIX_GAME_STRING)
                    .replace("{prefix_party}", PREFIX_PARTY_STRING)
            }
}

// Extension functions per l'invio
fun Player.sendDynamicMessage(
    message: DynamicMessage,
    vararg replacements: Pair<String, String>,
) {
    this.sendMessage(message.build(*replacements))
}

fun Player.sendDynamicMessage(message: DynamicMessage) {
    this.sendMessage(message.build())
}

fun Player.sendDynamicTitle(
    title: DynamicMessage,
    subTitle: DynamicMessage,
) {
    val mainTitle: Component = title.build()
    val subTitle: Component = subTitle.build()

    val title: Title = Title.title(mainTitle, subTitle)

    this.showTitle(title)
}

fun Player.sendDynamicTitle(
    title: DynamicMessage,
    subTitle: DynamicMessage,
    vararg subTitleReplace: Pair<String, String>,
) {
    val mainTitle: Component = title.build()
    val subTitle: Component = subTitle.build(*subTitleReplace)

    val title: Title = Title.title(mainTitle, subTitle)

    this.showTitle(title)
}
