package org.gourmet.gourPillars.managers

import org.bukkit.Sound
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Utils
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicTitle

object LevelManager {
    private val config get() = GourPillars.instance.config

    val enabled get() = config.getBoolean("level.enabled", true)

    fun xpFor(source: String): Int = config.getInt("level.xp-rewards.$source", 0)

    fun levelForXp(xp: Int): Int {
        val xpPerLevel = config.getInt("level.xp-per-level", 100).coerceAtLeast(1)
        return 1 + xp / xpPerLevel
    }

    fun announceLevelUp(
        player: Player,
        level: Int,
    ) {
        val soundPath = "level.level-up.sound"
        val sound = Utils.readSound(config.getString(soundPath), Sound.ENTITY_PLAYER_LEVELUP, soundPath)
        val volume = config.getDouble("level.level-up.sound-volume", 1.0).toFloat()
        val pitch = config.getDouble("level.level-up.sound-pitch", 1.0).toFloat()

        player.sendDynamicTitle(MessageData.LEVEL_UP_TITLE, MessageData.LEVEL_UP_SUBTITLE, "{level}" to level.toString())
        player.playSound(player.location, sound, volume, pitch)
    }
}
