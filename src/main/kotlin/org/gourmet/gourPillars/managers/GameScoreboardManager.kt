package org.gourmet.gourPillars.managers

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.arena.State

class GameScoreboardManager(private val arena: Arena) {
    private val scoreboards: MutableMap<Player, Scoreboard> = mutableMapOf()
    private val plugin = GourPillars.instance
    private val miniMessage = MiniMessage.miniMessage()
    private val config: FileConfiguration = plugin.config

    fun setWaitingBoard(player: Player) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val title = PlaceholderAPI.setPlaceholders(player, config.getString("scoreboards.titles", "Pillars")!!)
        val objective = scoreboard.registerNewObjective("lobby", "dummy", miniMessage.deserialize(title))
        objective.displaySlot = DisplaySlot.SIDEBAR

        val lines = config.getStringList("scoreboards.waiting")
        setLines(player, objective, lines)

        player.scoreboard = scoreboard
        scoreboards[player] = scoreboard
    }

    fun setGameScoreboard(player: Player) {
        if (arena.gameState != State.INGAME) return

        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val title = PlaceholderAPI.setPlaceholders(player, config.getString("scoreboards.titles", "Pillars")!!)
        val objective = scoreboard.registerNewObjective("game", "dummy", miniMessage.deserialize(title))
        objective.displaySlot = DisplaySlot.SIDEBAR

        val lines = config.getStringList("scoreboards.in-game")
        setLines(player, objective, lines)

        player.scoreboard = scoreboard
        scoreboards[player] = scoreboard
    }

    private fun setLines(player: Player, objective: Objective, lines: List<String>) {
        val scoreboard = objective.scoreboard
        var lineNumber = lines.size

        for (line in lines) {
            val parsedLine = PlaceholderAPI.setPlaceholders(player, line)
            val componentText: Component = miniMessage.deserialize(parsedLine)

            val entry = ChatColor.COLOR_CHAR.toString() + lineNumber

            val teamName = "line_$lineNumber"
            val team = scoreboard?.getTeam(teamName) ?: scoreboard?.registerNewTeam(teamName)

            team?.prefix(componentText)
            team?.addEntry(entry)

            objective.getScore(entry).score = lineNumber

            lineNumber--
        }
    }

}
