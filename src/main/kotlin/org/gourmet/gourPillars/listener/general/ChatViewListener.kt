package org.gourmet.gourPillars.listener.general

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.miniMessage

class ChatViewListener : Listener {
    private val arenaManager = GourPillars.arenaManager

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val sender = event.player
        val spectatingArena = arenaManager.getArenaBySpectator(sender)

        if (spectatingArena != null) {
            event.isCancelled = true
            event.recipients.clear() // in case another plugin reads this event with ignoreCancelled = true
            spectatingArena.sendDynamicMessageToSpectators(
                MessageData.SPECTATE_CHAT_FORMAT,
                "{player}" to sender.name,
                "{message}" to miniMessage.escapeTags(event.message),
            )
            return
        }

        val world = sender.world
        event.recipients.removeIf { it.world != world || arenaManager.isSpectating(it) }
    }
}
