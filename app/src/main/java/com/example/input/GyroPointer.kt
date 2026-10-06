package com.example.input

import kotlin.math.abs
import kotlin.math.sign

/**
 * Air mouse: converts gyroscope angular velocity (rad/s) into HID mouse counts, so the phone can
 * be waved like a laser pointer. Yaw (rotation about the screen normal) moves the cursor
 * sideways; pitch (tilting the top edge up or down) moves it vertically.
 *
 * - **Dead zone**: hand tremor and sensor drift below [DEAD_ZONE_RAD_S] are ignored, and the zone
 *   is subtracted rather than gated so motion ramps up smoothly from zero.
 * - **Sub-count residuals**: like [PointerAccelerator], fractions carry over so slow sweeps are exact.
 */
class GyroPointer {

    /** Linear multiplier. 1.0 is the default feel. */
    var sensitivity: Float = 1f

    private var residualX = 0f
    private var residualY = 0f
    private var lastTimestampNs = -1L

    fun reset() {
        residualX = 0f
        residualY = 0f
        lastTimestampNs = -1L
    }

    /**
     * [pitchRadS] is the device X-axis rate and [yawRadS] the Z-axis rate, both from
     * `Sensor.TYPE_GYROSCOPE`; [timestampNs] is the sensor event timestamp.
     */
    fun process(pitchRadS: Float, yawRadS: Float, timestampNs: Long): PointerAccelerator.Output {
        val last = lastTimestampNs
        lastTimestampNs = timestampNs
        if (last < 0) return ZERO
        val dt = ((timestampNs - last) / 1e9f).coerceIn(0f, MAX_DT_S)
        if (dt == 0f) return ZERO

        val scale = COUNTS_PER_RAD * sensitivity * dt
        // Counter-clockwise yaw swings the top edge left; tilting the top up is positive pitch.
        residualX += -shape(yawRadS) * scale
        residualY += -shape(pitchRadS) * scale
        val outX = residualX.toInt()
        val outY = residualY.toInt()
        residualX -= outX
        residualY -= outY
        return if (outX == 0 && outY == 0) ZERO else PointerAccelerator.Output(outX, outY)
    }

    /** Soft dead zone plus a gentle boost for fast sweeps across a big screen. */
    private fun shape(rate: Float): Float {
        val magnitude = abs(rate) - DEAD_ZONE_RAD_S
        if (magnitude <= 0f) return 0f
        val boost = 1f + BOOST * (magnitude / BOOST_FULL_RAD_S).coerceAtMost(1f)
        return sign(rate) * magnitude * boost
    }

    companion object {
        const val DEAD_ZONE_RAD_S = 0.035f
        const val COUNTS_PER_RAD = 900f
        const val BOOST = 0.8f
        const val BOOST_FULL_RAD_S = 4f

        /** Longer gaps (sensor paused, app resumed) must not turn into one giant jump. */
        const val MAX_DT_S = 0.05f
        private val ZERO = PointerAccelerator.Output(0, 0)
    }
}
