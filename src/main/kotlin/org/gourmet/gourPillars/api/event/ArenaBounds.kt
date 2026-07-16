package org.gourmet.gourPillars.api.event

import org.bukkit.Location

// Block-coordinate bounding box of an arena's build region (inclusive on both ends).
data class ArenaBounds(
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
) {
    val centerX: Double get() = (minX + maxX) / 2.0
    val centerZ: Double get() = (minZ + maxZ) / 2.0

    // World is not checked; compare against GameEventContext.world yourself if needed.
    fun contains(location: Location): Boolean =
        location.x >= minX && location.x <= maxX &&
            location.y >= minY && location.y <= maxY &&
            location.z >= minZ && location.z <= maxZ
}
