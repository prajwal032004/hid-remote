package com.example.hid

import kotlin.math.abs
import kotlin.math.max

/**
 * Pure encoders for the report layouts declared in [HidConstants.HID_REPORT_DESCRIPTOR].
 */
object HidReports {

    private const val AXIS_LIMIT = 127

    data class MouseChunk(val dx: Int, val dy: Int, val wheel: Int, val pan: Int)

    fun mouse(buttons: Int, dx: Int, dy: Int, wheel: Int = 0, pan: Int = 0): ByteArray = byteArrayOf(
        (buttons and 0x1F).toByte(),
        dx.coerceIn(-AXIS_LIMIT, AXIS_LIMIT).toByte(),
        dy.coerceIn(-AXIS_LIMIT, AXIS_LIMIT).toByte(),
        wheel.coerceIn(-AXIS_LIMIT, AXIS_LIMIT).toByte(),
        pan.coerceIn(-AXIS_LIMIT, AXIS_LIMIT).toByte()
    )

    /** Up to 6 simultaneously held keys (6KRO); extra keys are dropped. */
    fun keyboard(modifiers: Int, keys: Collection<Byte>): ByteArray {
        val report = ByteArray(HidConstants.KEYBOARD_REPORT_SIZE)
        report[0] = modifiers.toByte()
        keys.take(6).forEachIndexed { i, key -> report[2 + i] = key }
        return report
    }

    fun consumer(usage: Int): ByteArray = byteArrayOf(
        (usage and 0xFF).toByte(),
        ((usage shr 8) and 0xFF).toByte()
    )

    fun emptyReport(reportId: Byte): ByteArray = when (reportId) {
        HidConstants.REPORT_ID_MOUSE -> ByteArray(HidConstants.MOUSE_REPORT_SIZE)
        HidConstants.REPORT_ID_KEYBOARD -> ByteArray(HidConstants.KEYBOARD_REPORT_SIZE)
        HidConstants.REPORT_ID_CONSUMER -> ByteArray(HidConstants.CONSUMER_REPORT_SIZE)
        else -> ByteArray(0)
    }

    /**
     * Splits an accumulated relative motion into the fewest reports that fit the 8-bit axes.
     * Every axis is spread evenly across the chunks, so a fast diagonal stays diagonal instead
     * of turning into a staircase, and the chunks always sum exactly to the input.
     */
    fun splitMotion(dx: Int, dy: Int, wheel: Int, pan: Int): List<MouseChunk> {
        val largest = max(max(abs(dx), abs(dy)), max(abs(wheel), abs(pan)))
        if (largest == 0) return emptyList()
        val n = (largest + AXIS_LIMIT - 1) / AXIS_LIMIT
        if (n == 1) return listOf(MouseChunk(dx, dy, wheel, pan))
        return List(n) { i ->
            MouseChunk(
                part(dx, i, n),
                part(dy, i, n),
                part(wheel, i, n),
                part(pan, i, n)
            )
        }
    }

    private fun part(total: Int, i: Int, n: Int): Int = total * (i + 1) / n - total * i / n
}
