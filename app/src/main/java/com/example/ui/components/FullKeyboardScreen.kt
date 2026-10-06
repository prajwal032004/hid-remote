package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.bluetooth.HidConnectionState
import com.example.hid.CharToHidMapper
import com.example.hid.HidKeyCodes
import com.example.model.HostOs
import com.example.model.altKeyLabel
import com.example.model.superKeyLabel
import com.example.ui.theme.AppTheme
import com.example.viewmodel.ModifierState

private const val SHIFT_BIT = 0x02
private val ROW_GAP = 5.dp

/** Pairs of (base, shifted) for the non-letter character keys. */
private val SHIFTED = mapOf(
    '`' to '~', '1' to '!', '2' to '@', '3' to '#', '4' to '$', '5' to '%', '6' to '^', '7' to '&',
    '8' to '*', '9' to '(', '0' to ')', '-' to '_', '=' to '+', '[' to '{', ']' to '}', '\\' to '|',
    ';' to ':', '\'' to '"', ',' to '<', '.' to '>', '/' to '?'
)

/**
 * A full-size PC keyboard that fills the screen, built for landscape. Modifier keys are sticky
 * (tap = next key only, long-press = lock), so shortcuts like Ctrl+Shift+Esc work one-handed.
 */
@Composable
fun FullKeyboardScreen(
    connectionState: HidConnectionState,
    modifierState: ModifierState,
    capsLockOn: Boolean,
    hostOs: HostOs,
    showTouchpad: Boolean,
    onToggleTouchpad: () -> Unit,
    onToggleModifier: (Byte) -> Unit,
    onLockModifier: (Byte) -> Unit,
    onSendKey: (modifier: Byte, keyCode: Byte) -> Unit,
    onClose: () -> Unit,
    touchpad: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        val canSplit = maxWidth >= 640.dp
        val split = showTouchpad && canSplit
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconAction(Icons.Rounded.CloseFullscreen, "Close full keyboard", onClose, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                ConnectionPill(connectionState, Modifier.weight(1f))
                if (capsLockOn) TagLabel("CAPS", colors.success, Modifier.padding(horizontal = 8.dp))
                if (canSplit) {
                    IconAction(Icons.Rounded.VerticalSplit, "Show touchpad", onToggleTouchpad, active = showTouchpad, size = 38.dp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.weight(1f).fillMaxWidth()) {
                FullKeyboard(
                    modifierState = modifierState,
                    capsLockOn = capsLockOn,
                    hostOs = hostOs,
                    onToggleModifier = onToggleModifier,
                    onLockModifier = onLockModifier,
                    onSendKey = onSendKey,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                AnimatedVisibility(
                    visible = split,
                    enter = fadeIn(tween(220)) + expandHorizontally(tween(280)),
                    exit = fadeOut(tween(160)) + shrinkHorizontally(tween(240))
                ) {
                    Row(Modifier.fillMaxHeight()) {
                        Spacer(Modifier.width(8.dp))
                        touchpad(Modifier.width(this@BoxWithConstraints.maxWidth * 0.3f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

/** Compact connection indicator for dense headers. */
@Composable
fun ConnectionPill(state: HidConnectionState, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val (label, color) = when (state) {
        is HidConnectionState.Connected -> state.deviceName to colors.success
        is HidConnectionState.Connecting -> "Connecting to ${state.deviceName}…" to colors.warning
        HidConnectionState.ReadyToConnect -> "Not connected" to colors.info
        else -> "Bluetooth unavailable" to colors.danger
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StatusDot(color, pulsing = state is HidConnectionState.Connecting, size = 8.dp)
        Spacer(Modifier.width(4.dp))
        Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FullKeyboard(
    modifierState: ModifierState,
    capsLockOn: Boolean,
    hostOs: HostOs,
    onToggleModifier: (Byte) -> Unit,
    onLockModifier: (Byte) -> Unit,
    onSendKey: (Byte, Byte) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(24.dp))
            .padding(8.dp)
    ) {
        // Six rows; the function row is a little shorter, like on a laptop.
        val rowUnits = 5.78f
        val rowHeight = (maxHeight - ROW_GAP * 5) / rowUnits
        val keyWidth = maxWidth / 15f
        val font = (min(rowHeight * 0.34f, keyWidth * 0.40f).value).coerceIn(11f, 20f).sp
        val small = font * 0.78f

        val shiftActive = modifierState.active and SHIFT_BIT != 0
        val upper = shiftActive != capsLockOn

        fun key(code: Byte): () -> Unit = { onSendKey(0, code) }
        fun char(c: Char): () -> Unit = {
            CharToHidMapper.mapChar(c)?.let { onSendKey(it.modifier, it.keyCode) }
        }
        fun modStyle(bit: Byte): KeyStyle {
            val m = bit.toInt() and 0xFF
            return when {
                modifierState.locked and m != 0 -> KeyStyle.LOCKED
                modifierState.oneShot and m != 0 -> KeyStyle.ONE_SHOT
                else -> KeyStyle.ACTION
            }
        }

        @Composable
        fun RowScope.Sym(c: Char, weight: Float = 1f) {
            val shifted = SHIFTED[c]
            TextKey(
                label = if (shiftActive && shifted != null) shifted.toString() else c.toString(),
                hint = if (!shiftActive) shifted?.toString() else null,
                modifier = Modifier.weight(weight),
                fontSize = font,
                onPress = char(c)
            )
        }

        @Composable
        fun RowScope.Letter(c: Char) {
            TextKey(if (upper) c.uppercaseChar().toString() else c.toString(), Modifier.weight(1f), fontSize = font, onPress = char(c))
        }

        @Composable
        fun RowScope.Mod(label: String, bit: Byte, weight: Float) {
            TextKey(
                label,
                Modifier.weight(weight),
                style = modStyle(bit),
                fontSize = small,
                onLongPress = { onLockModifier(bit) },
                onPress = { onToggleModifier(bit) }
            )
        }

        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
            KbRow(rowHeight * 0.78f) {
                TextKey("Esc", Modifier.weight(1f), style = KeyStyle.ACTION, fontSize = small, onPress = key(HidKeyCodes.KEY_ESCAPE))
                Spacer(Modifier.weight(0.5f))
                val fKeys = listOf(
                    HidKeyCodes.KEY_F1, HidKeyCodes.KEY_F2, HidKeyCodes.KEY_F3, HidKeyCodes.KEY_F4,
                    HidKeyCodes.KEY_F5, HidKeyCodes.KEY_F6, HidKeyCodes.KEY_F7, HidKeyCodes.KEY_F8,
                    HidKeyCodes.KEY_F9, HidKeyCodes.KEY_F10, HidKeyCodes.KEY_F11, HidKeyCodes.KEY_F12
                )
                fKeys.forEachIndexed { i, code ->
                    TextKey("F${i + 1}", Modifier.weight(1f), style = KeyStyle.ACTION, fontSize = small * 0.9f, onPress = key(code))
                }
                TextKey("Del", Modifier.weight(1.5f), style = KeyStyle.ACTION, fontSize = small, repeatable = true, onPress = key(HidKeyCodes.KEY_DELETE))
            }
            KbRow(rowHeight) {
                "`1234567890-=".forEach { Sym(it) }
                IconKey(Icons.AutoMirrored.Rounded.Backspace, "Backspace", Modifier.weight(2f), repeatable = true, onPress = key(HidKeyCodes.KEY_BACKSPACE))
            }
            KbRow(rowHeight) {
                TextKey("Tab", Modifier.weight(1.5f), style = KeyStyle.ACTION, fontSize = small, onPress = key(HidKeyCodes.KEY_TAB))
                "qwertyuiop".forEach { Letter(it) }
                Sym('[')
                Sym(']')
                Sym('\\', 1.5f)
            }
            KbRow(rowHeight) {
                TextKey(
                    "Caps",
                    Modifier.weight(1.75f),
                    style = if (capsLockOn) KeyStyle.ONE_SHOT else KeyStyle.ACTION,
                    fontSize = small,
                    onPress = key(HidKeyCodes.KEY_CAPSLOCK)
                )
                "asdfghjkl".forEach { Letter(it) }
                Sym(';')
                Sym('\'')
                IconKey(Icons.AutoMirrored.Rounded.KeyboardReturn, "Enter", Modifier.weight(2.25f), style = KeyStyle.ACCENT, onPress = key(HidKeyCodes.KEY_ENTER))
            }
            KbRow(rowHeight) {
                Mod("Shift", HidKeyCodes.MOD_LEFT_SHIFT, 2.25f)
                "zxcvbnm".forEach { Letter(it) }
                Sym(',')
                Sym('.')
                Sym('/')
                Mod("Shift", HidKeyCodes.MOD_LEFT_SHIFT, 1.75f)
                IconKey(Icons.Rounded.ArrowUpward, "Up", Modifier.weight(1f), iconSize = 18.dp, repeatable = true, onPress = key(HidKeyCodes.KEY_UP))
            }
            KbRow(rowHeight) {
                Mod("Ctrl", HidKeyCodes.MOD_LEFT_CTRL, 1.25f)
                Mod(hostOs.superKeyLabel.lowercase().replaceFirstChar { it.uppercase() }, HidKeyCodes.MOD_LEFT_GUI, 1.25f)
                Mod(hostOs.altKeyLabel.lowercase().replaceFirstChar { it.uppercase() }, HidKeyCodes.MOD_LEFT_ALT, 1.25f)
                TextKey("space", Modifier.weight(6.25f), fontSize = small, repeatable = true, onPress = key(HidKeyCodes.KEY_SPACE))
                Mod(hostOs.altKeyLabel.lowercase().replaceFirstChar { it.uppercase() }, HidKeyCodes.MOD_LEFT_ALT, 1f)
                Mod("Ctrl", HidKeyCodes.MOD_LEFT_CTRL, 1f)
                IconKey(Icons.AutoMirrored.Rounded.ArrowBack, "Left", Modifier.weight(1f), iconSize = 18.dp, repeatable = true, onPress = key(HidKeyCodes.KEY_LEFT))
                IconKey(Icons.Rounded.ArrowDownward, "Down", Modifier.weight(1f), iconSize = 18.dp, repeatable = true, onPress = key(HidKeyCodes.KEY_DOWN))
                IconKey(Icons.AutoMirrored.Rounded.ArrowForward, "Right", Modifier.weight(1f), iconSize = 18.dp, repeatable = true, onPress = key(HidKeyCodes.KEY_RIGHT))
            }
        }
    }
}

@Composable
private fun KbRow(height: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
        content = content
    )
}
