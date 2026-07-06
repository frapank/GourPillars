package org.gourmet.gourPillars.commands

import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.ZipManager
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.toMini
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission
import java.io.File
import java.io.IOException

data class ArenaEdit(val editor: Player,
                     var name: String?,
                     var minPlayers: Int?,
                     var maxHeight: Int?,
                     var minHeight: Int?,
                     var slowFallingTime: Int?,
                     var deathSpawn: Location?,
                     var regionLocationOne: Location?,
                     var regionLocationSecond: Location?,
                     var locations: MutableMap<Int, Location>)

@Command("edit")
@CommandPermission("gpillars.admin")
object EditCMD {

    private val arenaManager = GourPillars.arenaManager
    private val editingPlayers: MutableMap<Player, ArenaEdit> = mutableMapOf()
    private val zipManager = ZipManager()
    private var isEditing = GourPillars.isEditing

    @Subcommand("start")
    fun startEditing(player: Player) {
        if (editingPlayers.contains(player)) {
            player.sendMessage("<red>You are already editing".toMini())
            return
        }

        editingPlayers[player] = ArenaEdit(player, null, null, null, null, null, null, null, null ,mutableMapOf())
        isEditing = true
        player.sendMessage("<green>You are now editing".toMini())
    }

    @Subcommand("save")
    fun saveArena(player: Player) {
        if (!checkIfEditing(player)) return

        val arenaEdit = editingPlayers[player] ?: return
        val name = arenaEdit.name
        val locations = arenaEdit.locations

        if (name.isNullOrEmpty()) {
            player.sendMessage("<red>You must set a name for the arena!".toMini())
            return
        }
        if (locations.isEmpty()) {
            player.sendMessage("<red>You must set at least one spawn!".toMini())
            return
        }
        if (arenaEdit.minPlayers == null) {
            player.sendMessage("<red>You must set the min-player!".toMini())
            return
        }
        if (arenaEdit.maxHeight == null) {
            player.sendMessage("<red>You must set the max-height!".toMini())
        }
        if (arenaEdit.minHeight == null) {
            player.sendMessage("<red>You must set the min-height!".toMini())
        }
        if (arenaEdit.deathSpawn == null) {
            player.sendMessage("<red>You must set the death spawn!".toMini())
            return
        }
        if (arenaEdit.regionLocationOne == null) {
            player.sendMessage("<red>You must set the first region!".toMini())
            return
        }
        if (arenaEdit.regionLocationSecond == null) {
            player.sendMessage("<red>You must set the second region!".toMini())
            return
        }

        val world = locations.values.first().world
        val worldName = world.name

        world.let {
            it.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
            it.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
            it.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        }

        val arenaConfig = YamlConfiguration()

        arenaConfig.set("world", worldName)
        arenaConfig.set("private-arena", false)
        arenaConfig.set("min-players", arenaEdit.minPlayers)
        arenaConfig.set("max-height", arenaEdit.maxHeight)
        arenaConfig.set("min-height", arenaEdit.minHeight)
        arenaConfig.set("slow-falling-time", arenaEdit.slowFallingTime)

        arenaConfig.set("main-spawn.x", arenaEdit.deathSpawn?.x)
        arenaConfig.set("main-spawn.y", arenaEdit.deathSpawn?.y)
        arenaConfig.set("main-spawn.z", arenaEdit.deathSpawn?.z)
        arenaConfig.set("main-spawn.yaw", arenaEdit.deathSpawn?.yaw)
        arenaConfig.set("main-spawn.pitch", arenaEdit.deathSpawn?.pitch)

        arenaConfig.set("region.loc-1.x", arenaEdit.regionLocationOne?.x)
        arenaConfig.set("region.loc-1.y", arenaEdit.regionLocationOne?.y)
        arenaConfig.set("region.loc-1.z", arenaEdit.regionLocationOne?.z)

        arenaConfig.set("region.loc-2.x", arenaEdit.regionLocationSecond?.x)
        arenaConfig.set("region.loc-2.y", arenaEdit.regionLocationSecond?.y)
        arenaConfig.set("region.loc-2.z", arenaEdit.regionLocationSecond?.z)

        locations.forEach { (index, location) ->
            arenaConfig.set("spawns.$index.x", location.x)
            arenaConfig.set("spawns.$index.y", location.y)
            arenaConfig.set("spawns.$index.z", location.z)
            arenaConfig.set("spawns.$index.yaw", location.yaw)
            arenaConfig.set("spawns.$index.pitch", location.pitch)
        }

        try {
            val arenasFolder = File(GourPillars.instance.dataFolder, "arenas").apply { mkdirs() }
            arenaConfig.save(File(arenasFolder, "$name.yml"))
        } catch (e: IOException) {
            Logger.warning("Failed to save arena '$name': ${e.message}")
            player.sendMessage("<red>Failed to save arena '$name': ${e.message}".toMini())
            return
        }

        player.sendMessage("<green>Arena '$name' saved successfully! Restart the server to load it.".toMini())
        editingPlayers.remove(player)
        zipManager.saveBackup(worldName)
    }

