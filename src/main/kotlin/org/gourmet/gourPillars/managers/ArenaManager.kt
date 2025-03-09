package org.gourmet.gourPillars.managers

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.other.Region
import java.io.File
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
        val plugin = GourPillars.instance

        val dataFolder = plugin.dataFolder
        val arenaFolder = File(dataFolder, "arena")

        if (!arenaFolder.exists() || !arenaFolder.isDirectory){
            Bukkit.getLogger().info("")
            return
        }

        val arenaFile = arenaFolder.listFiles()
        for (file in arenaFile) {
            val current_config: FileConfiguration = YamlConfiguration.loadConfiguration(file)

            //Basic info
            val arenaWorld = Bukkit.getWorld(current_config.getString("basic.world") ?: "world")
            val arenaName = current_config.getString("basic.name") ?: "error_arena_game"
            val minHeight = current_config.getInt("basic.min-height")
            val minPlayer = current_config.getInt("basic.min-players") ?: 2
            val slowFalling = current_config.getInt("basic.slow-falling") ?: 2

            //main-spawn
            var mainSpawn = Location (
                arenaWorld,
                current_config.getDouble("spawns.main-spawn.x"),
                current_config.getDouble("spawns.main-spawn.y"),
                current_config.getDouble("spawns.main-spawn.z"),
                current_config.getDouble("spawns.main-spawn.yaw").toFloat(),
                current_config.getDouble("spawns.main-spawn.pitch").toFloat(),
            )

            //region
            val regionLocOne = Location (
                arenaWorld,
                current_config.getDouble("spawns.regions.loc-1.x"),
                current_config.getDouble("spawns.regions.loc-1.y"),
                current_config.getDouble("spawns.regions.loc-1.z"),
                current_config.getDouble("spawns.regions.loc-1.yaw").toFloat(),
                current_config.getDouble("spawns.regions.loc-1.pitch").toFloat(),
            )
            val regionLocTwo = Location (
                arenaWorld,
                current_config.getDouble("spawns.regions.loc-2.x"),
                current_config.getDouble("spawns.regions.loc-2.y"),
                current_config.getDouble("spawns.regions.loc-2.z"),
                current_config.getDouble("spawns.regions.loc-2.yaw").toFloat(),
                current_config.getDouble("spawns.regions.loc-2.pitch").toFloat(),
            )

            val region = Region.createRegion(regionLocOne, regionLocTwo)

            val spawnsList: MutableMap<Location, Player?> = mutableMapOf()
            val gameSpawnsSection = current_config.getConfigurationSection("spawns.game-spawns")
            gameSpawnsSection?.let { section ->
                for (spawnKey in section.getKeys(false)) {
                    val spawn = section.getConfigurationSection(spawnKey) ?: continue

                    val x = spawn.getDouble("x")
                    val y = spawn.getDouble("y") + 3
                    val z = spawn.getDouble("z")
                    val yaw = spawn.getDouble("yaw", 0.0).toFloat()
                    val pitch = spawn.getDouble("pitch", 0.0).toFloat()

                    val location = Location(arenaWorld, x, y, z, yaw, pitch)

                    spawnsList[location] = null
                }
            } ?: run {
                Bukkit.getLogger().warning("Nessuno spawn di gioco trovato per l'arena $arenaName")
            }

            val arena = Arena(spawnsList, mainSpawn, slowFalling, spawnsList.size, minPlayer, -1, minHeight, regionLocOne, regionLocTwo, region, arenaName)
            Bukkit.getLogger().info("Created new arena $arenaName")
            Bukkit.getLogger().info("advanced: ${arena.spawnMap}")
            onlineArenas[arenaName] = arena
        }
    }
}
