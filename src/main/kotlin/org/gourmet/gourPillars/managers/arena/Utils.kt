package org.gourmet.gourPillars.managers.arena

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.gourmet.gourPillars.GourPillars

object Utils {

    val config = GourPillars.instance.config

    fun setGlass(put: Boolean, location: Location) {
        val world: World = location.world ?: return
        val x = location.blockX
        val y = location.blockY - 1
        val z = location.blockZ
        val material = if (put) Material.GLASS else Material.AIR

        for (dx in -1..1) {
            for (dy in 0..3) {
                for (dz in -1..1) {
                    val block: Block = world.getBlockAt(x + dx, y + dy, z + dz)

                    if (dy == 0 || dy == 3) {
                        block.type = material
                    } else if (dx == -1 || dx == 1 || dz == -1 || dz == 1) {
                        block.type = material
                    }
                }
            }
        }
    }
}
val miniMessage = MiniMessage.builder().build()
fun String.toMini(): Component = miniMessage.deserialize(this)
