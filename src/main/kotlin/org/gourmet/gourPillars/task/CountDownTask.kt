package org.gourmet.gourPillars.task

import org.bukkit.Sound
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.arena.State
import org.gourmet.gourPillars.other.messages.MessageData

class CountDownTask(val arena: Arena) : BukkitRunnable(){

    private var counter = 10

    override fun run() {

                if(arena.waitingPlayer.size < arena.minPlayer){
                    arena.gameState = State.WAITING
                    counter = 10
                    cancel()
                    return
                }
                if(counter <= 0){
                    //arena.sendTitleToPlayerInGame("&7Uccidi i tuoi avversari", "&8Ma non cadere...")
                    arena.sendDynamicTitleToPlayerInGame(MessageData.ARENA_TITLE_START, MessageData.ARENA_SUBTITLE_START)
                    arena.waitingPlayer.forEach { player ->
                        player.playSound(player.location, Sound.ENTITY_WITHER_SPAWN, 0.8f, 2.0f)
                    }
                    arena.gameState = State.INGAME
                    arena.gameTask.run()
                    counter = 10
                    cancel()
                    return
                }

                val volume = 0.2f + (counter * 0.2f)
                val pitch = 2.0f + (counter * 0.1f)

                arena.waitingPlayer.forEach { player ->
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch)
                }

                val countPrefix: String = when(counter){
                    1,2 -> "&c"
                    3,4,5 -> "&e"
                    else -> "&a"
                }
                arena.sendTitleToPlayerInGame("$countPrefix $counter", "")
        counter--
    }
}