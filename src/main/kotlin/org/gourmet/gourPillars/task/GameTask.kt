package org.gourmet.gourPillars.task

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.Utils
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.arena.State
import java.util.*

class GameTask(private val arena: Arena, private val plugin: JavaPlugin): BukkitRunnable(){

    lateinit var alivePlayer: MutableSet<Player>
    private var running = false

    override fun run(){
        running = true
        alivePlayer = arena.waitingPlayer.toMutableSet()
        removeAllGlass()
        preparePlayer()
        startRandomItemTask()
        object : BukkitRunnable() {
            override fun run() {
                if (!running) {
                    cancel()
                    return
                }

                if (alivePlayer.size <= 1) {
                    val lastPlayer = alivePlayer.first().name
                    arena.sendTitleToPlayerInGame("&aGioco terminato!", "&e${lastPlayer} &fha vinto")
                    object : BukkitRunnable(){
                        override fun run(){
                            running = false
                            arena.gameState = State.STOPPED
                            arena.waitingPlayer.forEach { player: Player ->
                                GourPillars.spawnManager.teleportPlayerToSpawn(player)
                                player.inventory.clear()
                            }
                            arena.spawnMap.forEach{ (location, player) ->
                                arena.spawnMap[location] = null
                            }
                            arena.waitingPlayer.clear()
                            alivePlayer.clear()

                            cancel()
                            arena.resetArenaTask.run()
                            GourPillars.instance.logger.info("Iniziato il reset")
                        }
                    }.runTaskLater(plugin, 50L)
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }

    private fun removeAllGlass(){
        arena.spawnMap.forEach{ (location, _) ->
            Utils.setGlass(false, location)
        }
    }

    private fun preparePlayer(){
        alivePlayer.forEach{player ->
            player.inventory.clear()
            player.gameMode = GameMode.SURVIVAL
            player.health = 20.0
        }
    }

    private fun startRandomItemTask() {
        object : BukkitRunnable() {
            override fun run() {
                if (!running) {
                    cancel()
                    return
                }

                alivePlayer.forEach { player ->
                    giveRandomItem(player)
                }
            }
        }.runTaskTimer(GourPillars.instance, 0L, 70L)
    }

    private fun giveRandomItem(player: Player) {
        val randomMaterial = getRandomMaterial()
        val itemStack = ItemStack(randomMaterial)

        player.inventory.addItem(itemStack)

    }

    private fun getRandomMaterial(): Material {
        val materials = Material.values().filter { it.isItem || it.isBlock }
        return materials[Random().nextInt(materials.size)]
    }

    fun playerEliminated(player: Player){
        alivePlayer.remove(player)
        player.gameMode = GameMode.SPECTATOR
        alivePlayer.forEach { playerMessage ->
            playerMessage.sendMessage("${player.name} e' stato eliminato")
        }

    }


}