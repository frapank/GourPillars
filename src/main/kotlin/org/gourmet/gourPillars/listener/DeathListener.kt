package org.gourmet.gourPillars.listener

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.managers.ArenaManager
import org.gourmet.gourPillars.task.GameTask
import org.gourmet.gourPillars.managers.arena.State

class DeathListener : Listener {

    private val arenaManager: ArenaManager = GourPillars.arenaManager

    @EventHandler
    fun onDeath(event: PlayerDeathEvent){
        Bukkit.getLogger().info("${event.deathMessage()}")

        val player: Player = event.player
        val arena: Arena = arenaManager.getArenaByPlayer(player) ?: run {
            GourPillars.spawnManager.teleportPlayerToSpawn(player)
            return
        }
        val gameRunnable: GameTask = arena.gameTask
        if(arena.gameState != State.INGAME) return
        if(player.killer != null && player.killer is Player ){
            gameRunnable.playerEliminated(player, player.killer!!)
        } else {
            gameRunnable.playerEliminated(player)
        }
    }
}