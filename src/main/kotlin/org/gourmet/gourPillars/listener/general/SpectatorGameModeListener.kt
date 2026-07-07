package org.gourmet.gourPillars.listener.general

import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.gourmet.gourPillars.GourPillars

class SpectatorGameModeListener : Listener {
    private val arenaManager = GourPillars.arenaManager

    // Blocks any change away from SPECTATOR for a tracked spectator (Multiverse's per-world
    // gamemode, /gamemode, F3+N). Arena.removeSpectator untracks first, so /leave still works.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onGameModeChange(event: PlayerGameModeChangeEvent) {
        if (event.newGameMode == GameMode.SPECTATOR) return
        if (arenaManager.isSpectating(event.player)) {
            event.isCancelled = true
        }
    }
}
