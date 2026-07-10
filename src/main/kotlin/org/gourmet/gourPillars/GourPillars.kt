package org.gourmet.gourPillars

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import org.gourmet.gourPillars.api.GourPillarsAPI
import org.gourmet.gourPillars.api.GourPillarsAPIImpl
import org.gourmet.gourPillars.commands.BuildCMD
import org.gourmet.gourPillars.commands.EditCMD
import org.gourmet.gourPillars.commands.JoinerCMD
import org.gourmet.gourPillars.commands.PartyCMD
import org.gourmet.gourPillars.commands.SetSpawnCMD
import org.gourmet.gourPillars.commands.SpecCMD
import org.gourmet.gourPillars.commands.StatsCMD
import org.gourmet.gourPillars.commands.TestCMD
import org.gourmet.gourPillars.database.Database
import org.gourmet.gourPillars.database.PlayerStats
import org.gourmet.gourPillars.database.checkDatabase
import org.gourmet.gourPillars.listener.game.BorderLimitListener
import org.gourmet.gourPillars.listener.game.GameDeathListener
import org.gourmet.gourPillars.listener.game.KnockbackListener
import org.gourmet.gourPillars.listener.game.QueueDamageListener
import org.gourmet.gourPillars.listener.game.QuitGameListener
import org.gourmet.gourPillars.listener.game.StopBreakStartingListener
import org.gourmet.gourPillars.listener.game.VoidKillListener
import org.gourmet.gourPillars.listener.general.ChatViewListener
import org.gourmet.gourPillars.listener.general.DatabaseListener
import org.gourmet.gourPillars.listener.general.GuiClickListener
import org.gourmet.gourPillars.listener.general.LevelListener
import org.gourmet.gourPillars.listener.general.SpectatorGameModeListener
import org.gourmet.gourPillars.listener.lobby.ItemLobbyListener
import org.gourmet.gourPillars.listener.lobby.JoinListener
import org.gourmet.gourPillars.listener.lobby.WorldChangeListener
import org.gourmet.gourPillars.managers.ConfigManager
import org.gourmet.gourPillars.managers.LobbyScoreboardManager
import org.gourmet.gourPillars.managers.PlaceHolderManager
import org.gourmet.gourPillars.managers.SpawnManager
import org.gourmet.gourPillars.managers.game.ArenaManager
import org.gourmet.gourPillars.managers.party.PartyManager
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.messages.LanguageManager
import org.gourmet.gourPillars.task.ShowPlayerTask
import revxrsal.commands.bukkit.BukkitLamp

// open: MockBukkit needs to subclass this to load it in tests.
open class GourPillars : JavaPlugin() {
    companion object {
        lateinit var instance: GourPillars
        lateinit var arenaManager: ArenaManager
        lateinit var spawnManager: SpawnManager
        lateinit var partyManager: PartyManager
        lateinit var database: Database
        lateinit var lobbyScoreboardManager: LobbyScoreboardManager
        lateinit var languageManager: LanguageManager
        lateinit var api: GourPillarsAPI
        var isEditing = false
        val playersStats = HashMap<Player, PlayerStats>()

        val isDatabaseInitialized get() = ::database.isInitialized
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

        try {
            PlaceHolderManager().register()
        } catch (e: Exception) {
            Logger.warning("Failed to register the PlaceholderAPI expansion: ${e.message}")
        }
    }

    override fun onDisable() {
        server.servicesManager.unregisterAll(this)
        if (isDatabaseInitialized) {
            database.close()
        }
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
        database = checkDatabase()
        partyManager = PartyManager()
        spawnManager = SpawnManager()
        arenaManager = ArenaManager()
        lobbyScoreboardManager = LobbyScoreboardManager()

        api = GourPillarsAPIImpl(arenaManager)
        server.servicesManager.register(GourPillarsAPI::class.java, api, this, ServicePriority.Normal)

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
                QueueDamageListener(),
                QuitGameListener(),
                StopBreakStartingListener(),
                VoidKillListener(),
                // general
                ChatViewListener(),
                DatabaseListener(),
                GuiClickListener(),
                LevelListener(),
                SpectatorGameModeListener(),
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
            SpecCMD,
        )
    }
}
