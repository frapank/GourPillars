package org.gourmet.gourPillars.commands

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.ZipManager
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand

@Command("test")
object TestCMD {

    private val zipManager = ZipManager()
    private val arenaManager = GourPillars.arenaManager

    @Subcommand("backup")
    fun backupCommand(player: Player){
        zipManager.saveBackup("arena")
        player.sendMessage("fatto")
    }

    @Subcommand("restore")
    fun restoreCommand(player: Player){
        zipManager.restoreBackup("arena")
        Bukkit.getLogger().warning("-- COMINCIATO -- ${arenaManager.onlineArenas.toString()}")
        object : BukkitRunnable(){
            override fun run(){

                arenaManager.onlineArenas.forEach{ (name, arena) ->
                    if(name == "brutta"){
                        arena.spawnMap.forEach{(location, boolean) ->
                            location.world = Bukkit.getWorld("arena")
                            Bukkit.getLogger().warning("   > fatto")
                        }
                        Bukkit.getLogger().warning("   > Trovata")
                    } else {
                        Bukkit.getLogger().warning(" --> !! Niente $name")
                    }
                }
            }
        }.runTaskLater(GourPillars.instance, 100L)

        player.sendMessage("fatto")
    }


}