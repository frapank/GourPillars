package org.gourmet.gourPillars.other

import org.bukkit.Bukkit

object Logger {
    private const val PREFIX: String = "<light_purple>[GourPillars]</light_purple> "

    fun info(text: String?) {
        sendText("<white>$text</white>")
    }

    fun warning(text: String?) {
        sendText("<red>$text</red>")
    }

    fun watch(text: String?) {
        sendText("<obfuscated>$text</obfuscated>")
    }

    private fun sendText(text: String?) {
        Bukkit.getConsoleSender().sendMessage((PREFIX + text).toMini())
    }
}
