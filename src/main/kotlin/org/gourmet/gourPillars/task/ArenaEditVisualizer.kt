package org.gourmet.gourPillars.task

import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.gourmet.gourPillars.commands.ArenaEdit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object ArenaEditVisualizer {
    private const val VIEW_DISTANCE = 48.0

    private const val MAX_POINTS = 300
    private const val MIN_STEP = 1.0

    private const val MAX_SPAWN_MARKERS = 64

    private val REGION_COLOR = Color.fromRGB(0x4FC3F7)
    private val MIN_HEIGHT_COLOR = Color.fromRGB(0xFF5252)
    private val MAX_HEIGHT_COLOR = Color.fromRGB(0xFFC107)
    private val SPAWN_COLOR = Color.fromRGB(0x69F0AE)
    private val DEATH_SPAWN_COLOR = Color.fromRGB(0xE040FB)
    private val PENDING_COLOR = Color.fromRGB(0xFFFFFF)

    data class Marker(
        val location: Location,
        val color: Color,
    )

    fun render(
        player: Player,
        session: ArenaEdit,
    ) {
        val world = session.world ?: return
        if (player.world != world) return

        val origin = player.location
        val maxDistanceSquared = VIEW_DISTANCE * VIEW_DISTANCE
        for (marker in markers(session)) {
            if (marker.location.distanceSquared(origin) > maxDistanceSquared) continue
            player.spawnParticle(
                Particle.DUST,
                marker.location,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                Particle.DustOptions(marker.color, 1.0f),
            )
        }
    }

    fun markers(session: ArenaEdit): List<Marker> {
        val world = session.world ?: return emptyList()
        val markers = mutableListOf<Marker>()

        val cornerOne = session.regionLocationOne
        val cornerTwo = session.regionLocationSecond
        if (cornerOne != null && cornerTwo != null) {
            markers += regionMarkers(session, cornerOne, cornerTwo)
        } else {
            (cornerOne ?: cornerTwo)?.let { markers += pillar(it, PENDING_COLOR, height = 3.0) }
        }

        session.locations.values.take(MAX_SPAWN_MARKERS).forEach { spawn ->
            markers += pillar(spawn, SPAWN_COLOR, height = 1.5)
        }
        session.deathSpawn?.let { markers += pillar(it, DEATH_SPAWN_COLOR, height = 2.5) }

        return markers.filter { it.location.world == world }
    }

    private fun regionMarkers(
        session: ArenaEdit,
        cornerOne: Location,
        cornerTwo: Location,
    ): List<Marker> {
        val world = cornerOne.world
        val minX = min(cornerOne.blockX, cornerTwo.blockX).toDouble()
        val maxX = max(cornerOne.blockX, cornerTwo.blockX) + 1.0
        val minZ = min(cornerOne.blockZ, cornerTwo.blockZ).toDouble()
        val maxZ = max(cornerOne.blockZ, cornerTwo.blockZ) + 1.0

        val floor = session.minHeight!!.toDouble()
        val ceiling = session.maxHeight!!.toDouble()

        val perimeter = 2 * ((maxX - minX) + (maxZ - minZ))
        val step = max(MIN_STEP, (perimeter * 2 + (ceiling - floor) * 4) / MAX_POINTS)

        val markers = mutableListOf<Marker>()
        for ((x, z) in listOf(minX to minZ, minX to maxZ, maxX to minZ, maxX to maxZ)) {
            markers += line(Location(world, x, floor, z), Location(world, x, ceiling, z), step, REGION_COLOR)
        }
        markers += rectangle(cornerOne, minX, maxX, minZ, maxZ, floor, step, MIN_HEIGHT_COLOR)
        markers += rectangle(cornerOne, minX, maxX, minZ, maxZ, ceiling, step, MAX_HEIGHT_COLOR)
        return markers
    }

    private fun rectangle(
        reference: Location,
        minX: Double,
        maxX: Double,
        minZ: Double,
        maxZ: Double,
        y: Double,
        step: Double,
        color: Color,
    ): List<Marker> {
        val world = reference.world
        val corners =
            listOf(
                Location(world, minX, y, minZ),
                Location(world, maxX, y, minZ),
                Location(world, maxX, y, maxZ),
                Location(world, minX, y, maxZ),
            )
        return corners.flatMapIndexed { index, corner ->
            line(corner, corners[(index + 1) % corners.size], step, color)
        }
    }

    private fun pillar(
        base: Location,
        color: Color,
        height: Double,
    ): List<Marker> = line(base.clone().add(0.0, 0.1, 0.0), base.clone().add(0.0, height, 0.0), 0.3, color)

    private fun line(
        from: Location,
        to: Location,
        step: Double,
        color: Color,
    ): List<Marker> {
        val distance = from.distance(to)
        val steps = min(max(1, ceil(distance / step).toInt()), MAX_POINTS)
        return (0..steps).map { index ->
            val progress = index.toDouble() / steps
            Marker(
                Location(
                    from.world,
                    from.x + (to.x - from.x) * progress,
                    from.y + (to.y - from.y) * progress,
                    from.z + (to.z - from.z) * progress,
                ),
                color,
            )
        }
    }
}
