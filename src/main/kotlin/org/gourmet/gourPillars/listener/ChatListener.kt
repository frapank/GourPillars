package org.gourmet.gourPillars.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener : Listener{
    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {

        val sender = event.player
        val world = sender.world

        event.recipients.removeIf { it.world != world }

    }
}