package org.gourmet.gourPillars.listener.game

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.LobbyConfig
import org.gourmet.gourPillars.managers.game.arena.State

class VoidKillListener : Listener {
    val arenaManager = GourPillars.arenaManager

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val playerLoc = player.y
        val arena = arenaManager.getArenaByPlayer(player)

        if (arena != null && arena.gameState == State.INGAME) {
            if (playerLoc <= arena.minHeight) {
                val damagerId = arena.lastDamagerMap.remove(player.uniqueId)
                val damagerPlayer = damagerId?.let { Bukkit.getPlayer(it) }
                if (!arena.gameTask.alivePlayer.contains(player)) return
                if (damagerPlayer != null) {
                    arena.gameTask.playerEliminatedVoid(player, damagerPlayer)
                } else {
                    arena.gameTask.playerEliminatedVoid(player)
                }
            }
            return
        }

        // Still queued: put them back in their cage instead of the lobby spawn.
        if (arena != null && (arena.gameState == State.WAITING || arena.gameState == State.STARTING)) {
            if (playerLoc <= arena.minHeight) {
                val cageSpawn =
                    arena.spawnMap.entries
                        .firstOrNull { it.value == player }
                        ?.key
                player.teleport(cageSpawn?.let { arena.cageLocation(it) } ?: arena.spawnMainLocation)
            }
            return
        }

        if (!LobbyConfig.voidTeleportToSpawn) return
        val minHeight = arena?.minHeight ?: player.world.minHeight
        if (playerLoc <= minHeight) {
            GourPillars.spawnManager.teleportPlayerToSpawn(player)
        }
    }
}
