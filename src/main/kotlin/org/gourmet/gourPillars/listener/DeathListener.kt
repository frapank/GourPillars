package org.gourmet.gourPillars.listener

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.data.PlayerData
import org.gourmet.gourPillars.managers.arena.Arena
import org.gourmet.gourPillars.task.game.gametasks.GameTask
import org.gourmet.gourPillars.managers.arena.State

@SuppressWarnings("deprecation")
class DeathListener : Listener {

    private val arenaManager = GourPillars.arenaManager
    private val jsonManager = GourPillars.jsonManager

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
            updatePlayerData(player, player.killer!!)
        } else if(event.entity.lastDamageCause?.cause == EntityDamageEvent.DamageCause.FALL) {
            gameRunnable.playerEliminatedFall(player)
            updatePlayerData(player)
        } else {
            gameRunnable.playerEliminated(player)
            updatePlayerData(player)
        }

    }

    private fun instantRespawn(player: Player) {

        Bukkit.getScheduler().runTaskLater(GourPillars.instance, Runnable {
            player.spigot().respawn()
        }, 1L)

    }

    private fun updatePlayerData(death: Player, killer: Player) {

        val deathData = jsonManager.getPlayerData(death) ?: PlayerData(death.name, 0, 0, 0, 0, 0, 0, 0)
        val killerData = jsonManager.getPlayerData(killer) ?: PlayerData(killer.name, 0, 0, 0, 0, 0, 0, 0)

        deathData.deaths += 1
        deathData.gamesPlayed += 1
        killerData.kills += 1

        jsonManager.setPlayerData(killer, killerData)
        jsonManager.setPlayerData(death, deathData)
        jsonManager.addXP(killer, 100)
        jsonManager.savePlayerData()

    }

    private fun updatePlayerData(player: Player) {

        val playerData = jsonManager.getPlayerData(player) ?: PlayerData(player.name, 0, 0, 0, 0, 0, 0, 0)

        playerData.deaths += 1
        playerData.gamesPlayed += 1

        jsonManager.setPlayerData(player, playerData)
        jsonManager.savePlayerData()

    }

}