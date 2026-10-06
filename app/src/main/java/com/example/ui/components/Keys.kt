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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class KeyStyle { NORMAL, ACTION, ACCENT, ONE_SHOT, LOCKED }

private const val REPEAT_DELAY_MS = 400L
private const val REPEAT_INTERVAL_MS = 45L
private const val LONG_PRESS_MS = 450L

private fun AppColors.keyBackground(style: KeyStyle): Color = when (style) {
    KeyStyle.NORMAL -> surfaceHigh
    KeyStyle.ACTION -> surfaceHigher
    KeyStyle.ACCENT, KeyStyle.LOCKED -> accent
    KeyStyle.ONE_SHOT -> accentSoft
}

private fun AppColors.keyContent(style: KeyStyle): Color = when (style) {
    KeyStyle.NORMAL -> text
    KeyStyle.ACTION -> textSecondary
    KeyStyle.ACCENT, KeyStyle.LOCKED -> onAccent
    KeyStyle.ONE_SHOT -> accent
}

/**
 * A keyboard key. It fires on touch-down (no release lag, like a hardware key) and, when
 * [repeatable], repeats while held: 400 ms delay, then ~22 per second. Keys with [onLongPress]
 * fire on release instead so the two can be told apart. Every key tracks its own pointer, so
 * several fingers can type at once.
 */
@Composable
fun KeyButton(
    modifier: Modifier = Modifier,
    style: KeyStyle = KeyStyle.NORMAL,
    repeatable: Boolean = false,
    description: String? = null,
    cornerRadius: Dp = 10.dp,
    onLongPress: (() -> Unit)? = null,
    onPress: () -> Unit,
    content: @Composable BoxScope.(Color) -> Unit
) {
    val colors = AppTheme.colors
    val press by rememberUpdatedState(onPress)
    val longPress by rememberUpdatedState(onLongPress)
    val hasLongPress = onLongPress != null
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 1600f),
        label = "key_scale"
    )
    val base = colors.keyBackground(style)
    val bg by animateColorAsState(
        targetValue = if (pressed) {
            if (style == KeyStyle.ACCENT || style == KeyStyle.LOCKED) base.copy(alpha = 0.75f) else colors.accentSoft
        } else base,
        animationSpec = tween(if (pressed) 30 else 200),
        label = "key_bg"
    )
    val borderColor = when {
        pressed -> colors.accent.copy(alpha = 0.7f)
        style == KeyStyle.ONE_SHOT -> colors.accent.copy(alpha = 0.7f)
        style == KeyStyle.ACCENT || style == KeyStyle.LOCKED -> Color.Transparent
        else -> colors.outline
    }
    val contentColor = if (pressed && style != KeyStyle.ACCENT && style != KeyStyle.LOCKED) colors.accent else colors.keyContent(style)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .semantics {
                role = Role.Button
                if (description != null) contentDescription = description
            }
            .pointerInput(repeatable, hasLongPress) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false).consume()
                        pressed = true
                        if (hasLongPress) {
                            var longFired = false
                            val timer = launch {
                                delay(LONG_PRESS_MS)
                                longFired = true
                                longPress?.invoke()
                            }
                            try {
                                val up = waitForUpOrCancellation()
                                up?.consume()
                                timer.cancel()
                                if (up != null && !longFired) press()
                            } finally {
                                timer.cancel()
                                pressed = false
                            }
                        } else {
                            press()
                            val repeater = if (repeatable) {
                                launch {
                                    delay(REPEAT_DELAY_MS)
                                    while (true) {
                                        press()
                                        delay(REPEAT_INTERVAL_MS)
                                    }
                                }
                            } else null
                            try {
                                waitForUpOrCancellation()?.consume()
                            } finally {
                                repeater?.cancel()
                                pressed = false
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content(contentColor)
    }
}

@Composable
fun TextKey(
    label: String,
    modifier: Modifier = Modifier,
    style: KeyStyle = KeyStyle.NORMAL,
    fontSize: TextUnit = 15.sp,
    hint: String? = null,
    repeatable: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onPress: () -> Unit
) {
    KeyButton(
        modifier = modifier.fillMaxHeight(),
        style = style,
        repeatable = repeatable,
        description = label,
        onLongPress = onLongPress,
        onPress = onPress
    ) { color ->
        if (hint != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(hint, color = color.copy(alpha = 0.55f), fontSize = fontSize * 0.62f, maxLines = 1, lineHeight = fontSize * 0.7f)
                Text(label, color = color, fontSize = fontSize, fontWeight = FontWeight.Medium, maxLines = 1, lineHeight = fontSize * 1.05f)
            }
        } else {
            Text(
                label,
                color = color,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center,
                softWrap = false
            )
        }
    }
}

@Composable
fun IconKey(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    style: KeyStyle = KeyStyle.ACTION,
    iconSize: Dp = 20.dp,
    repeatable: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onPress: () -> Unit
) {
    KeyButton(
        modifier = modifier.fillMaxHeight(),
        style = style,
        repeatable = repeatable,
        description = description,
        onLongPress = onLongPress,
        onPress = onPress
    ) { color ->
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
    }
}
