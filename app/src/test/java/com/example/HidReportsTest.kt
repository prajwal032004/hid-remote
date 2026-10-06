package com.example

import com.example.hid.HidConstants
import com.example.hid.HidReports
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class HidReportsTest {

    @Test
    fun `small motion is a single chunk`() {
        assertEquals(listOf(HidReports.MouseChunk(10, -20, 1, 0)), HidReports.splitMotion(10, -20, 1, 0))
    }

    @Test
    fun `no motion produces no reports`() {
        assertTrue(HidReports.splitMotion(0, 0, 0, 0).isEmpty())
    }

    @Test
    fun `large motion splits into in-range chunks that sum exactly`() {
        val chunks = HidReports.splitMotion(1000, -333, 5, -300)
        assertEquals(1000, chunks.sumOf { it.dx })
        assertEquals(-333, chunks.sumOf { it.dy })
        assertEquals(5, chunks.sumOf { it.wheel })
        assertEquals(-300, chunks.sumOf { it.pan })
        chunks.forEach {
            assertTrue(abs(it.dx) <= 127 && abs(it.dy) <= 127 && abs(it.wheel) <= 127 && abs(it.pan) <= 127)
        }
        assertEquals(8, chunks.size) // ceil(1000 / 127)
    }

    @Test
    fun `diagonal stays diagonal when split`() {
        val chunks = HidReports.splitMotion(400, 400, 0, 0)
        chunks.forEach { assertEquals(it.dx, it.dy) }
    }

    @Test
    fun `mouse report layout`() {
        assertArrayEquals(
            byteArrayOf(0x01, 5, -3, 1, -1),
            HidReports.mouse(buttons = 1, dx = 5, dy = -3, wheel = 1, pan = -1)
        )
        assertEquals(HidConstants.MOUSE_REPORT_SIZE, HidReports.mouse(0, 0, 0).size)
    }

    @Test
    fun `keyboard report holds modifiers and up to six keys`() {
        val report = HidReports.keyboard(0x02, listOf<Byte>(4, 5, 6, 7, 8, 9, 10))
        assertEquals(HidConstants.KEYBOARD_REPORT_SIZE, report.size)
        assertEquals(0x02.toByte(), report[0])
        assertEquals(0.toByte(), report[1])
        assertArrayEquals(byteArrayOf(4, 5, 6, 7, 8, 9), report.copyOfRange(2, 8))
    }

    @Test
    fun `consumer usage is 16-bit little endian`() {
        assertArrayEquals(byteArrayOf(0x23, 0x02), HidReports.consumer(0x223))
        assertEquals(HidConstants.CONSUMER_REPORT_SIZE, HidReports.consumer(0).size)
    }

    /**
     * Walks the report descriptor like a host would and checks that each report ID's input
     * payload matches what the app sends. A mismatch here breaks input on strict hosts.
     */
    @Test
    fun `descriptor declares exactly the report sizes the app sends`() {
        val inputBits = HashMap<Int, Int>()
        val outputBits = HashMap<Int, Int>()
        var reportId = 0
        var reportSize = 0
        var reportCount = 0
        var depth = 0
        val d = HidConstants.HID_REPORT_DESCRIPTOR
        var i = 0
        while (i < d.size) {
            val prefix = d[i].toInt() and 0xFF
            val size = when (prefix and 0x03) { 3 -> 4; else -> prefix and 0x03 }
            var value = 0
            for (b in 0 until size) value = value or ((d[i + 1 + b].toInt() and 0xFF) shl (8 * b))
            when (prefix and 0xFC) {
                0x84 -> reportId = value
                0x74 -> reportSize = value
                0x94 -> reportCount = value
                0x80 -> inputBits.merge(reportId, reportSize * reportCount, Int::plus)
                0x90 -> outputBits.merge(reportId, reportSize * reportCount, Int::plus)
                0xA0 -> depth++
                0xC0 -> depth--
            }
            i += 1 + size
        }
        assertEquals("collections must be balanced", 0, depth)
        assertEquals(HidConstants.MOUSE_REPORT_SIZE * 8, inputBits[HidConstants.REPORT_ID_MOUSE.toInt()])
        assertEquals(HidConstants.KEYBOARD_REPORT_SIZE * 8, inputBits[HidConstants.REPORT_ID_KEYBOARD.toInt()])
        assertEquals(HidConstants.CONSUMER_REPORT_SIZE * 8, inputBits[HidConstants.REPORT_ID_CONSUMER.toInt()])
        assertEquals(8, outputBits[HidConstants.REPORT_ID_KEYBOARD.toInt()]) // LED byte
    }
}
