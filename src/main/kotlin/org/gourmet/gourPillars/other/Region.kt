package org.gourmet.gourPillars.other

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World

class Region private constructor(
    var world: World,
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int
) {
    companion object {

        fun createRegion(loc1: Location, loc2: Location): Region {
            if (loc1.world != loc2.world) throw IllegalArgumentException("Le location devono essere nello stesso mondo")

            val world = loc1.world!!

            val (minX, maxX) = listOf(loc1.blockX, loc2.blockX).sorted()
            val (minY, maxY) = listOf(loc1.blockY, loc2.blockY).sorted()
            val (minZ, maxZ) = listOf(loc1.blockZ, loc2.blockZ).sorted()

            return Region(world, minX, minY, minZ, maxX, maxY, maxZ)
        }

    }

    fun isInRegion(loc: Location): Boolean {
        return loc.world == world &&
                loc.x >= minX && loc.x <= maxX &&
                loc.y >= minY && loc.y <= maxY &&
                loc.z >= minZ && loc.z <= maxZ
    }

    fun replaceYLevelWithLava(targetY: Int) {
        if (targetY < world.minHeight || targetY > world.maxHeight) return

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val block = world.getBlockAt(x, targetY, z)
                block.type = Material.LAVA
            }
        }
    }

    fun getBounds(): String {
        return "Mondo: ${world.name} | X: $minX-$maxX | Y: $minY-$maxY | Z: $minZ-$maxZ"
    }
}