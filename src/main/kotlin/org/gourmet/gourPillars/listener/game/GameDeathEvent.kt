package org.gourmet.gourPillars.listener.game

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.task.game.gametasks.GameTask

@SuppressWarnings("deprecation")
class GameDeathEvent : Listener {

    private val arenaManager = GourPillars.Companion.arenaManager

    @EventHandler
    fun onDeath(event: PlayerDeathEvent){

        val player: Player = event.player

        event.deathMessage = null
        instantRespawn(player)

        val arena: Arena = arenaManager.getArenaByPlayer(player) ?: run {
            GourPillars.Companion.spawnManager.teleportPlayerToSpawn(player)
            return
        }

        val gameRunnable: GameTask = arena.gameTask

        if(arena.gameState != State.INGAME) return
        if(player.killer != null && player.killer is Player){
            gameRunnable.playerEliminated(player, player.killer!!)
        } else if(event.entity.lastDamageCause?.cause == EntityDamageEvent.DamageCause.FALL) {
            gameRunnable.playerEliminatedFall(player)
        } else {
            gameRunnable.playerEliminated(player)
        }

    }

    private fun instantRespawn(player: Player) {

        Bukkit.getScheduler().runTaskLater(GourPillars.Companion.instance, Runnable {
            player.spigot().respawn()
        }, 1L)

    }

}