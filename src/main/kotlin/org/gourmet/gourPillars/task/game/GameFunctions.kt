package org.gourmet.gourPillars.task.game

import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Sound
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena

object GameFunctions {

    fun playVictoryEffects(winner: Player, arena: Arena) {
        val world = winner.world

        arena.waitingPlayer.forEach { player ->
            player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        }

        if(arena.containPlayer(winner)) {
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
            }.runTaskTimer(GourPillars.instance, 0L, 20L)
        }
    }
}
