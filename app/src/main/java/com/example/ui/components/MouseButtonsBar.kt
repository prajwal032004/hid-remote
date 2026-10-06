package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme
import kotlin.math.abs

@Composable
fun MouseButtonsBar(
    leftHandedMode: Boolean,
    isDragLockActive: Boolean,
    onSlotPress: (leftSlot: Boolean, pressed: Boolean) -> Unit,
    onMiddleClick: () -> Unit,
    onWheelScroll: (dyDp: Float) -> Unit,
    onToggleDragLock: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 64.dp,
    compact: Boolean = false
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(26.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressButton(
            label = if (leftHandedMode) "Right" else "Left",
            subtitle = if (compact) null else if (leftHandedMode) "Menu" else "Hold to drag",
            highlight = isDragLockActive && !leftHandedMode,
            onPressChange = { onSlotPress(true, it) },
            modifier = Modifier
                .weight(1.3f)
                .height(buttonHeight)
                .testTag("primary_mouse_button")
        )
        ScrollWheelButton(
            onClick = onMiddleClick,
            onScroll = onWheelScroll,
            modifier = Modifier
                .weight(0.6f)
                .height(buttonHeight)
                .testTag("middle_mouse_button")
        )
        PressButton(
            label = if (leftHandedMode) "Left" else "Right",
            subtitle = if (compact) null else if (leftHandedMode) "Hold to drag" else "Menu",
            highlight = isDragLockActive && leftHandedMode,
            onPressChange = { onSlotPress(false, it) },
            modifier = Modifier
                .weight(1.3f)
                .height(buttonHeight)
                .testTag("secondary_mouse_button")
        )
        if (!compact) {
            DragLockButton(
                active = isDragLockActive,
                onToggle = onToggleDragLock,
                modifier = Modifier
                    .height(buttonHeight)
                    .testTag("drag_lock_button")
            )
        }
    }
}

/** Sends button-down on touch and button-up on release, exactly like a physical switch. */
@Composable
private fun PressButton(
    label: String,
    subtitle: String?,
    highlight: Boolean,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val pressChange by rememberUpdatedState(onPressChange)
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, spring(dampingRatio = 0.6f, stiffness = 1400f), label = "press_scale")
    val bg by animateColorAsState(
        if (isPressed) colors.accentSoft else colors.surfaceHigh,
        tween(if (isPressed) 30 else 180),
        label = "press_bg"
    )
    val borderColor by animateColorAsState(
        when {
            isPressed -> colors.accent
            highlight -> colors.warning
            else -> colors.outline
        },
        tween(if (isPressed) 30 else 180),
        label = "press_border"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .semantics {
                role = Role.Button
                contentDescription = "$label mouse button"
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    isPressed = true
                    pressChange(true)
                    try {
                        waitForUpOrCancellation()?.consume()
                    } finally {
                        isPressed = false
                        pressChange(false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = if (isPressed) colors.accent else colors.text,
                style = MaterialTheme.typography.titleSmall
            )
            if (subtitle != null) {
                Text(text = subtitle, color = colors.textMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Tap for a middle click; drag vertically to roll the wheel. */
@Composable
private fun ScrollWheelButton(
    onClick: () -> Unit,
    onScroll: (dyDp: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val click by rememberUpdatedState(onClick)
    val scroll by rememberUpdatedState(onScroll)
    var isPressed by remember { mutableStateOf(false) }
    var ridgeOffset by remember { mutableFloatStateOf(0f) }
    val bg by animateColorAsState(
        if (isPressed) colors.accentSoft else colors.surfaceHigh,
        tween(if (isPressed) 30 else 180),
        label = "wheel_bg"
    )
    val ridgeColor = if (isPressed) colors.accent else colors.textMuted

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, if (isPressed) colors.accent else colors.outline, RoundedCornerShape(20.dp))
            .semantics {
                role = Role.Button
                contentDescription = "Middle button and scroll wheel"
            }
            .drawBehind {
                // A wheel whose ridges roll with the finger; read in draw only, so no recomposition.
                val spacing = 6.dp.toPx()
                val w = 16.dp.toPx()
                val h = 2.dp.toPx()
                val cx = size.width / 2f
                val shift = ((ridgeOffset % spacing) + spacing) % spacing
                var y = shift
                while (y < size.height) {
                    val edgeFade = 1f - abs(y - size.height / 2f) / (size.height / 2f)
                    drawRoundRect(
                        color = ridgeColor.copy(alpha = 0.6f * edgeFade * edgeFade),
                        topLeft = Offset(cx - w / 2f, y - h / 2f),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(h, h)
                    )
                    y += spacing
                }
            }
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    var travel = 0f
                    var scrolling = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                if (!scrolling && change.uptimeMillis - down.uptimeMillis < 400) click()
                                break
                            }
                            val dy = change.position.y - change.previousPosition.y
                            travel += abs(dy)
                            if (!scrolling && travel > slop) scrolling = true
                            if (scrolling && dy != 0f) {
                                ridgeOffset += dy
                                scroll(dy / density)
                            }
                            change.consume()
                        }
                    } finally {
                        isPressed = false
                    }
                }
            }
    )
}

@Composable
private fun DragLockButton(active: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val toggle by rememberUpdatedState(onToggle)
    val tint by animateColorAsState(if (active) colors.warning else colors.textMuted, tween(200), label = "lock_tint")
    val bg by animateColorAsState(if (active) colors.warning.copy(alpha = 0.16f) else colors.surfaceHigh, tween(200), label = "lock_bg")
    Box(
        modifier = modifier
            .width(54.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, if (active) colors.warning.copy(alpha = 0.7f) else colors.outline, RoundedCornerShape(20.dp))
            .semantics {
                role = Role.Switch
                contentDescription = if (active) "Drag lock on" else "Drag lock off"
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    if (waitForUpOrCancellation()?.also { it.consume() } != null) toggle()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (active) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(text = "Drag", color = tint, style = MaterialTheme.typography.labelSmall)
        }
    }
}
