package org.gourmet.gourPillars.managers.game

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.Region
import java.io.File
import kotlin.random.Random

class ArenaManager {
    var onlineArenas: MutableMap<String, Arena> = hashMapOf()

    private val arenasFolder = File(GourPillars.instance.dataFolder, "arenas")

    init {
        migrateLegacyArenas()
        loadArenas()
    }

    fun getArenaByName(name: String): Arena? = onlineArenas[name]

    fun getArenaByPlayer(player: Player): Arena? {
        for (arena in onlineArenas.values) {
            if (arena.containPlayer(player)) {
                return arena
            }
        }

        return null
    }

    fun isPlayerInArena(player: Player): Boolean {
        for ((_, arena) in onlineArenas) {
            if (arena.inGamePlayer.contains(player)) {
                return true
            }
        }
        return false
    }

    fun getArenaBySpectator(player: Player): Arena? = onlineArenas.values.find { it.spectators.contains(player) }

    fun isSpectating(player: Player): Boolean = getArenaBySpectator(player) != null

    // Excludes private arenas so /spec can't be used to discover or watch them.
    fun getSpectatableArena(name: String): Arena? = onlineArenas.values.find { !it.isPrivate && it.name.equals(name, ignoreCase = true) }

    // Private arenas are excluded: nothing can ever join one (see Arena.addPlayer).
    fun maxArenaCapacity(): Int = onlineArenas.values.filterNot { it.isPrivate }.maxOfOrNull { it.maxPlayer } ?: 0

    fun shuffleArenas() {
        onlineArenas =
            onlineArenas.entries
                .shuffled(Random.Default)
                .associate { it.toPair() }
                .toMutableMap()
    }

    /**
     * Loads every arena file inside the arenas/ folder. Each file is parsed
     * independently: a missing world, a corrupt file or invalid data only
     * skips that single arena (with a warning) instead of failing startup.
     */
    private fun loadArenas() {
        if (!arenasFolder.exists() && !arenasFolder.mkdirs()) {
            Logger.warning("Unable to create the 'arenas' folder")
            return
        }

        val arenaFiles = arenasFolder.listFiles { file -> file.isFile && file.extension.equals("yml", ignoreCase = true) }
        if (arenaFiles == null) {
            Logger.warning("Unable to read the 'arenas' folder")
            return
        }

        if (arenaFiles.isEmpty()) {
            Logger.info("No arena files found in 'arenas/'")
            return
        }

        for (file in arenaFiles) {
            val arenaName = file.nameWithoutExtension
            try {
                val arena = loadArenaFile(file, arenaName) ?: continue
                onlineArenas[arenaName] = arena
                Logger.info("Arena '$arenaName' loaded")
            } catch (e: Exception) {
                Logger.warning("Skipping arena '$arenaName': ${e.message}")
            }
        }
    }

    private fun loadArenaFile(
        file: File,
        arenaName: String,
    ): Arena? {
        val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)

        val worldName = config.getString("world")
        if (worldName.isNullOrBlank()) {
            Logger.warning("Skipping arena '$arenaName': missing 'world'")
            return null
        }
        val world =
            Bukkit.getWorld(worldName) ?: run {
                Logger.warning("Skipping arena '$arenaName': world '$worldName' is not loaded")
                return null
            }

        val isPrivateArena = config.getBoolean("private-arena", false)
        val minHeight = config.getInt("min-height")
        val minPlayer = config.getInt("min-players", 2)
        val slowFalling = config.getInt("slow-falling-time", 1)

        val mainSpawn =
            config.getConfigurationSection("main-spawn")?.toLocation(world) ?: run {
                Logger.warning("Skipping arena '$arenaName': missing or invalid 'main-spawn'")
                return null
            }

        val regionLocOne = config.getConfigurationSection("region.loc-1")?.toLocation(world)
        val regionLocTwo = config.getConfigurationSection("region.loc-2")?.toLocation(world)
        if (regionLocOne == null || regionLocTwo == null) {
            Logger.warning("Skipping arena '$arenaName': missing or invalid region")
            return null
        }
        val region = Region.createRegion(regionLocOne, regionLocTwo)

