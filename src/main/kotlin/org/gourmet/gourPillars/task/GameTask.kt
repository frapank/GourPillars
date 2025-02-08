package org.gourmet.gourPillars.task

import org.bukkit.*
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.arena.State
import org.gourmet.gourPillars.managers.arena.Utils
import org.gourmet.gourPillars.managers.arena.toMini
import org.jetbrains.annotations.ApiStatus
import java.lang.reflect.Field
import java.util.*


class GameTask(private val arena: Arena, private val plugin: JavaPlugin): BukkitRunnable(){

    lateinit var alivePlayer: MutableMap<Player, Int>
    private var running = false
    private var secondsPassed = 300

    override fun run(){
        running = true
        alivePlayer = mutableMapOf()
        arena.waitingPlayer.forEach { player ->
            alivePlayer[player] = 0
        }
        removeAllGlass()
        preparePlayer()
        startRandomItemTask()
        object : BukkitRunnable() {
            override fun run() {
                secondsPassed--
                if (!running) {
                    cancel()
                    return
                }
                alivePlayer.forEach{(player, _) ->
                    arena.scoreboardManager.setGameScoreboard(player)
                }
                if (alivePlayer.size <= 1 || secondsPassed == 0) {
                    val winner = when(alivePlayer.size){
                        1 -> alivePlayer.keys.first()
                        else -> alivePlayer.maxByOrNull { it.value }?.key
                    }
                    arena.sendTitleToPlayerInGame("&aGioco terminato!", "&e${winner?.name} &fha vinto")
                    if (winner != null) {
                        playVictoryEffects(winner)
                    }
                    object : BukkitRunnable(){
                        override fun run(){
                            running = false
                            secondsPassed = 300
                            arena.gameState = State.STOPPED
                            arena.waitingPlayer.forEach { player ->
                                GourPillars.spawnManager.teleportPlayerToSpawn(player)
                                GourPillars.lobbyScoreboardManager.setScoreboard(player)
                                player.inventory.clear()
                                player.health = 20.0
                                player.foodLevel = 20
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
                    }.runTaskLater(plugin, 80L)
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
        alivePlayer.forEach{(player, _) ->
            player.inventory.clear()
            player.gameMode = GameMode.SURVIVAL
            player.health = 20.0
            arena.scoreboardManager.setGameScoreboard(player)
        }
    }

    private fun startRandomItemTask() {
        object : BukkitRunnable() {
            override fun run() {
                if (!running) {
                    cancel()
                    return
                }

                alivePlayer.forEach { (player, _ )->
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
        Material.COPPER_TRAPDOOR
        val materials = Material.values()
            .filter { it.isItem && it.isBlock }
            .filter { m: Material ->
                !m.isEmpty && !m.isLegacy && m.isItem && m.isEnabledByFeature(Bukkit.getWorld("world")!!)
            }

        return materials.random()
    }

    fun playerEliminated(player: Player){
        alivePlayer.remove(player)
        player.gameMode = GameMode.SPECTATOR
        alivePlayer.forEach { (playerMessage, _) ->
            playerMessage.sendMessage("<yellow><green>${player.name}</green> e' stato eliminato</yellow>".toMini())
        }
        arena.reloadInGameScoreboard()

    }

    fun playerEliminated(player: Player, killer: Player){
        alivePlayer.remove(player)
        player.gameMode = GameMode.SPECTATOR
        alivePlayer.forEach { (playerMessage, _) ->
            playerMessage.sendMessage("<yellow><green>${player.name}</green> e' stato eliminato da <green>${killer.name}</green></yellow>".toMini())
        }
        if(alivePlayer.contains(killer)){
            val oldKills = alivePlayer[killer]!! + 1
            alivePlayer[killer] = oldKills
        }
        arena.reloadInGameScoreboard()

    }


    fun getTimeFormatted(): String{
        val minutes = secondsPassed / 60
        val remainingSeconds = secondsPassed % 60

        val minuteText = if (minutes == 1) "1 minuto" else "$minutes minuti"
        val secondText = if (remainingSeconds == 1) "1 secondo" else "$remainingSeconds secondi"

        return if (minutes > 0) "$minuteText $secondText" else secondText
    }

    private fun playVictoryEffects(winner: Player) {
        val world = winner.world

        arena.waitingPlayer.forEach { player ->
            player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        }

        object : BukkitRunnable() {
            var count = 0
            override fun run() {
                if (count >= 3) {
                    cancel()
                    return
                }
                val firework = world.spawn(winner.location, Firework::class.java)
                val meta = firework.fireworkMeta
                meta.addEffect(
                    FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.RED, Color.BLUE, Color.YELLOW)
                        .withFlicker()
                        .build()
                )
                meta.power = 1
                firework.fireworkMeta = meta
                count++
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }
}


