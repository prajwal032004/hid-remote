package com.example

import com.example.hid.HidKeyCodes
import com.example.input.PointerAccelerator
import com.example.input.ScrollAccumulator
import com.example.input.TextDiff
import com.example.model.HostCommand
import com.example.model.HostOs
import com.example.model.ShortcutAction
import com.example.model.commandFor
import com.example.model.describe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class InputProcessingTest {

    private fun linear() = PointerAccelerator().apply {
        sensitivity = 1f
        acceleration = 1f
    }

    @Test
    fun `slow sub-count motion is accumulated, not truncated`() {
        val acc = linear()
        var total = 0
        var t = 0L
        // 200 events of 0.3 dp at 8 ms: 60 dp total -> 120 counts at 2 counts/dp.
        repeat(200) {
            t += 8
            total += acc.process(0.3f, 0f, t).dx
        }
        assertEquals(120, total)
    }

    @Test
    fun `linear mode is independent of event rate`() {
        fun run(stepDp: Float, stepMs: Long, events: Int): Int {
            val acc = linear()
            var t = 0L
            var sum = 0
            repeat(events) {
                t += stepMs
                sum += acc.process(stepDp, 0f, t).dx
            }
            return sum
        }
        // Same 96 dp stroke at 60 Hz and 240 Hz.
        val at60 = run(1.6f, 16, 60)
        val at240 = run(0.4f, 4, 240)
        assertTrue("60Hz=$at60 240Hz=$at240", abs(at60 - at240) <= 1)
    }

    @Test
    fun `acceleration boosts fast strokes more than slow ones`() {
        val acc = PointerAccelerator().apply { acceleration = 1.5f }
        val slowGain = acc.gainFor(0.1f)
        val fastGain = acc.gainFor(2f)
        assertTrue(fastGain > slowGain * 2f)
        assertTrue(acc.gainFor(0.0f) < 1f) // precision zone for fine positioning
    }

    @Test
    fun `resting finger noise is gated out`() {
        val acc = PointerAccelerator().apply { sensitivity = 3f }
        var t = 0L
        var moved = 0
        repeat(100) { i ->
            t += 8
            val wobble = if (i % 2 == 0) 0.1f else -0.1f
            val out = acc.process(wobble, -wobble, t)
            moved += abs(out.dx) + abs(out.dy)
        }
        assertEquals(0, moved)
    }

    @Test
    fun `scroll ticks carry the remainder`() {
        val scroll = ScrollAccumulator(dpPerTick = 10f)
        var ticks = 0
        repeat(25) { ticks += scroll.add(2f) } // 50 dp
        assertEquals(5, ticks)
        repeat(10) { ticks += scroll.add(-3f) } // back 30 dp
        assertEquals(2, ticks)
    }

    @Test
    fun `text diff handles appends, deletions and replacements`() {
        assertEquals("o", TextDiff.keystrokes("hell", "hello"))
        assertEquals("\b\b", TextDiff.keystrokes("hello", "hel"))
        // Autocorrect "teh" -> "the ": rewind to the first difference, then retype.
        assertEquals("\b\bhe ", TextDiff.keystrokes("teh", "the "))
        assertEquals("", TextDiff.keystrokes("same", "same"))
    }

    @Test
    fun `shortcuts adapt to the host OS`() {
        val ctrl = HidKeyCodes.MOD_LEFT_CTRL.toInt()
        val gui = HidKeyCodes.MOD_LEFT_GUI.toInt()
        assertEquals(HostCommand.Key(ctrl, HidKeyCodes.KEY_C), HostOs.WINDOWS.commandFor(ShortcutAction.COPY))
        assertEquals(HostCommand.Key(gui, HidKeyCodes.KEY_C), HostOs.MAC.commandFor(ShortcutAction.COPY))
        assertTrue(HostOs.ANDROID.commandFor(ShortcutAction.SHOW_DESKTOP) is HostCommand.Consumer)
        assertNull(HostOs.ANDROID.commandFor(ShortcutAction.LOCK_SCREEN))
        assertEquals("Ctrl + C", HostOs.WINDOWS.describe(ShortcutAction.COPY))
        assertEquals("⌘ C", HostOs.MAC.describe(ShortcutAction.COPY))
        assertEquals("Win + D", HostOs.WINDOWS.describe(ShortcutAction.SHOW_DESKTOP))
    }
}
