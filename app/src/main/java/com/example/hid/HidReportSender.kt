package com.example.hid

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import android.util.Log

/**
 * Owns every outgoing HID report on one dedicated high-priority thread.
 *
 * - Pointer motion and scrolling are *coalesced*: deltas accumulate between flushes and go out
 *   as one report at most every [MOTION_INTERVAL_MS]. If the radio is slow, motion merges
 *   instead of queueing, so latency never builds up behind a backlog of stale reports.
 * - Buttons, keys and consumer keys run through one ordered queue, so a key-up can never
 *   overtake its key-down, and pending motion is always flushed before a click lands.
 * - When the queue goes idle, the final key/button/media state is sent once more. A congested
 *   radio silently drops reports, and a lost key-up would otherwise leave a key held down on
 *   the host (Windows then auto-repeats it).
 */
class HidReportSender(private val transport: Transport) {

    interface Transport {
        fun isReady(): Boolean
        fun send(reportId: Byte, data: ByteArray): Boolean
    }

    companion object {
        private const val TAG = "HidReportSender"
        /** ~125 Hz: as smooth as a typical Bluetooth mouse without flooding the link. */
        const val MOTION_INTERVAL_MS = 8L
        private const val CLICK_HOLD_MS = 16L
        private const val MULTI_CLICK_GAP_MS = 50L
        private const val KEY_HOLD_MS = 14L
        private const val KEY_GAP_MS = 8L
        private const val CONSUMER_HOLD_MS = 40L
        private const val SYNC_DELAY_MS = 60L
        private const val SEND_RETRY_DELAY_MS = 3L
    }

    private val thread = HandlerThread("hid-tx", Process.THREAD_PRIORITY_URGENT_DISPLAY).apply { start() }
    private val handler = Handler(thread.looper)

    // ---- Motion accumulator (any thread, guarded by lock) ----
    private val lock = Any()
    private var pendingX = 0
    private var pendingY = 0
    private var pendingWheel = 0
    private var pendingPan = 0
    private var flushScheduled = false
    @Volatile private var lastMouseSendAt = 0L

    // ---- Device state (hid-tx thread only) ----
    private var heldButtons = 0
    private var buttons = 0
    private var stickyModifiers = 0
    private val heldKeys = LinkedHashMap<Byte, Int>() // keyCode -> modifiers held with it

    private class Op(val gapAfterMs: Long, val action: () -> Unit)
    private val ops = ArrayDeque<Op>()
    private var draining = false

    // Which report types changed since the last idle sync (hid-tx thread only).
    private var keyboardDirty = false
    private var buttonsDirty = false
    private var consumerDirty = false

    private val flushRunnable = Runnable { flushMotion() }
    private val drainRunnable = Runnable { drain() }
    private val syncRunnable = Runnable { syncState() }

    // ------------------------------------------------------------------
    // Pointer
    // ------------------------------------------------------------------

    fun move(dx: Int, dy: Int) = addMotion(dx, dy, 0, 0)

    fun scroll(wheel: Int, pan: Int) = addMotion(0, 0, wheel, pan)

    private fun addMotion(dx: Int, dy: Int, wheel: Int, pan: Int) {
        if (dx == 0 && dy == 0 && wheel == 0 && pan == 0) return
        if (!transport.isReady()) return
        synchronized(lock) {
            pendingX += dx
            pendingY += dy
            pendingWheel += wheel
            pendingPan += pan
            if (flushScheduled) return
            flushScheduled = true
        }
        val wait = MOTION_INTERVAL_MS - (SystemClock.uptimeMillis() - lastMouseSendAt)
        if (wait > 0) handler.postDelayed(flushRunnable, wait) else handler.post(flushRunnable)
    }

    private fun flushMotion() {
        val x: Int
        val y: Int
        val w: Int
        val p: Int
        synchronized(lock) {
            x = pendingX; y = pendingY; w = pendingWheel; p = pendingPan
            pendingX = 0; pendingY = 0; pendingWheel = 0; pendingPan = 0
            flushScheduled = false
        }
        for (chunk in HidReports.splitMotion(x, y, w, p)) {
            sendMouse(chunk.dx, chunk.dy, chunk.wheel, chunk.pan)
        }
    }

    private fun sendMouse(dx: Int = 0, dy: Int = 0, wheel: Int = 0, pan: Int = 0) {
        if (dx == 0 && dy == 0 && wheel == 0 && pan == 0) buttonsDirty = true
        send(HidConstants.REPORT_ID_MOUSE, HidReports.mouse(buttons, dx, dy, wheel, pan))
        lastMouseSendAt = SystemClock.uptimeMillis()
    }

    /** Buttons that stay down until changed again (physical buttons, drag lock, tap-drag). */
    fun setHeldButtons(mask: Int) = enqueue(Op(0) {
        heldButtons = mask
        if (buttons != mask) {
            buttons = mask
            sendMouse()
        }
    })

