package org.gourmet.gourPillars.other

import org.bukkit.Location
import kotlin.math.abs
import kotlin.math.roundToInt

object LocationAlignment {
    private const val ANGLE_STEP = 45.0f

    private const val ANGLE_TOLERANCE = 12.0f
    private const val Y_TOLERANCE = 0.3

    private const val POSITION_EPSILON = 0.01
    private const val ANGLE_EPSILON = 0.01f

    private val COMPASS =
        listOf("south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east")

    data class Aligned(
        val location: Location,
        val changes: List<String>,
    )

    fun align(location: Location): Aligned {
        val aligned = location.clone()
        val changes = mutableListOf<String>()

        val centreX = location.blockX + 0.5
        val centreZ = location.blockZ + 0.5
        if (abs(location.x - centreX) > POSITION_EPSILON || abs(location.z - centreZ) > POSITION_EPSILON) {
            aligned.x = centreX
            aligned.z = centreZ
            changes += "centred on the block"
        }

        val wholeY = location.y.roundToInt().toDouble()
        if (abs(location.y - wholeY) > POSITION_EPSILON && abs(location.y - wholeY) <= Y_TOLERANCE) {
            aligned.y = wholeY
            changes += "levelled to Y ${wholeY.toInt()}"
        }

        val yaw = normalizeYaw(location.yaw)
        snap(yaw)?.let { snapped ->
            aligned.yaw = normalizeYaw(snapped)
            if (abs(yaw - snapped) > ANGLE_EPSILON) changes += "turned to face ${facingName(snapped)}"
        }

        snap(location.pitch)?.let { snapped ->
            aligned.pitch = snapped
            if (abs(location.pitch - snapped) > ANGLE_EPSILON) changes += pitchName(snapped)
        }

        return Aligned(aligned, changes)
    }

    fun facingName(yaw: Float): String = COMPASS[((normalizeYaw(yaw) / ANGLE_STEP).roundToInt()) % COMPASS.size]

    private fun snap(angle: Float): Float? {
        val nearest = (angle / ANGLE_STEP).roundToInt() * ANGLE_STEP
        return if (abs(angle - nearest) <= ANGLE_TOLERANCE) nearest else null
    }

    private fun pitchName(pitch: Float): String =
        when {
            pitch <= -89f -> "aimed straight up"
            pitch >= 89f -> "aimed straight down"
            pitch < 0f -> "tilted ${-pitch.toInt()}° up"
            pitch > 0f -> "tilted ${pitch.toInt()}° down"
            else -> "levelled the view"
        }

    private fun normalizeYaw(yaw: Float): Float = ((yaw % 360f) + 360f) % 360f
}
