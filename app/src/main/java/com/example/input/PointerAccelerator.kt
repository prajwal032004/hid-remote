package com.example.input

import kotlin.math.exp
import kotlin.math.hypot

/**
 * Converts finger motion (in dp) into HID mouse counts.
 *
 * - **Velocity-based gain**: the curve is driven by a smoothed finger speed (dp/ms), not by the
 *   per-event delta, so it behaves the same at 60 Hz, 120 Hz or 240 Hz touch sampling.
 * - **Sub-count residuals**: the fractional part of every delta is carried into the next one.
 *   Nothing is truncated away, so slow motion is exact and the cursor never stair-steps.
 * - **Noise gate**: sub-threshold wobble from a resting finger is ignored, removing jitter
 *   without adding the lag that a low-pass filter on the motion itself would.
 */
class PointerAccelerator {

    data class Output(val dx: Int, val dy: Int)

    /** Linear multiplier. 1.0 is the default feel. */
    var sensitivity: Float = 1f

    /** 1.0 = off (linear); larger values boost fast flicks harder. */
    var acceleration: Float = 1.25f

    private var residualX = 0f
    private var residualY = 0f
    private var smoothedSpeed = 0f
    private var lastEventMs = -1L

    /** Call on every new touch-down so a new stroke doesn't inherit stale state. */
    fun reset() {
        residualX = 0f
        residualY = 0f
        smoothedSpeed = 0f
        lastEventMs = -1L
    }

    fun process(dxDp: Float, dyDp: Float, eventTimeMs: Long): Output {
        val dist = hypot(dxDp, dyDp)
        val dtMs = if (lastEventMs < 0) NOMINAL_FRAME_MS else (eventTimeMs - lastEventMs).toFloat()
        lastEventMs = eventTimeMs
        val dt = if (dtMs <= 0f || dtMs > STALE_GAP_MS) NOMINAL_FRAME_MS else dtMs.coerceAtLeast(1f)

        val instantSpeed = dist / dt
        val alpha = 1f - exp(-dt / SPEED_SMOOTHING_TAU_MS)
        smoothedSpeed += (instantSpeed - smoothedSpeed) * alpha

        if (dist < NOISE_GATE_DP && smoothedSpeed < NOISE_GATE_SPEED) {
            return ZERO
        }

        val scale = BASE_COUNTS_PER_DP * sensitivity * gainFor(smoothedSpeed)
        residualX += dxDp * scale
        residualY += dyDp * scale
        val outX = residualX.toInt()
        val outY = residualY.toInt()
        residualX -= outX
        residualY -= outY
        return if (outX == 0 && outY == 0) ZERO else Output(outX, outY)
    }

    /** Gain as a function of speed in dp/ms. Exposed for tests. */
    fun gainFor(speed: Float): Float {
        val boostAmount = ((acceleration - 1f) * 3.5f).coerceAtLeast(0f)
        if (boostAmount == 0f) return 1f
        val precision = lerp(PRECISION_GAIN, 1f, smoothstep(0.02f, 0.20f, speed))
        val boost = 1f + boostAmount * smoothstep(0.25f, 1.6f, speed)
        return precision * boost
    }

    companion object {
        /** Mouse counts per dp at gain 1 and sensitivity 1. */
        const val BASE_COUNTS_PER_DP = 2.0f
        private const val PRECISION_GAIN = 0.72f
        private const val NOMINAL_FRAME_MS = 8f
        private const val STALE_GAP_MS = 100f
        private const val SPEED_SMOOTHING_TAU_MS = 20f
        private const val NOISE_GATE_DP = 0.18f
        private const val NOISE_GATE_SPEED = 0.03f
        private val ZERO = Output(0, 0)
    }
}

/** Turns scroll distance (dp) into whole wheel ticks, carrying the remainder forward. */
class ScrollAccumulator(var dpPerTick: Float = 14f) {
    private var residual = 0f

    fun reset() {
        residual = 0f
    }

    fun add(dp: Float): Int {
        residual += dp / dpPerTick
        val ticks = residual.toInt()
        residual -= ticks
        return ticks
    }
}

internal fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
