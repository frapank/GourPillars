package org.gourmet.gourPillars.task

import org.bukkit.Sound
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.arena.State

class CountDownTask(val arena: Arena) : BukkitRunnable(){

    private var counter = 0

    override fun run() {

                if(arena.waitingPlayer.size < arena.minPlayer){
                    arena.sendMessageToPlayerInGame("<red>E' uscito un player, mancano ${arena.minPlayer - arena.waitingPlayer.size} player")
                    arena.gameState = State.WAITING
                    counter = 0
                    cancel()
                    return
                }
                if(counter >= 5){
                    arena.sendTitleToPlayerInGame("&aPartita cominciata!", "")
                    arena.gameState = State.INGAME
                    arena.gameTask.run()
                    counter = 0
                    cancel()
                    return
                }

                val volume = 0.2f + (counter * 0.2f)
                val pitch = 2.0f + (counter * 0.1f)

                arena.waitingPlayer.forEach { player ->
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch)
                }

                counter++
                val countPrefix: String = when(counter){
                    1,2 -> "&a"
                    3 -> "&e"
                    else -> "&c"
                }
                arena.sendTitleToPlayerInGame("$countPrefix $counter", "")
    }
}