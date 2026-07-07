package org.gourmet.gourPillars

import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.gourmet.gourPillars.commands.BuildCMD
import org.gourmet.gourPillars.commands.EditCMD
import org.gourmet.gourPillars.commands.JoinerCMD
import org.gourmet.gourPillars.commands.PartyCMD
import org.gourmet.gourPillars.commands.SetSpawnCMD
import org.gourmet.gourPillars.commands.StatsCMD
import org.gourmet.gourPillars.commands.TestCMD
import org.gourmet.gourPillars.listener.game.BorderLimitListener
import org.gourmet.gourPillars.listener.game.GameDeathListener
import org.gourmet.gourPillars.listener.game.KnockbackListener
import org.gourmet.gourPillars.listener.game.QuitGameListener
import org.gourmet.gourPillars.listener.game.StopBreakStartingListener
import org.gourmet.gourPillars.listener.game.VoidKillListener
import org.gourmet.gourPillars.listener.general.ChatViewListener
import org.gourmet.gourPillars.listener.general.DatabaseListener
import org.gourmet.gourPillars.listener.general.GuiClickListener
import org.gourmet.gourPillars.listener.general.LevelListener
import org.gourmet.gourPillars.listener.lobby.ItemLobbyListener
import org.gourmet.gourPillars.listener.lobby.JoinListener
import org.gourmet.gourPillars.listener.lobby.WorldChangeListener
import org.gourmet.gourPillars.managers.ConfigManager
import org.gourmet.gourPillars.managers.DatabaseManager
import org.gourmet.gourPillars.managers.LobbyScoreboardManager
import org.gourmet.gourPillars.managers.PlaceHolderManager
import org.gourmet.gourPillars.managers.SpawnManager
import org.gourmet.gourPillars.managers.game.ArenaManager
import org.gourmet.gourPillars.managers.party.PartyManager
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.messages.LanguageManager
import org.gourmet.gourPillars.task.ShowPlayerTask
import revxrsal.commands.bukkit.BukkitLamp

class GourPillars : JavaPlugin() {
    companion object {
        lateinit var instance: GourPillars
        lateinit var arenaManager: ArenaManager
        lateinit var spawnManager: SpawnManager
        lateinit var partyManager: PartyManager
        lateinit var databaseManager: DatabaseManager
        lateinit var lobbyScoreboardManager: LobbyScoreboardManager
        lateinit var languageManager: LanguageManager
        var isEditing = false
    }

    override fun onEnable() {
        instance = this

        Logger.info("GourPillars starting...")

        loadConfiguration()
        initializeManagers()
        registerListeners()
        registerCommands()

        if (!placeholderApiPresent()) {
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        PlaceHolderManager().register()
    }

    private fun placeholderApiPresent(): Boolean {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Logger.warning("Missing PlaceholderAPI")
            return false
        }
        return true
    }

    private fun loadConfiguration() {
        saveDefaultConfig()
        ConfigManager.applyMissingDefaults()
        languageManager = LanguageManager()
        languageManager.saveDefaultLanguageFile()
    }

    private fun initializeManagers() {
        databaseManager = DatabaseManager()
        partyManager = PartyManager()
        spawnManager = SpawnManager()
        arenaManager = ArenaManager()
        lobbyScoreboardManager = LobbyScoreboardManager()

        ShowPlayerTask().runTaskTimer(this, 100L, 20L)
    }

    private fun registerListeners() {
        val listeners: List<Listener> =
            listOf(
                // lobby
                ItemLobbyListener(),
                JoinListener(),
                WorldChangeListener(),
                // game
                BorderLimitListener(),
                GameDeathListener(),
                KnockbackListener(),
                QuitGameListener(),
                StopBreakStartingListener(),
                VoidKillListener(),
                // general
                ChatViewListener(),
                DatabaseListener(),
                GuiClickListener(),
                LevelListener(),
            )

        val pluginManager = Bukkit.getPluginManager()
        listeners.forEach { pluginManager.registerEvents(it, this) }
    }

    private fun registerCommands() {
        val lamp = BukkitLamp.builder(this).build()
        lamp.register(
            JoinerCMD,
            TestCMD,
            PartyCMD,
            EditCMD,
            BuildCMD,
            StatsCMD,
            SetSpawnCMD,
        )
    }
}
