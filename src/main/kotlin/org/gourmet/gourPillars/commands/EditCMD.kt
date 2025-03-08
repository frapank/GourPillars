package org.gourmet.gourPillars.commands

import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.ZipManager
import org.gourmet.gourPillars.other.toMini
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission

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
@CommandPermission("gpillars.admim")
object EditCMD {

    private val arenaManager = GourPillars.arenaManager
    private val editingPlayers: MutableMap<Player, ArenaEdit> = mutableMapOf()
    private val zipManager = ZipManager()
    private var isEditing = GourPillars.isEditing

    @Subcommand("start")
    fun startEditing(player: Player){
        if(editingPlayers.contains(player)){
            player.sendMessage("<red>Stai gia editando".toMini())
            return
        }

        editingPlayers[player] = ArenaEdit(player, null, null, null, null, null, null, null, null ,mutableMapOf())
        isEditing = true
        player.sendMessage("<green>Ora stai editando".toMini())
    }

    @Subcommand("save")
    fun saveArena(player: Player) {
        if (!checkIfEditing(player)) return

        val arenaEdit = editingPlayers[player] ?: return
        val name = arenaEdit.name
        val locations = arenaEdit.locations

        if (name.isNullOrEmpty()) {
            player.sendMessage("<red>Devi impostare un nome per l'arena!".toMini())
            return
        }
        if (locations.isEmpty()) {
            player.sendMessage("<red>Devi impostare almeno uno spawn!".toMini())
            return
        }
        if(arenaEdit.minPlayers == null){
            player.sendMessage("<red>Devi impostare il min-player!".toMini())
            return
        }
        if(arenaEdit.maxHeight == null){
            player.sendMessage("<red>Devi impostare il max-height!".toMini())
        }
        if(arenaEdit.minHeight == null){
            player.sendMessage("<red>Devi impostare il min-height!".toMini())
        }
        if(arenaEdit.deathSpawn == null){
            player.sendMessage("<red>Devi impostare il dath spawn!".toMini())
        }
        if(arenaEdit.regionLocationOne == null){
            player.sendMessage("<red>Devi impostare la prima region!".toMini())
        }
        if(arenaEdit.regionLocationSecond == null){
            player.sendMessage("<red>Devi impostare la seconda region!".toMini())
        }

        val config = GourPillars.instance.config
        val arenaPath = "Arenas.$name"

        val world = locations.values.first().world
        val worldName = world.name

        world.let{
            it.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
            it.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
            it.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        }

        config.set("$arenaPath.world", worldName)
        config.set("$arenaPath.min-players", arenaEdit.minPlayers)
        config.set("$arenaPath.max-height", arenaEdit.maxHeight)
        config.set("$arenaPath.min-height", arenaEdit.minHeight)
        config.set("$arenaPath.slow-falling-time", arenaEdit.slowFallingTime)

        config.set("$arenaPath.main-spawn.x", arenaEdit.deathSpawn?.x)
        config.set("$arenaPath.main-spawn.y", arenaEdit.deathSpawn?.y)
        config.set("$arenaPath.main-spawn.z", arenaEdit.deathSpawn?.z)
        config.set("$arenaPath.main-spawn.yaw", arenaEdit.deathSpawn?.yaw)
        config.set("$arenaPath.main-spawn.pitch", arenaEdit.deathSpawn?.pitch)

        config.set("$arenaPath.region.loc-1.x", arenaEdit.regionLocationOne?.x)
        config.set("$arenaPath.region.loc-1.y", arenaEdit.regionLocationOne?.y)
        config.set("$arenaPath.region.loc-1.z", arenaEdit.regionLocationOne?.z)

        config.set("$arenaPath.region.loc-2.x", arenaEdit.regionLocationSecond?.x)
        config.set("$arenaPath.region.loc-2.y", arenaEdit.regionLocationSecond?.y)
        config.set("$arenaPath.region.loc-2.z", arenaEdit.regionLocationSecond?.z)

        locations.forEach { (index, location) ->
            config.set("$arenaPath.spawns.$index.x", location.x)
            config.set("$arenaPath.spawns.$index.y", location.y)
            config.set("$arenaPath.spawns.$index.z", location.z)
            config.set("$arenaPath.spawns.$index.yaw", location.yaw)
            config.set("$arenaPath.spawns.$index.pitch", location.pitch)
        }

        GourPillars.instance.saveConfig()
        player.sendMessage("<green>Arena '$name' salvata con successo!".toMini())
        editingPlayers.remove(player)
        zipManager.saveBackup(worldName)
    }