    @Subcommand("setRegionOne")
    fun setRegionOne(player: Player) {
        editingPlayers[player]?.regionLocationOne = player.location
        player.sendMessage("".toMini())
    }

    @Subcommand("setRegionTwo")
    fun setRegionTwo(player: Player) {
        editingPlayers[player]?.regionLocationSecond = player.location
        player.sendMessage("".toMini())
    }

    @Subcommand("setMaxHeight")
    fun setMaxHeight(player: Player) {
        editingPlayers[player]?.maxHeight = player.location.y.toInt()
        player.sendMessage("<green>Height set to ${player.location.y.toInt()}".toMini())
    }

    @Subcommand("setDeathSpawn")
    fun setDeathSpawn(player: Player) {
        editingPlayers[player]?.deathSpawn = player.location
        player.sendMessage("<green>Death spawn set!".toMini())
    }

    @Subcommand("setFallingTime <number>")
    fun setFallingTime(player: Player, number: Int) {
        editingPlayers[player]?.slowFallingTime = number
        player.sendMessage("<green>Height set to number".toMini())
    }

    @Subcommand("setMinHeight")
    fun setMinHeight(player: Player) {
        editingPlayers[player]?.minHeight = player.location.y.toInt()
        player.sendMessage("<green>Height set to ${player.location.y.toInt()}".toMini())
    }

    @Subcommand("stop")
    fun stopEditing(player: Player) {
        if (!checkIfEditing(player)) return

        editingPlayers.remove(player)
        isEditing = false
        player.sendMessage("<green>You are no longer editing".toMini())
    }

    @Subcommand("name <name>")
    fun setName(player: Player, name: String) {
        if (!checkIfEditing(player)) return

        editingPlayers[player]?.name = name
        player.sendMessage("<green>Name set to $name".toMini())
    }
    @Subcommand("minplayers <min>")
    fun setMinPlayers(player: Player, min: Int) {
        if (!checkIfEditing(player)) return

        editingPlayers[player]?.minPlayers = min
        player.sendMessage("<green>Minimum players set to $min".toMini())
    }

    @Subcommand("spawn <number>")
    fun setSpawn(player: Player, number: Int) {
        if (!checkIfEditing(player)) return

        val spawnLocation = player.location
        val locationsEditor = editingPlayers[player]?.locations

        if (locationsEditor != null) {
            for ((index, spawn) in locationsEditor) {
                if (spawn.world != spawnLocation.world) {
                    player.sendMessage("<red>This world differs from the other spawns, invalid operation".toMini())
                    return
                }
                if (spawn.world == GourPillars.spawnManager.getConfiguredWorld()) {
                    player.sendMessage("<red>Don't create an arena in the default world".toMini())
                }
            }
        }

        locationsEditor?.set(number, spawnLocation)
        player.sendMessage("<green>$number added successfully".toMini())
    }

    @Subcommand("check")
    fun checkArena(player: Player) {
        if (!checkIfEditing(player)) return

        val editorName = player.name
        val name = editingPlayers[player]?.name ?: "Not set"
        val locations = editingPlayers[player]?.locations
        player.sendMessage("<yellow>-----------------------------".toMini())
        player.sendMessage("<yellow>Editor <green>-> $editorName".toMini())
        player.sendMessage("<yellow>Name <green>-> $name".toMini())
        player.sendMessage("Arenas:".toMini())
        locations?.forEach { (index, location) ->
            player.sendMessage("<yellow>$index <green>-> ${location.world.name}, ${location.x.toInt()}, ${location.y.toInt()}, ${location.z.toInt()}".toMini())
        }
        player.sendMessage("<yellow>-----------------------------".toMini())
    }

    private fun checkIfEditing(player: Player): Boolean {
        if (!editingPlayers.contains(player)) {
            player.sendMessage("<red>You are not editing".toMini())
            return false
        }
        return true
    }

}