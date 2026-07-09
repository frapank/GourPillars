package org.gourmet.gourPillars.managers

import org.gourmet.gourPillars.GourPillars

object LobbyConfig {
    private val config get() = GourPillars.instance.config

    val enabled get() = config.getBoolean("lobby.enabled", true)

    private fun check(name: String): Boolean = enabled && config.getBoolean("lobby.checks.$name", true)

    val teleportToSpawnOnJoin get() = check("teleport-to-spawn-on-join")
    val resetStateOnJoin get() = check("reset-state-on-join")
    val lobbyItems get() = check("lobby-items")
    val scoreboard get() = check("scoreboard")
    val unlimitedFood get() = check("unlimited-food")
    val worldProtection get() = check("world-protection")
    val damageProtection get() = check("damage-protection")
    val itemProtection get() = check("item-protection")
    val voidTeleportToSpawn get() = check("void-teleport-to-spawn")
}
