package org.gourmet.gourPillars

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.gourmet.gourPillars.commands.*
import org.gourmet.gourPillars.data.JsonManager
import org.gourmet.gourPillars.external.PlaceHolderManager
import org.gourmet.gourPillars.listener.*
import org.gourmet.gourPillars.listener.BlockBreakListener
import org.gourmet.gourPillars.listener.ClickItemEvent
import org.gourmet.gourPillars.managers.ArenaManager
import org.gourmet.gourPillars.managers.LobbyScoreboardManager
import org.gourmet.gourPillars.managers.PartyManager
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
        lateinit var jsonManager: JsonManager
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
        jsonManager = JsonManager()
        partyManager = PartyManager()
        spawnManager = SpawnManager()
        ShowPlayerTask().runTaskTimer(this, 0L, 20L)


        arenaManager = ArenaManager()
        Bukkit.getPluginManager().registerEvents(DeathListener(), this)
        Bukkit.getPluginManager().registerEvents(JoinListener(), this)
        Bukkit.getPluginManager().registerEvents(BlockBreakListener(), this)
        Bukkit.getPluginManager().registerEvents(LeaveListener(), this)
        Bukkit.getPluginManager().registerEvents(FallListener(), this)
        Bukkit.getPluginManager().registerEvents(ChatListener(), this)
        Bukkit.getPluginManager().registerEvents(PlaceBlockListener(), this)
        Bukkit.getPluginManager().registerEvents(LevelListener(), this)
        Bukkit.getPluginManager().registerEvents(ClickItemEvent(), this)
        Bukkit.getPluginManager().registerEvents(KnockBackEvent(), this)
        val handler = BukkitLamp.builder(this).build()
        handler.register(
            JoinerCMD,
            TestCMD,
            PartyCMD,
            EditCMD,
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

    // Lista
    /*
        - una
        - due
     */

}
