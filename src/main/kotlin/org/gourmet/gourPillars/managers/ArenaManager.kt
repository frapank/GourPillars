package org.gourmet.gourPillars.managers

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena

class ArenaManager {

    private val plugin = GourPillars.instance
    var onlineArenas: MutableMap<String, Arena> = hashMapOf()

    init {
        loadArenas()
    }

    fun getArenaByName(name: String): Arena?{
        return onlineArenas[name]
    }

    fun getArenaByPlayer(player: Player): Arena?{
        for(arena in onlineArenas.values){
            if(arena.containPlayer(player)){
                return arena
            }
        }

        return null
    }


    private fun loadArenas() {
        val config = plugin.config
        val arenasSection = config.getConfigurationSection("Arenas") ?: return

        onlineArenas.clear()
        Bukkit.getLogger().info("[ArenaManager] Inizio caricamento delle arene...")

        for (arenaName in arenasSection.getKeys(false)) {
            val arenaSection = arenasSection.getConfigurationSection(arenaName) ?: continue
            val minPlayers = arenaSection.getInt("min-players")
            val spawnsSection = arenaSection.getConfigurationSection("spawns") ?: continue

            val spawnsList = mutableMapOf<Location, Player?>()
            for (spawnKey in spawnsSection.getKeys(false)) {
                val spawn = spawnsSection.getConfigurationSection(spawnKey) ?: continue
                val world = Bukkit.getWorld(arenaName ?: continue) //?: continue
                if (world == null) {
                    Bukkit.getLogger().warning("[ArenaManager] Mondo non trovato: ${spawn.getString("world")}")
                    Bukkit.getWorlds().forEach{world ->
                        Bukkit.getLogger().warning("  --  ${world.name}")
                    }
                    continue
                }
                val x = spawn.getDouble("x")
                val y = spawn.getDouble("y") + 2
                val z = spawn.getDouble("z")
                val yaw = spawn.getDouble("yaw").toFloat()
                val pitch = spawn.getDouble("pitch").toFloat()
                spawnsList[Location(world, x, y, z, yaw, pitch)] = null
                Bukkit.getLogger().info("[ArenaManager] Spawn aggiunto per arena $arenaName: $world ($x, $y, $z, $yaw, $pitch)")
            }

            val maxPlayers = spawnsList.size
            val arena = Arena(spawnsList, maxPlayers, minPlayers, arenaName)
            onlineArenas[arenaName] = arena
            Bukkit.getLogger().info("[ArenaManager] Arena caricata: $arenaName con $maxPlayers spawn e minimo $minPlayers giocatori.")
        }

        Bukkit.getLogger().info("[ArenaManager] Caricamento delle arene completato. ${onlineArenas.size} arene caricate.")
    }
}
