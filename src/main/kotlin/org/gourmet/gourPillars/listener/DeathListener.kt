package org.gourmet.gourPillars.listener

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.task.game.gametasks.GameTask
import org.gourmet.gourPillars.managers.arena.State

@SuppressWarnings("deprecation")
class DeathListener : Listener {

    private val arenaManager = GourPillars.arenaManager

    @EventHandler
    fun onDeath(event: PlayerDeathEvent){

        val player: Player = event.player

        event.deathMessage = null
        instantRespawn(player)

        val arena: Arena = arenaManager.getArenaByPlayer(player) ?: run {
            GourPillars.spawnManager.teleportPlayerToSpawn(player)
            return
        }

        val gameRunnable: GameTask = arena.gameTask

        if(arena.gameState != State.INGAME) return
        if(player.killer != null && player.killer is Player ){
            gameRunnable.playerEliminated(player, player.killer!!)
        } else if(event.entity.lastDamageCause?.cause == EntityDamageEvent.DamageCause.FALL) {
            gameRunnable.playerEliminatedFall(player)
        } else {
            gameRunnable.playerEliminated(player)
        }

    }

    private fun instantRespawn(player: Player) {

        Bukkit.getScheduler().runTaskLater(GourPillars.instance, Runnable {
            player.spigot().respawn()
        }, 1L)

    }

}