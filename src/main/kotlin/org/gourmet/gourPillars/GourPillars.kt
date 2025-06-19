package org.gourmet.gourPillars

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.gourmet.gourPillars.commands.*
import org.gourmet.gourPillars.managers.DatabaseManager
import org.gourmet.gourPillars.managers.PlaceHolderManager
import org.gourmet.gourPillars.listener.*
import org.gourmet.gourPillars.listener.game.StopBreakStartingEvent
import org.gourmet.gourPillars.listener.general.GuiClickEvent
import org.gourmet.gourPillars.listener.game.BorderLimitEvent
import org.gourmet.gourPillars.listener.game.GameDeathEvent
import org.gourmet.gourPillars.listener.game.QuitGameEvent
import org.gourmet.gourPillars.listener.game.VoidKillEvent
import org.gourmet.gourPillars.listener.general.ChatViewEvent
import org.gourmet.gourPillars.listener.general.DatabaseEvent
import org.gourmet.gourPillars.listener.general.LevelEvent
import org.gourmet.gourPillars.listener.lobby.ItemLobbyEvent
import org.gourmet.gourPillars.listener.lobby.JoinEvent
import org.gourmet.gourPillars.listener.lobby.WorldChangeEvent
import org.gourmet.gourPillars.managers.game.ArenaManager
import org.gourmet.gourPillars.managers.LobbyScoreboardManager
import org.gourmet.gourPillars.managers.party.PartyManager
import org.gourmet.gourPillars.managers.SpawnManager
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.messages.LanguageManager
import org.gourmet.gourPillars.task.ShowPlayerTask
import revxrsal.commands.bukkit.BukkitLamp

class GourPillars : JavaPlugin() {


    companion object{
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
        saveDefaultConfig()
        languageManager = LanguageManager()
        languageManager.saveDefaultLanguageFile()

        Logger.info("GourPillars starting...")
        databaseManager = DatabaseManager()

        partyManager = PartyManager()
        spawnManager = SpawnManager()
        arenaManager = ArenaManager()
        ShowPlayerTask().runTaskTimer(this, 100L, 20L)


        Bukkit.getPluginManager().registerEvents(GameDeathEvent(), this)
        Bukkit.getPluginManager().registerEvents(ItemLobbyEvent(), this)
        Bukkit.getPluginManager().registerEvents(WorldChangeEvent(), this)
        Bukkit.getPluginManager().registerEvents(DatabaseEvent(), this)
        Bukkit.getPluginManager().registerEvents(JoinEvent(), this)
        Bukkit.getPluginManager().registerEvents(StopBreakStartingEvent(), this)
        Bukkit.getPluginManager().registerEvents(QuitGameEvent(), this)
        Bukkit.getPluginManager().registerEvents(VoidKillEvent(), this)
        Bukkit.getPluginManager().registerEvents(ChatViewEvent(), this)
        Bukkit.getPluginManager().registerEvents(BorderLimitEvent(), this)
        Bukkit.getPluginManager().registerEvents(LevelEvent(), this)
        Bukkit.getPluginManager().registerEvents(GuiClickEvent(), this)
        Bukkit.getPluginManager().registerEvents(KnockBackEvent(), this)
        val handler = BukkitLamp.builder(this).build()
        handler.register(
            JoinerCMD,
            TestCMD,
            PartyCMD,
            EditCMD,
            BuildCMD,
            StatsCMD
        )

        lobbyScoreboardManager = LobbyScoreboardManager()
        placeHolderInit()

    }

    private fun placeHolderInit() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Logger.warning("Missing PlaceHolderAPI")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        PlaceHolderManager().register()
    }

}