        val spawnsList: MutableMap<Location, Player?> = mutableMapOf()
        config.getConfigurationSection("spawns")?.let { spawnsSection ->
            for (key in spawnsSection.getKeys(false)) {
                val location = spawnsSection.getConfigurationSection(key)?.toLocation(world) ?: continue
                spawnsList[location] = null
            }
        }

        if (spawnsList.isEmpty()) {
            Logger.warning("Skipping arena '$arenaName': no valid spawns configured")
            return null
        }

        return Arena(
            spawnsList,
            mainSpawn,
            isPrivateArena,
            slowFalling,
            spawnsList.size,
            minPlayer,
            -1,
            minHeight,
            regionLocOne,
            regionLocTwo,
            region,
            arenaName,
        )
    }

    private fun ConfigurationSection.toLocation(world: World): Location? {
        if (!isSet("x") || !isSet("y") || !isSet("z")) return null
        return Location(
            world,
            getDouble("x"),
            getDouble("y"),
            getDouble("z"),
            getDouble("yaw", 0.0).toFloat(),
            getDouble("pitch", 0.0).toFloat(),
        )
    }

    /**
     * Non-destructive fallback: arenas used to live under config.yml's "Arenas"
     * section. Every legacy entry that doesn't already have a file under
     * arenas/ is copied into its own file, so existing setups keep working.
     * The original config.yml section is left untouched. Re-checking per
     * arena (instead of a one-shot "folder exists" gate) means a legacy
     * arena that failed to migrate gets retried on the next startup instead
     * of being silently orphaned forever.
     */
    private fun migrateLegacyArenas() {
        val legacySection = GourPillars.instance.config.getConfigurationSection("Arenas") ?: return
        val legacyNames = legacySection.getKeys(false)
        if (legacyNames.isEmpty()) return

        val pendingNames = legacyNames.filterNot { File(arenasFolder, "$it.yml").exists() }
        if (pendingNames.isEmpty()) return

        if (!arenasFolder.exists() && !arenasFolder.mkdirs()) {
            Logger.warning("Unable to create the 'arenas' folder for migration")
            return
        }

        Logger.info("Migrating ${pendingNames.size} legacy arena(s) from config.yml to 'arenas/'")
        for (name in pendingNames) {
            val legacyArena = legacySection.getConfigurationSection(name) ?: continue
            try {
                migrateLegacyArena(name, legacyArena)
            } catch (e: Exception) {
                Logger.warning("Failed to migrate legacy arena '$name': ${e.message}")
            }
        }
    }

    private fun migrateLegacyArena(
        name: String,
        legacy: ConfigurationSection,
    ) {
        val target = YamlConfiguration()

        target.set("world", legacy.getString("world", "world"))
        target.set("private-arena", legacy.getBoolean("private-arena", false))
        target.set("min-players", legacy.getInt("min-players", 2))
        target.set("max-height", legacy.getInt("max-height"))
        target.set("min-height", legacy.getInt("min-height"))
        target.set("slow-falling-time", legacy.getInt("slow-falling-time", 1))

        copyLocation(legacy, "main-spawn", target, "main-spawn")
        copyLocation(legacy, "region.loc-1", target, "region.loc-1")
        copyLocation(legacy, "region.loc-2", target, "region.loc-2")

        legacy.getConfigurationSection("spawns")?.getKeys(false)?.forEach { key ->
            copyLocation(legacy, "spawns.$key", target, "spawns.$key")
        }

        target.save(File(arenasFolder, "$name.yml"))
        Logger.info("Migrated legacy arena '$name' to arenas/$name.yml")
    }

    private fun copyLocation(
        source: ConfigurationSection,
        sourcePath: String,
        target: YamlConfiguration,
        targetPath: String,
    ) {
        if (!source.isSet("$sourcePath.x")) return
        target.set("$targetPath.x", source.getDouble("$sourcePath.x"))
        target.set("$targetPath.y", source.getDouble("$sourcePath.y"))
        target.set("$targetPath.z", source.getDouble("$sourcePath.z"))
        if (source.isSet("$sourcePath.yaw")) target.set("$targetPath.yaw", source.getDouble("$sourcePath.yaw"))
        if (source.isSet("$sourcePath.pitch")) target.set("$targetPath.pitch", source.getDouble("$sourcePath.pitch"))
    }
}
