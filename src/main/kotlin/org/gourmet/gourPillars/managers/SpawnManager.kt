package org.gourmet.gourPillars.managers

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars

class SpawnManager {
    companion object {
        var spawn: Location? = null
    }

    private val config = GourPillars.instance.config

    init {
        spawn = loadSpawn()
    }

    fun teleportPlayerToSpawn(player: Player) {
        if (spawn != null) {
            player.teleport(spawn!!)
            player.gameMode = GameMode.SURVIVAL
        } else {
            player.sendMessage("[Error] Spawn not set")
        }
    }

    private fun loadSpawn(): Location? {
        val world: World = Bukkit.getWorld(config.getString("spawn.world").toString()) ?: return null
        val x: Double = config.getDouble("spawn.x")
        val y: Double = config.getDouble("spawn.y")
        val z: Double = config.getDouble("spawn.z")
        val yaw: Float = config.getDouble("spawn.yaw").toFloat()
        val pitch: Float = config.getDouble("spawn.pitch").toFloat()

        return Location(world, x, y, z, yaw, pitch)
    }

    fun getConfiguredWorld(): World? {
        val worldName = config.getString("spawn.world") ?: return null
        return Bukkit.getWorld(worldName)
    }
}