    fun click(mask: Int, count: Int = 1) {
        val sequence = ArrayList<Op>(count * 2)
        repeat(count) { i ->
            sequence += Op(CLICK_HOLD_MS) {
                buttons = heldButtons or mask
                sendMouse()
            }
            sequence += Op(if (i < count - 1) MULTI_CLICK_GAP_MS else 0) {
                buttons = heldButtons
                sendMouse()
            }
        }
        enqueue(*sequence.toTypedArray())
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    fun setStickyModifiers(mask: Int) = enqueue(Op(0) {
        if (stickyModifiers != mask) {
            stickyModifiers = mask
            sendKeyboard()
        }
    })

    fun keyDown(modifiers: Int, keyCode: Byte) = enqueue(Op(0) {
        heldKeys.remove(keyCode)
        heldKeys[keyCode] = modifiers
        sendKeyboard()
    })

    fun keyUp(keyCode: Byte) = enqueue(Op(0) {
        if (heldKeys.remove(keyCode) != null) sendKeyboard()
    })

    fun tapKey(modifiers: Int, keyCode: Byte) = enqueue(*tapOps(modifiers, keyCode))

    /** Types a sequence of key events in order, then invokes [onDone] on the hid-tx thread. */
    fun typeKeys(events: List<CharToHidMapper.HidKeyEvent>, onDone: (() -> Unit)? = null) {
        val sequence = ArrayList<Op>(events.size * 3 + 1)
        for (event in events) sequence += tapOps(event.modifier.toInt(), event.keyCode)
        sequence += Op(0) { onDone?.invoke() }
        enqueue(*sequence.toTypedArray())
    }

    private fun tapOps(modifiers: Int, keyCode: Byte): Array<Op> {
        val mods = modifiers and 0xFF
        val down = Op(KEY_HOLD_MS) {
            // Present the modifier on its own first; some TVs and macOS drop a modifier that
            // arrives in the same report as the key it modifies.
            if (mods != 0 && (currentModifiers() and mods) != mods) {
                keyboardDirty = true
                send(HidConstants.REPORT_ID_KEYBOARD, HidReports.keyboard(currentModifiers() or mods, heldKeys.keys))
            }
            heldKeys.remove(keyCode)
            heldKeys[keyCode] = mods
            sendKeyboard()
        }
        val up = Op(KEY_GAP_MS) {
            heldKeys.remove(keyCode)
            sendKeyboard()
        }
        return arrayOf(down, up)
    }

    private fun currentModifiers(): Int {
        var mods = stickyModifiers
        for (m in heldKeys.values) mods = mods or m
        return mods and 0xFF
    }

    private fun sendKeyboard() {
        keyboardDirty = true
        send(HidConstants.REPORT_ID_KEYBOARD, HidReports.keyboard(currentModifiers(), heldKeys.keys))
    }

    // ------------------------------------------------------------------
    // Consumer (media) keys
    // ------------------------------------------------------------------

    fun tapConsumer(usage: Int) = enqueue(
        Op(CONSUMER_HOLD_MS) {
            consumerDirty = true
            send(HidConstants.REPORT_ID_CONSUMER, HidReports.consumer(usage))
        },
        Op(0) { send(HidConstants.REPORT_ID_CONSUMER, HidReports.consumer(HidConsumerKeys.NONE)) }
    )

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Drops anything queued and puts every key and button up. Use on connect, disconnect and
     * when the app leaves the foreground so nothing can get stuck down on the host.
     */
    fun releaseAll() {
        synchronized(lock) {
            pendingX = 0; pendingY = 0; pendingWheel = 0; pendingPan = 0
        }
        handler.post {
            ops.clear()
            handler.removeCallbacks(drainRunnable)
            handler.removeCallbacks(syncRunnable)
            keyboardDirty = false
            buttonsDirty = false
            consumerDirty = false
            draining = false
            heldButtons = 0
            buttons = 0
            stickyModifiers = 0
            heldKeys.clear()
            if (transport.isReady()) {
                send(HidConstants.REPORT_ID_MOUSE, HidReports.emptyReport(HidConstants.REPORT_ID_MOUSE))
                send(HidConstants.REPORT_ID_KEYBOARD, HidReports.emptyReport(HidConstants.REPORT_ID_KEYBOARD))
                send(HidConstants.REPORT_ID_CONSUMER, HidReports.emptyReport(HidConstants.REPORT_ID_CONSUMER))
            }
        }
    }

    fun shutdown() {
        handler.removeCallbacksAndMessages(null)
        thread.quitSafely()
    }

    // ------------------------------------------------------------------

    private fun enqueue(vararg newOps: Op) {
        handler.post {
            ops.addAll(newOps)
            if (!draining) drain()
        }
    }

    private fun drain() {
        draining = true
        while (ops.isNotEmpty()) {
            val op = ops.removeFirst()
            flushMotion()
            op.action()
            if (op.gapAfterMs > 0 && ops.isNotEmpty()) {
                handler.postDelayed(drainRunnable, op.gapAfterMs)
                return
            }
        }
        draining = false
        if (keyboardDirty || buttonsDirty || consumerDirty) {
            handler.removeCallbacks(syncRunnable)
            handler.postDelayed(syncRunnable, SYNC_DELAY_MS)
        }
    }

    /** Re-sends the settled state once the queue is idle; identical reports are harmless. */
    private fun syncState() {
        if (draining || ops.isNotEmpty()) return
        if (keyboardDirty) send(HidConstants.REPORT_ID_KEYBOARD, HidReports.keyboard(currentModifiers(), heldKeys.keys))
        if (buttonsDirty) send(HidConstants.REPORT_ID_MOUSE, HidReports.mouse(buttons, 0, 0))
        if (consumerDirty) send(HidConstants.REPORT_ID_CONSUMER, HidReports.consumer(HidConsumerKeys.NONE))
        keyboardDirty = false
        buttonsDirty = false
        consumerDirty = false
    }

    private fun send(reportId: Byte, data: ByteArray) {
        repeat(2) { attempt ->
            if (!transport.isReady()) return
            try {
                if (transport.send(reportId, data)) return
                Log.v(TAG, "sendReport($reportId) rejected (attempt ${attempt + 1})")
            } catch (e: Exception) {
                Log.w(TAG, "sendReport($reportId) failed", e)
            }
            SystemClock.sleep(SEND_RETRY_DELAY_MS)
        }
    }
}
