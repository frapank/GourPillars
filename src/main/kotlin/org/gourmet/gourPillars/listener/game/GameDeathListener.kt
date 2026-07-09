package org.gourmet.gourPillars.listener.game

import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.task.game.gametasks.GameTask
import java.util.*

@SuppressWarnings("deprecation")
class GameDeathListener : Listener {
    private val arenaManager = GourPillars.arenaManager

    // The void death case is handled in VoidKillListener.kt
    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val player: Player = event.player

        event.deathMessage = null
        instantRespawn(player)

        val arena: Arena =
            arenaManager.getArenaByPlayer(player) ?: run {
                GourPillars.spawnManager.teleportPlayerToSpawn(player)
                return
            }

        val gameRunnable: GameTask = arena.gameTask

        if (arena.gameState != State.INGAME) return

        val lastDamageCause = event.entity.lastDamageCause

        if (player.killer != null && player.killer is Player) {
            // direct kill
            gameRunnable.playerEliminated(player, player.killer!!)
        } else if (lastDamageCause?.cause == EntityDamageEvent.DamageCause.FALL) {
            // fall damage
            gameRunnable.playerEliminatedFall(player)
        } else if (lastDamageCause is EntityDamageByEntityEvent && lastDamageCause.damager !is Player) {
            gameRunnable.playerEliminatedByMob(player, lastDamageCause.damager)
        } else {
            gameRunnable.playerEliminated(player)
        }
    }

    // get damager and victim in map
    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        if (victim !is Player) {
            return
        }

        val arena: Arena =
            arenaManager.getArenaByPlayer(victim) ?: run {
                return
            }

        val damager = event.damager
        if (damager is Player) {
            arena.lastDamagerMap[victim.uniqueId] = damager.uniqueId
        }
    }

    private fun instantRespawn(player: Player) {
        Bukkit.getScheduler().runTaskLater(
            GourPillars.instance,
            Runnable {
                player.spigot().respawn()
            },
            1L,
        )
    }
}
