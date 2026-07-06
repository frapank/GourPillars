package org.gourmet.gourPillars.task

import org.bukkit.Sound
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.messages.MessageData

class CountDownTask(val arena: Arena) : BukkitRunnable() {

    private val countdownSeconds = GourPillars.instance.config.getInt("game.countdown-seconds", 10)
    private var counter = countdownSeconds

    override fun run() {

                //Cancel if player is not enought
                if (arena.inGamePlayer.size < arena.minPlayer) {
                    arena.gameState = State.WAITING
                    counter = countdownSeconds
                    cancel()
                    return
                }

                //End countdown
                if (counter <= 0) {
                    //Messages and Effects
                    arena.sendTitleToPlayerInGame("&7Uccidi i tuoi avversari", "&8Ma non cadere...")
                    arena.sendDynamicTitleToPlayerInGame(MessageData.ARENA_TITLE_START, MessageData.ARENA_SUBTITLE_START)
                    arena.inGamePlayer.forEach { player ->
                        player.playSound(player.location, Sound.ENTITY_WITHER_SPAWN, 0.8f, 2.0f)
                    }

                    //Arena update
                    arena.gameState = State.INGAME
                    arena.gameTask.run()
                    counter = countdownSeconds
                    cancel()
                    return
                }

                //Increse countdown pitch
                val volume = 0.2f + (counter * 0.2f)
                val pitch = 2.0f + (counter * 0.1f)

                //Playe countdown sounmd
                arena.inGamePlayer.forEach { player ->
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch)
                }

                //Send countdown with color
                val countPrefix: String = when (counter) {
                    1,2 -> "&c"
                    3,4,5 -> "&e"
                    else -> "&a"
                }
                arena.sendTitleToPlayerInGame("$countPrefix $counter", "")
        counter--
    }
}