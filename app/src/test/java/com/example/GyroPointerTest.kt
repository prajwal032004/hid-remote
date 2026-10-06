package com.example

import com.example.input.GyroPointer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GyroPointerTest {

    private val stepNs = 5_000_000L // 200 Hz, like SENSOR_DELAY_GAME on most phones

    private fun sweep(pointer: GyroPointer, pitch: Float, yaw: Float, events: Int): Pair<Int, Int> {
        var x = 0
        var y = 0
        var t = 1_000_000_000L
        pointer.process(0f, 0f, t) // first event only primes the clock
        repeat(events) {
            t += stepNs
            val out = pointer.process(pitch, yaw, t)
            x += out.dx
            y += out.dy
        }
        return x to y
    }

    @Test
    fun `resting hand inside the dead zone does not move the cursor`() {
        val (x, y) = sweep(GyroPointer(), pitch = 0.02f, yaw = -0.03f, events = 400)
        assertEquals(0, x)
        assertEquals(0, y)
    }

    @Test
    fun `turning left moves the cursor left and tilting up moves it up`() {
        val (x, y) = sweep(GyroPointer(), pitch = 0.8f, yaw = 0.8f, events = 100)
        assertTrue("x=$x", x < 0)
        assertTrue("y=$y", y < 0)
    }

    @Test
    fun `slow sweeps accumulate sub-count motion`() {
        // 0.1 rad/s for 2 s: (0.1 - dead zone) * 900 * 2 ≈ 117 counts, nothing truncated away.
        val (x, _) = sweep(GyroPointer(), pitch = 0f, yaw = -0.1f, events = 400)
        assertTrue("x=$x", x in 110..125)
    }

    @Test
    fun `sensitivity scales motion linearly`() {
        val (slow, _) = sweep(GyroPointer().apply { sensitivity = 1f }, 0f, -1f, 100)
        val (fast, _) = sweep(GyroPointer().apply { sensitivity = 2f }, 0f, -1f, 100)
        assertEquals(slow * 2.0, fast.toDouble(), 2.0)
    }

    @Test
    fun `a long sensor gap does not cause a jump`() {
        val pointer = GyroPointer()
        pointer.process(0f, 0f, 0L)
        val out = pointer.process(0f, -2f, 3_000_000_000L) // 3 s later
        assertTrue("dx=${out.dx}", out.dx < 200)
    }
}
