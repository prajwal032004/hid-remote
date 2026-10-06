package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.bluetooth.HidConnectionState
import com.example.ui.theme.AppTheme

@Composable
fun CompatibilityWarningCard(
    state: HidConnectionState,
    onRequestEnableBluetooth: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    when (state) {
        HidConnectionState.BluetoothOff -> WarningCard(
            icon = Icons.Rounded.BluetoothDisabled,
            tint = colors.warning,
            title = "Bluetooth is off",
            body = "Turn on Bluetooth so this phone can act as a keyboard and mouse.",
            action = "Turn on Bluetooth",
            onAction = onRequestEnableBluetooth,
            modifier = modifier.testTag("bluetooth_off_warning")
        )
        HidConnectionState.PermissionsRequired -> WarningCard(
            icon = Icons.Rounded.Security,
            tint = colors.warning,
            title = "Allow Nearby devices",
            body = "Android needs the Nearby devices permission to connect to your computer or TV over Bluetooth.",
            action = "Allow",
            onAction = onRequestPermissions,
            modifier = modifier.testTag("permissions_warning")
        )
        is HidConnectionState.NotSupported -> WarningCard(
            icon = Icons.Rounded.Warning,
            tint = colors.danger,
            title = "This phone can't act as a Bluetooth keyboard",
            body = state.reason + " Pixel, Samsung Galaxy, Motorola, OnePlus and most recent phones support it.",
            action = "Check again",
            retry = true,
            onAction = onRetry,
            modifier = modifier.testTag("unsupported_warning")
        )
        is HidConnectionState.Error -> WarningCard(
            icon = Icons.Rounded.ErrorOutline,
            tint = colors.danger,
            title = "Bluetooth keyboard service stopped",
            body = state.message,
            action = "Retry",
            retry = true,
            onAction = onRetry,
            modifier = modifier.testTag("error_warning")
        )
        else -> Unit
    }
}

@Composable
private fun WarningCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    retry: Boolean = false
) {
    val colors = AppTheme.colors
    AppCard(modifier = modifier.fillMaxWidth(), borderColor = tint.copy(alpha = 0.4f)) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(icon, tint)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text)
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = action,
            onClick = onAction,
            icon = if (retry) Icons.Rounded.Refresh else null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
