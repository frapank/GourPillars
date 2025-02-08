package org.gourmet.gourPillars

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.gourmet.gourPillars.commands.EditCMD
import org.gourmet.gourPillars.commands.JoinerCMD
import org.gourmet.gourPillars.commands.TestCMD
import org.gourmet.gourPillars.external.PlaceHolderManager
import org.gourmet.gourPillars.listener.*
import org.gourmet.gourPillars.managers.ArenaManager
import org.gourmet.gourPillars.managers.GameScoreboardManager
import org.gourmet.gourPillars.managers.LobbyScoreboardManager
import org.gourmet.gourPillars.managers.SpawnManager
import revxrsal.commands.bukkit.BukkitLamp

class GourPillars : JavaPlugin() {

    companion object{
        lateinit var instance: GourPillars
        lateinit var arenaManager: ArenaManager
        lateinit var spawnManager: SpawnManager
        lateinit var lobbyScoreboardManager: LobbyScoreboardManager
        var isEditing = false
    }

    override fun onEnable() {
        saveDefaultConfig()
        instance = this
        spawnManager = SpawnManager()


        arenaManager = ArenaManager()
        Bukkit.getPluginManager().registerEvents(DeathListener(), this)
        Bukkit.getPluginManager().registerEvents(JoinListener(), this)
        Bukkit.getPluginManager().registerEvents(BeakBlockListener(), this)
        Bukkit.getPluginManager().registerEvents(LeaveListener(), this)
        Bukkit.getPluginManager().registerEvents(FallDeathListener(), this)
        val handler = BukkitLamp.builder(this).build()
        handler.register(
            JoinerCMD,
            TestCMD,
            EditCMD
        )

        lobbyScoreboardManager = LobbyScoreboardManager()
        placeHolderInit()

    }

    private fun placeHolderInit() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            logger.severe("Missing PlaceHolderAPI")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        PlaceHolderManager().register()
    }

    /*
        Estetica codice
        Gestione eliminazioni fix
        Letti dormire fix
        Blocchi non validi fix
     */

}
