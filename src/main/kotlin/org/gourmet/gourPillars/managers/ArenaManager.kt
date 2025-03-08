package org.gourmet.gourPillars.managers

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.other.Region
import kotlin.random.Random

class ArenaManager {

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

    fun isPlayerInArena(player: Player): Boolean{
        for((_, arena) in onlineArenas){
            if(arena.waitingPlayer.contains(player)){
                return true
            }
        }
        return false
    }

    fun shuffleArenas() {
        onlineArenas = onlineArenas.entries.shuffled(Random).associate { it.toPair() }.toMutableMap()
    }

    private fun loadArenas() {
        val config = GourPillars.instance.config
        val arenasSection = config.getConfigurationSection("Arenas") ?: return

        onlineArenas.clear()
        Bukkit.getLogger().info("[ArenaManager] Inizio caricamento delle arene...")

        for (arenaName in arenasSection.getKeys(false)) {
            val arenaSection = arenasSection.getConfigurationSection(arenaName) ?: continue
            val minPlayers = arenaSection.getInt("min-players")
            val worldName = arenaSection.getString("world") ?: continue
            val minHeight = arenaSection.getInt("min-height") ?: continue
            val maxHeight = arenaSection.getInt("max-height") ?: continue
            val slowFallingTime = arenaSection.getInt("slow-falling-time") ?: continue
            val world = Bukkit.getWorld(worldName)

            if (world == null) {
                Bukkit.getLogger().warning("[ArenaManager] Mondo non trovato: $worldName")
                continue
            }

            val mainSpawnLocation = Location(
                world,
                arenaSection.getDouble("main-spawn.x"),
                arenaSection.getDouble("main-spawn.y"),
                arenaSection.getDouble("main-spawn.z"),
                arenaSection.getDouble("main-spawn.yaw").toFloat(),
                arenaSection.getDouble("main-spawn.pitch").toFloat(),
            )

            val regionLocOne = Location(
                world,
                arenaSection.getDouble("region.loc-1.x"),
                arenaSection.getDouble("region.loc-1.y"),
                arenaSection.getDouble("region.loc-1.z"),
            )

            val regionLocSecond = Location(
                world,
                arenaSection.getDouble("region.loc-2.x"),
                arenaSection.getDouble("region.loc-2.y"),
                arenaSection.getDouble("region.loc-2.z"),
            )

            val region = Region.createRegion(regionLocOne, regionLocSecond)

            val spawnsSection = arenaSection.getConfigurationSection("spawns") ?: continue
            val spawnsList = mutableMapOf<Location, Player?>()
            for (spawnKey in spawnsSection.getKeys(false)) {
                val spawn = spawnsSection.getConfigurationSection(spawnKey) ?: continue
                val x = spawn.getDouble("x")
                val y = spawn.getDouble("y") + 3
                val z = spawn.getDouble("z")
                val yaw = spawn.getDouble("yaw").toFloat()
                val pitch = spawn.getDouble("pitch").toFloat()
                spawnsList[Location(world, x, y, z, yaw, pitch)] = null
            }

            val maxPlayers = spawnsList.size
            val arena = Arena(spawnsList, mainSpawnLocation, slowFallingTime, maxPlayers, minPlayers, maxHeight, minHeight, regionLocOne, regionLocSecond ,region ,arenaName)
            onlineArenas[arenaName] = arena
            Bukkit.getLogger().info("[ArenaManager] Arena caricata: $arenaName con $maxPlayers spawn e minimo $minPlayers giocatori.")
        }

        Bukkit.getLogger().info("[ArenaManager] Caricamento delle arene completato. ${onlineArenas.size} arene caricate.")
    }

}
