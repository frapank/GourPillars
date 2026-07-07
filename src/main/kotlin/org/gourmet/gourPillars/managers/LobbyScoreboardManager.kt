package org.gourmet.gourPillars.managers

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.gourmet.gourPillars.GourPillars

class LobbyScoreboardManager {
    private val scoreboards: MutableMap<Player, Scoreboard> = mutableMapOf()
    private val miniMessage = MiniMessage.miniMessage()
    private lateinit var langCfg: FileConfiguration

    fun setScoreboard(player: Player) {
        langCfg = GourPillars.languageManager.getLanguageConfig()

        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val title = PlaceholderAPI.setPlaceholders(player, langCfg.getString("scoreboard.lobby.title", "Pillars Of Fortune")!!)
        val objective = scoreboard.registerNewObjective("game", "dummy", miniMessage.deserialize(title))
        objective.displaySlot = DisplaySlot.SIDEBAR

        val lines = langCfg.getStringList("scoreboard.lobby.lines")
        setLines(player, objective, lines)

        player.scoreboard = scoreboard
        scoreboards[player] = scoreboard
    }

    private fun setLines(
        player: Player,
        objective: Objective,
        lines: List<String>,
    ) {
        var lineNumber = lines.size
        for (line in lines) {
            val parsedLine = PlaceholderAPI.setPlaceholders(player, line)
            val componentText: Component = miniMessage.deserialize(parsedLine)

            val teamName = "line$lineNumber"
            val team = objective.scoreboard?.getTeam(teamName) ?: objective.scoreboard?.registerNewTeam(teamName)
            team?.prefix(componentText)
            team?.addEntry("§$lineNumber")
            objective.getScore("§$lineNumber").score = lineNumber
            lineNumber--
        }
    }
}
