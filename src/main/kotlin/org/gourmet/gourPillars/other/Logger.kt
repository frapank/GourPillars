package org.gourmet.gourPillars.other

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit

object Logger {
    private val prefix: String = "&d[DreamKitPvP]: ";

    fun info(text: String?) {
        sendText("&f" + text)
    }

    fun warning(text: String?) {
        sendText("&c" + text)
    }

    fun watch(text: String?) {
        sendText("&k" + text)
    }

    private fun sendText(text: String?) {
        Bukkit.getConsoleSender().sendMessage((prefix + text).toMini())
    }
}
