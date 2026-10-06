package com.example.ui.components

import android.os.Build
import android.view.InputDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.animation.animateColorAsState
import com.example.ui.theme.AppTheme
import com.example.viewmodel.SwipeDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot

private const val TAP_TIMEOUT_MS = 220L
private const val MULTI_TAP_TIMEOUT_MS = 300L
private const val DOUBLE_TAP_WINDOW_MS = 200L
private const val DOUBLE_TAP_RADIUS_DP = 64f
private const val SWIPE_DISTANCE_DP = 56f
private const val AXIS_LOCK_RATIO = 1.8f
private const val FLING_MIN_DP_PER_MS = 0.35f
private const val FLING_STOP_DP_PER_MS = 0.02f
private const val FLING_DECAY_TAU_MS = 325f

private enum class ScrollAxis { UNDECIDED, FREE, VERTICAL, HORIZONTAL }

@Composable
fun TouchpadView(
    isConnected: Boolean,
    isDragLockActive: Boolean,
    isTapDragActive: Boolean,
    tapToClick: Boolean,
    tapToDrag: Boolean,
    scrollInertia: Boolean,
    onPointerDown: () -> Unit,
    onPointerMove: (dxDp: Float, dyDp: Float, eventTimeMs: Long) -> Unit,
    onScroll: (dxDp: Float, dyDp: Float) -> Unit,
    onTap: (fingers: Int, count: Int) -> Unit,
    onTapDrag: (active: Boolean) -> Unit,
    onSwipe: (SwipeDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    // Gesture code runs in a long-lived pointerInput coroutine, so it must read the latest values.
    val currentTapToClick by rememberUpdatedState(tapToClick)
    val currentTapToDrag by rememberUpdatedState(tapToDrag)
    val currentInertia by rememberUpdatedState(scrollInertia)
    val pointerDown by rememberUpdatedState(onPointerDown)
    val pointerMove by rememberUpdatedState(onPointerMove)
    val scroll by rememberUpdatedState(onScroll)
    val tap by rememberUpdatedState(onTap)
    val tapDrag by rememberUpdatedState(onTapDrag)
    val swipe by rememberUpdatedState(onSwipe)

    val scope = rememberCoroutineScope()
    // Read only in the draw phase, so per-event updates redraw without recomposing.
    val touchPoints = remember { mutableStateOf(emptyList<Offset>()) }
    val lastPoints = remember { arrayOf(emptyList<Offset>()) }
    var isTouching by remember { mutableStateOf(false) }
    var isScrolling by remember { mutableStateOf(false) }
    var hasTouched by remember { mutableStateOf(false) }

    // Deliver touch events as soon as the digitizer produces them instead of batching them to
    // the next display frame. This removes up to one frame (8-16 ms) of input latency.
    val view = LocalView.current
    DisposableEffect(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.requestUnbufferedDispatch(InputDevice.SOURCE_TOUCHSCREEN)
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) view.requestUnbufferedDispatch(0)
        }
    }

    val colors = AppTheme.colors
    val accent = colors.accent
    val dragging = isDragLockActive || isTapDragActive
    val borderColor by animateColorAsState(
        if (dragging) colors.warning.copy(alpha = 0.8f) else colors.outline,
        tween(220),
        label = "pad_border"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isTouching) 1f else 0f,
        animationSpec = tween(if (isTouching) 60 else 260),
        label = "touch_glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(colors.surface, colors.surface.copy(alpha = 0.7f))
                )
            )
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .testTag("touchpad_surface")
            .semantics { contentDescription = "Touchpad" }
            .drawWithCache {
                // The dot grid only depends on size, so it is built once per layout, not per frame.
                val spacing = 28.dp.toPx()
                val points = ArrayList<Offset>()
                var x = spacing
                while (x < size.width - spacing / 2) {
                    var y = spacing
                    while (y < size.height - spacing / 2) {
                        points += Offset(x, y)
                        y += spacing
                    }
                    x += spacing
                }
                val dotColor = accent.copy(alpha = 0.10f)
                val dotSize = 2.5.dp.toPx()
                onDrawBehind {
                    drawPoints(points, PointMode.Points, dotColor, strokeWidth = dotSize, cap = StrokeCap.Round)
                }
            }
            .drawBehind {
                if (glowAlpha > 0f) {
                    val halo = 44.dp.toPx()
                    val live = touchPoints.value
                    for (point in live.ifEmpty { lastPoints[0] }) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(accent.copy(alpha = 0.30f * glowAlpha), Color.Transparent),
                                center = point,
                                radius = halo
                            ),
                            radius = halo,
                            center = point
                        )
                        drawCircle(accent.copy(alpha = 0.9f * glowAlpha), radius = 7.dp.toPx(), center = point)
                    }
                }
            }
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                val pxPerDp = density
                var lastTapUpTime = 0L
                var lastTapPos = Offset.Zero
                var pendingTap: Job? = null
                var inertia: Job? = null

                fun startInertia(vxDpMs: Float, vyDpMs: Float) {
                    inertia = scope.launch {
                        var vx = vxDpMs
                        var vy = vyDpMs
                        var last = withFrameNanos { it }
                        while (isActive) {
                            val now = withFrameNanos { it }
                            val dt = (now - last) / 1_000_000f
                            last = now
                            val decay = exp(-dt / FLING_DECAY_TAU_MS)
                            vx *= decay
                            vy *= decay
                            if (hypot(vx, vy) < FLING_STOP_DP_PER_MS) break
                            scroll(vx * dt, vy * dt)
                        }
                    }
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    inertia?.cancel()
                    hasTouched = true
                    isTouching = true
                    pointerDown()

                    val start = down.uptimeMillis
                    val tapDragEnabled = currentTapToClick && currentTapToDrag
                    // A touch that lands right after a tap may become a drag or a double-click.
                    var candidate = tapDragEnabled &&
                        pendingTap?.isActive == true &&
                        start - lastTapUpTime < DOUBLE_TAP_WINDOW_MS &&
                        (down.position - lastTapPos).getDistance() < DOUBLE_TAP_RADIUS_DP * pxPerDp
                    if (pendingTap?.isActive == true) {
                        pendingTap?.cancel()
                        if (!candidate) tap(1, 1) // flush the earlier tap now, in order
                    }
                    pendingTap = null

                    var maxPointers = 1
                    var travel = 0f
                    var dragging = false
                    var multiTravel = 0f
                    var axis = ScrollAxis.UNDECIDED
                    var axisProbe = Offset.Zero
                    var swipeAccum = Offset.Zero
                    var swiped = false
                    var scrollPos = Offset.Zero
                    val velocity = VelocityTracker()
                    var endTime = start
                    var endPos = down.position

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        event.changes.forEach { endTime = maxOf(endTime, it.uptimeMillis) }
                        if (pressed.isNotEmpty()) {
                            val points = pressed.map { it.position }
                            touchPoints.value = points
                            lastPoints[0] = points
                        }
                        if (pressed.isEmpty()) {
                            endPos = event.changes.firstOrNull()?.position ?: endPos
                            event.changes.forEach { it.consume() }
                            break
                        }

                        if (pressed.size > maxPointers) {
                            maxPointers = pressed.size
                            if (candidate && !dragging) {
                                tap(1, 1) // the earlier single tap was real; this is a new gesture
                                candidate = false
                            }
                        }

                        when {
                            maxPointers == 1 -> {
                                val c = pressed[0]
                                if (c.previousPressed) {
                                    val d = c.position - c.previousPosition
                                    travel += d.getDistance()
                                    if (candidate && !dragging && travel > slop) {
                                        dragging = true
                                        tapDrag(true)
                                    }
                                    if (d != Offset.Zero) pointerMove(d.x / pxPerDp, d.y / pxPerDp, c.uptimeMillis)
                                }
                            }

                            maxPointers == 2 -> {
                                val moving = pressed.filter { it.previousPressed }
                                if (moving.size >= 2) {
                                    var sx = 0f
                                    var sy = 0f
                                    for (m in moving) {
                                        sx += m.position.x - m.previousPosition.x
                                        sy += m.position.y - m.previousPosition.y
                                    }
                                    val d = Offset(sx / moving.size, sy / moving.size)
                                    multiTravel += d.getDistance()
                                    scrollPos += d
                                    velocity.addPosition(moving[0].uptimeMillis, scrollPos)

                                    if (axis == ScrollAxis.UNDECIDED) {
                                        axisProbe += d
                                        if (multiTravel > slop) {
                                            axis = when {
                                                abs(axisProbe.y) > abs(axisProbe.x) * AXIS_LOCK_RATIO -> ScrollAxis.VERTICAL
                                                abs(axisProbe.x) > abs(axisProbe.y) * AXIS_LOCK_RATIO -> ScrollAxis.HORIZONTAL
                                                else -> ScrollAxis.FREE
                                            }
                                            isScrolling = true
                                            // Deliver the distance travelled while deciding, so nothing is lost.
                                            val p = lockAxis(axisProbe, axis)
                                            scroll(p.x / pxPerDp, p.y / pxPerDp)
                                        }
                                    } else {
                                        val p = lockAxis(d, axis)
                                        scroll(p.x / pxPerDp, p.y / pxPerDp)
                                    }
                                }
                            }

                            else -> {
                                val moving = pressed.filter { it.previousPressed }
                                if (moving.isNotEmpty() && !swiped) {
                                    var sx = 0f
                                    var sy = 0f
                                    for (m in moving) {
                                        sx += m.position.x - m.previousPosition.x
                                        sy += m.position.y - m.previousPosition.y
                                    }
                                    val d = Offset(sx / moving.size, sy / moving.size)
                                    multiTravel += d.getDistance()
                                    swipeAccum += d
                                    if (swipeAccum.getDistance() > SWIPE_DISTANCE_DP * pxPerDp) {
                                        swiped = true
                                        swipe(
                                            if (abs(swipeAccum.x) > abs(swipeAccum.y)) {
                                                if (swipeAccum.x > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
                                            } else {
                                                if (swipeAccum.y > 0) SwipeDirection.DOWN else SwipeDirection.UP
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        event.changes.forEach { it.consume() }
                    }

                    // ---- Gesture ended ----
                    touchPoints.value = emptyList()
                    isTouching = false
                    val duration = endTime - start
                    when {
                        dragging -> tapDrag(false)

                        maxPointers == 1 -> {
                            val isTap = travel <= slop && duration < TAP_TIMEOUT_MS
                            if (candidate) {
                                // Second tap in quick succession: a double-click. A long still
                                // hold after a tap just completes the first click.
                                tap(1, if (isTap) 2 else 1)
                            } else if (isTap && currentTapToClick) {
                                if (tapDragEnabled) {
                                    lastTapUpTime = endTime
                                    lastTapPos = endPos
                                    pendingTap = scope.launch {
                                        delay(DOUBLE_TAP_WINDOW_MS)
                                        tap(1, 1)
                                    }
                                } else {
                                    tap(1, 1)
                                }
                            }
                        }

                        maxPointers == 2 -> {
                            if (!isScrolling) {
                                if (currentTapToClick && multiTravel <= slop * 1.5f && duration < MULTI_TAP_TIMEOUT_MS) {
                                    tap(2, 1)
                                }
                            } else if (currentInertia) {
                                val v = velocity.calculateVelocity()
                                val locked = lockAxis(Offset(v.x, v.y), axis)
                                val vx = locked.x / pxPerDp / 1000f
                                val vy = locked.y / pxPerDp / 1000f
                                if (hypot(vx, vy) > FLING_MIN_DP_PER_MS) startInertia(vx, vy)
                            }
                            isScrolling = false
                        }

                        else -> {
                            if (!swiped && currentTapToClick && multiTravel <= slop * 2f && duration < MULTI_TAP_TIMEOUT_MS) {
                                tap(3, 1)
                            }
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusPill(
                visible = isDragLockActive,
                icon = Icons.Rounded.Lock,
                text = "DRAG LOCK",
                color = colors.warning
            )
            StatusPill(
                visible = isTapDragActive && !isDragLockActive,
                icon = Icons.Rounded.PanTool,
                text = "DRAGGING",
                color = colors.warning
            )
            StatusPill(
                visible = isScrolling,
                icon = Icons.Rounded.UnfoldMore,
                text = "SCROLLING",
                color = accent
            )
        }

        AnimatedVisibility(
            visible = !isConnected,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(34.dp))
                Spacer(Modifier.size(8.dp))
                Text("Not connected", color = colors.textSecondary, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Tap the status bar above to pick a computer or TV",
                    color = colors.textMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        AnimatedVisibility(
            visible = !hasTouched,
            enter = fadeIn(),
            exit = fadeOut(tween(600)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = buildString {
                    append("Move with one finger")
                    if (tapToClick) append(" · Tap to click · Two-finger tap to right-click")
                    append("\nTwo fingers to scroll · Three-finger swipe to switch views")
                    if (tapToClick && tapToDrag) append(" · Tap, then slide, to drag")
                },
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun lockAxis(d: Offset, axis: ScrollAxis): Offset = when (axis) {
    ScrollAxis.VERTICAL -> Offset(0f, d.y)
    ScrollAxis.HORIZONTAL -> Offset(d.x, 0f)
    else -> d
}

@Composable
private fun StatusPill(visible: Boolean, icon: ImageVector, text: String, color: Color) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(120)), exit = fadeOut(tween(220))) {
        Surface(
            color = color.copy(alpha = 0.14f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            }
        }
    }
}
