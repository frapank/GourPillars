package org.gourmet.gourPillars.listener.general

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatViewEvent : Listener {

    /*
        This will show the messages only
        if the player is in the same world
     */
    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val sender = event.player
        val world = sender.world
        event.recipients.removeIf { it.world != world }
    }
}