    @Subcommand("setRegionOne")
    fun setRegionOne(player: Player){
        editingPlayers[player]?.regionLocationOne = player.location
        player.sendMessage("".toMini())
    }

    @Subcommand("setRegionTwo")
    fun setRegionTwo(player: Player){
        editingPlayers[player]?.regionLocationSecond = player.location
        player.sendMessage("".toMini())
    }

    @Subcommand("setMaxHeight")
    fun setMaxHeight(player: Player){
        editingPlayers[player]?.maxHeight = player.location.y.toInt()
        player.sendMessage("<green>Altezza impostata a ${player.location.y.toInt()}".toMini())
    }

    @Subcommand("setDeathSpawn")
    fun setDeathSpawn(player: Player){
        editingPlayers[player]?.deathSpawn = player.location
        player.sendMessage("<green>Spawn dei morti impostato!".toMini())
    }

    @Subcommand("setFallingTime <number>")
    fun setFallingTime(player: Player, number: Int){
        editingPlayers[player]?.slowFallingTime = number
        player.sendMessage("<green>Altezza impostata number".toMini())
    }

    @Subcommand("setMinHeight")
    fun setMinHeight(player: Player){
        editingPlayers[player]?.minHeight = player.location.y.toInt()
        player.sendMessage("<green>Altezza impostata a ${player.location.y.toInt()}".toMini())
    }

    @Subcommand("stop")
    fun stopEditing(player: Player){
        if(!checkIfEditing(player)) return

        editingPlayers.remove(player)
        isEditing = false
        player.sendMessage("<green>Non stai piu' editando".toMini())
    }

    @Subcommand("name <name>")
    fun setName(player: Player, name: String){
        if(!checkIfEditing(player)) return

        editingPlayers[player]?.name = name
        player.sendMessage("<green>Nome impostato a $name".toMini())
    }
    @Subcommand("minplayers <min>")
    fun setMinPlayers(player: Player, min: Int){
        if(!checkIfEditing(player)) return

        editingPlayers[player]?.minPlayers = min
        player.sendMessage("<green>Player minimi impostati a $min".toMini())
    }

    @Subcommand("spawn <number>")
    fun setSpawn(player: Player, number: Int){
        if(!checkIfEditing(player)) return

        val spawnLocation = player.location
        val locationsEditor = editingPlayers[player]?.locations

        if (locationsEditor != null) {
            for((index, spawn) in locationsEditor){
                if(spawn.world != spawnLocation.world){
                    player.sendMessage("<red>Il mondo e' diverso dagli altri spawn, operazione non valida".toMini())
                    return
                }
                if(spawn.world == Bukkit.getWorld("world")){
                    player.sendMessage("<red>Non creare una arena nel mondo default".toMini())
                }
            }
        }

        locationsEditor?.set(number, spawnLocation)
        player.sendMessage("<green>$number aggiunto con successo".toMini())
    }

    @Subcommand("check")
    fun checkArena(player: Player){
        if(!checkIfEditing(player)) return

        val editorName = player.name
        val name = editingPlayers[player]?.name ?: "Non settato"
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

    private fun checkIfEditing(player: Player): Boolean{
        if(!editingPlayers.contains(player)){
            player.sendMessage("<red>Non stai editando".toMini())
            return false
        }
        return true
    }

}