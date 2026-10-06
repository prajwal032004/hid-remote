package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bluetooth.HidConnectionState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppTheme

fun AppColors.statusColor(state: HidConnectionState): Color = when (state) {
    is HidConnectionState.Connected -> success
    is HidConnectionState.Connecting, HidConnectionState.Disconnecting -> warning
    HidConnectionState.ReadyToConnect -> info
    HidConnectionState.Registering, HidConnectionState.Initializing -> accent
    HidConnectionState.BluetoothOff, HidConnectionState.PermissionsRequired -> warning
    is HidConnectionState.NotSupported, is HidConnectionState.Error -> danger
}

fun HidConnectionState.title(): String = when (this) {
    is HidConnectionState.Connected -> deviceName
    is HidConnectionState.Connecting -> if (attempt > 0) "Reconnecting to $deviceName…" else "Connecting to $deviceName…"
    HidConnectionState.ReadyToConnect -> "Not connected"
    HidConnectionState.Registering -> "Preparing keyboard & mouse…"
    HidConnectionState.Initializing -> "Starting Bluetooth…"
    HidConnectionState.BluetoothOff -> "Bluetooth is off"
    HidConnectionState.PermissionsRequired -> "Permission needed"
    is HidConnectionState.NotSupported -> "Not supported"
    is HidConnectionState.Error -> "Connection problem"
    HidConnectionState.Disconnecting -> "Disconnecting…"
}

fun HidConnectionState.subtitle(): String = when (this) {
    is HidConnectionState.Connected -> "Connected · tap to manage"
    HidConnectionState.ReadyToConnect -> "Tap to choose a computer or TV"
    HidConnectionState.BluetoothOff -> "Tap to turn on"
    HidConnectionState.PermissionsRequired -> "Tap to allow Nearby devices"
    is HidConnectionState.Error -> "Tap to retry"
    is HidConnectionState.Connecting -> "Keep the host awake and nearby"
    else -> "Bluetooth keyboard & mouse"
}

@Composable
fun ConnectionStatusBar(
    connectionState: HidConnectionState,
    isKeyboardOpen: Boolean,
    onToggleKeyboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDevicePicker: () -> Unit,
    onOpenMedia: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = AppTheme.colors
    val statusColor by animateColorAsState(colors.statusColor(connectionState), tween(300), label = "status_color")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = if (compact) 44.dp else 52.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.surface)
                .border(1.dp, colors.outline, RoundedCornerShape(50))
                .bounceClick(pressedScale = 0.98f) {
                    when (connectionState) {
                        HidConnectionState.BluetoothOff,
                        HidConnectionState.PermissionsRequired,
                        is HidConnectionState.Error -> onRetry()
                        else -> onOpenDevicePicker()
                    }
                }
                .padding(start = 10.dp, end = 8.dp)
                .testTag("status_chip"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(
                statusColor,
                pulsing = connectionState is HidConnectionState.Connecting || connectionState == HidConnectionState.Registering
            )
            Spacer(Modifier.width(6.dp))
            AnimatedContent(
                targetState = connectionState.title() to connectionState.subtitle(),
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 3 }) togetherWith
                        (fadeOut(tween(140)) + slideOutVertically(tween(200)) { -it / 3 })
                },
                modifier = Modifier.weight(1f),
                label = "status_text"
            ) { (title, subtitle) ->
                Column {
                    Text(
                        title,
                        color = colors.text,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!compact) {
                        Text(
                            subtitle,
                            color = colors.textMuted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconAction(
                Icons.Rounded.SettingsRemote,
                "Media and TV remote",
                onOpenMedia,
                modifier = Modifier.testTag("open_media_button")
            )
            IconAction(
                Icons.Rounded.Keyboard,
                "Toggle keyboard",
                onToggleKeyboard,
                active = isKeyboardOpen,
                modifier = Modifier.testTag("toggle_keyboard_button")
            )
            IconAction(
                Icons.Rounded.Settings,
                "Settings",
                onOpenSettings,
                modifier = Modifier.testTag("open_settings_button")
            )
        }
    }
}
