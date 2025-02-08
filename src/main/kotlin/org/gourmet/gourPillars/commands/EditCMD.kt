package org.gourmet.gourPillars.commands

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.ZipManager
import org.gourmet.gourPillars.managers.arena.toMini
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission

data class ArenaEdit(val editor: Player, var name: String?, var minPlayers: Int?, var locations: MutableMap<Int, Location>)

@Command("edit")
@CommandPermission("gpillars.admim")
object EditCMD {

    private val arenaManager = GourPillars.arenaManager
    val editingPlayers: MutableMap<Player, ArenaEdit> = mutableMapOf()
    private val zipManager = ZipManager()
    private var isEditing = GourPillars.isEditing

    @Subcommand("start")
    fun startEditing(player: Player){
        if(editingPlayers.contains(player)){
            player.sendMessage("<red>Stai gia editando".toMini())
            return
        }

        editingPlayers[player] = ArenaEdit(player, null, null, mutableMapOf())
        isEditing = true
        player.sendMessage("<green>Ora sti editando".toMini())
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

        val config = GourPillars.instance.config
        val arenaPath = "Arenas.$name"

        val worldName = locations.values.first().world.name
        config.set("$arenaPath.world", worldName)
        config.set("$arenaPath.min-players", arenaEdit.minPlayers)

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


    @Subcommand("stop")
    fun stopEditing(player: Player){
        if(!checkIfEditing(player)) return

        editingPlayers.remove(player)
        isEditing = false
    }

    @Subcommand("name <name>")
    fun setSpawn(player: Player, name: String){
        if(!checkIfEditing(player)) return

        editingPlayers[player]?.name = name
    }
    @Subcommand("minplayers <min>")
    fun setMinPlayers(player: Player, min: Int){
        if(!checkIfEditing(player)) return

        editingPlayers[player]?.minPlayers = min
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
        player.sendMessage("$number aggiunto con successo")
    }

    @Subcommand("check")
    fun checkArena(player: Player){
        if(!checkIfEditing(player)) return

        val editorName = player.name
        val name = editingPlayers[player]?.name ?: "Non settato"
        val locations = editingPlayers[player]?.locations
        player.sendMessage("<yellow>-----------------------------")
        player.sendMessage("<yellow>Editor <green>-> $editorName".toMini())
        player.sendMessage("<yellow>Name <green>-> $name".toMini())
        player.sendMessage("Arenas:".toMini())
        locations?.forEach { (index, location) ->
            player.sendMessage("<yellow>$index <green>-> ${location.world.name}, ${location.x.toInt()}, ${location.y.toInt()}, ${location.z.toInt()}".toMini())
        }
        player.sendMessage("<yellow>-----------------------------")
    }

    private fun checkIfEditing(player: Player): Boolean{
        if(!editingPlayers.contains(player)){
            player.sendMessage("<red>Non stai editando".toMini())
            return false
        }
        return true
    }

}