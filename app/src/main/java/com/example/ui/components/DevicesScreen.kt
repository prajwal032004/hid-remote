package com.example.ui.components

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bluetooth.BluetoothDeviceModel
import com.example.bluetooth.DeviceType
import com.example.bluetooth.HidConnectionState
import com.example.model.HostOs
import com.example.ui.theme.AppTheme

fun DeviceType.icon(): ImageVector = when (this) {
    DeviceType.COMPUTER -> Icons.Rounded.Computer
    DeviceType.TV -> Icons.Rounded.Tv
    DeviceType.PHONE -> Icons.Rounded.PhoneAndroid
    DeviceType.AUDIO -> Icons.Rounded.Headphones
    DeviceType.UNKNOWN -> Icons.Rounded.Devices
}

@Composable
fun DevicesScreen(
    pairedDevices: List<BluetoothDeviceModel>,
    connectionState: HidConnectionState,
    hostOs: HostOs,
    lastDeviceAddress: String?,
    onSelectDevice: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onOpenHelp: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val connected = connectionState as? HidConnectionState.Connected
    val connecting = connectionState as? HidConnectionState.Connecting
    val hosts = pairedDevices.filter { it.deviceType != DeviceType.AUDIO }
    val others = pairedDevices.filter { it.deviceType == DeviceType.AUDIO }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("device_selection_screen")
    ) {
        ScreenHeader(
            title = "Devices",
            subtitle = "Connect as a Bluetooth keyboard & mouse",
            onBack = onBack
        ) {
            IconAction(Icons.Rounded.Refresh, "Refresh devices", onRefresh)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (connected != null || connecting != null) {
                    item(key = "current") {
                        CurrentHostCard(connectionState, onDisconnect, Modifier.animateItem())
                    }
                }

                item(key = "pair") { PairNewCard(hostOs, onMakeDiscoverable, Modifier.animateItem()) }

                item(key = "hosts_title") {
                    SectionTitle("Paired computers & TVs", Modifier.padding(top = 8.dp).animateItem())
                }
                if (hosts.isEmpty()) {
                    item(key = "empty") {
                        AppCard(Modifier.fillMaxWidth().animateItem()) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.BluetoothSearching, null, tint = colors.textMuted, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("No paired computers yet", style = MaterialTheme.typography.titleSmall, color = colors.text)
                                Text(
                                    "Follow the steps above to pair your PC, Mac or TV with this phone.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                items(hosts, key = { it.address }) { dev ->
                    DeviceRow(
                        device = dev,
                        isConnected = dev.address == connected?.address,
                        isConnecting = dev.address == connecting?.address,
                        isLastUsed = dev.address == lastDeviceAddress,
                        onClick = { onSelectDevice(dev.device) },
                        modifier = Modifier.animateItem()
                    )
                }
                if (others.isNotEmpty()) {
                    item(key = "others_title") {
                        SectionTitle("Other paired devices", Modifier.padding(top = 8.dp).animateItem())
                    }
                    items(others, key = { it.address }) { dev ->
                        DeviceRow(
                            device = dev,
                            isConnected = false,
                            isConnecting = dev.address == connecting?.address,
                            isLastUsed = false,
                            dimmed = true,
                            onClick = { onSelectDevice(dev.device) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
                item(key = "footer") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp).animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryButton(
                            text = "Bluetooth settings",
                            icon = Icons.Rounded.Bluetooth,
                            tonal = true,
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryButton(
                            text = "Help",
                            icon = Icons.AutoMirrored.Rounded.HelpOutline,
                            tonal = true,
                            onClick = onOpenHelp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentHostCard(state: HidConnectionState, onDisconnect: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val connected = state is HidConnectionState.Connected
    val tint = if (connected) colors.success else colors.warning
    AppCard(modifier.fillMaxWidth(), borderColor = tint.copy(alpha = 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                IconBadge(Icons.Rounded.Computer, tint, size = 48.dp)
                if (!connected) {
                    CircularProgressIndicator(color = tint, strokeWidth = 2.dp, modifier = Modifier.size(56.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (connected) "CONNECTED" else "CONNECTING",
                    style = MaterialTheme.typography.labelSmall,
                    color = tint
                )
                Text(
                    state.title().removePrefix("Connecting to ").removePrefix("Reconnecting to ").removeSuffix("…"),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = if (connected) "Disconnect" else "Cancel",
            icon = Icons.Rounded.LinkOff,
            tonal = true,
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth().testTag("disconnect_button")
        )
    }
}

private fun pairingSteps(os: HostOs): List<String> = when (os) {
    HostOs.WINDOWS -> listOf(
        "Keep this app open and tap Make phone discoverable.",
        "On the PC open Settings › Bluetooth & devices › Add device › Bluetooth.",
        "Pick this phone and confirm the same code on both screens.",
        "Paired this phone with the PC before? Remove it there first, then pair again. Windows only picks up the keyboard and mouse when pairing."
    )
    HostOs.MAC -> listOf(
        "Keep this app open and tap Make phone discoverable.",
        "On the Mac open System Settings › Bluetooth and click Connect next to this phone.",
        "Confirm the code. If Keyboard Setup Assistant opens, follow it and choose ANSI."
    )
    HostOs.LINUX -> listOf(
        "Keep this app open and tap Make phone discoverable.",
        "Open Bluetooth settings, or run bluetoothctl, then scan on and pair the phone.",
        "Confirm the code and mark the device as trusted so it reconnects on its own."
    )
    HostOs.ANDROID -> listOf(
        "Keep this app open and tap Make phone discoverable.",
        "On the TV open Settings › Remotes & Accessories › Pair accessory (on a tablet: Bluetooth › Pair new device).",
        "Select this phone and confirm."
    )
}

@Composable
private fun PairNewCard(hostOs: HostOs, onMakeDiscoverable: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    var os by rememberSaveable { mutableIntStateOf(hostOs.ordinal) }
    AppCard(modifier.fillMaxWidth().animateContentSize(spring(stiffness = 500f))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.BluetoothSearching, colors.accent)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Pair a new computer or TV", style = MaterialTheme.typography.titleMedium, color = colors.text)
                Text(
                    "Pair from the computer, not from this phone",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        SegmentedControl(
            options = HostOs.entries.map { it.shortLabel },
            selectedIndex = os,
            onSelect = { os = it },
            height = 36.dp
        )
        Spacer(Modifier.height(12.dp))
        AnimatedContent(
            targetState = HostOs.entries[os],
            transitionSpec = { fadeIn(tween(220, 60)) togetherWith fadeOut(tween(120)) },
            label = "steps"
        ) { target ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pairingSteps(target).forEachIndexed { i, step -> StepRow(i + 1, step) }
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = "Make phone discoverable",
            icon = Icons.Rounded.Bluetooth,
            onClick = onMakeDiscoverable,
            modifier = Modifier.fillMaxWidth().testTag("discoverable_button")
        )
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    val colors = AppTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", style = MaterialTheme.typography.labelMedium, color = colors.accent)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DeviceRow(
    device: BluetoothDeviceModel,
    isConnected: Boolean,
    isConnecting: Boolean,
    isLastUsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false
) {
    val colors = AppTheme.colors
    val tint = when {
        isConnected -> colors.success
        dimmed -> colors.textMuted
        else -> colors.accent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isConnected) colors.success.copy(alpha = 0.08f) else colors.surface)
            .border(1.dp, if (isConnected) colors.success.copy(alpha = 0.5f) else colors.outline, RoundedCornerShape(20.dp))
            .bounceClick(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(device.deviceType.icon(), tint, size = 42.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                device.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (dimmed) colors.textSecondary else colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                when {
                    dimmed -> "Audio device · usually can't be controlled"
                    isLastUsed -> "Last used · ${device.address}"
                    else -> device.address
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        when {
            isConnected -> TagLabel("ACTIVE", colors.success)
            isConnecting -> CircularProgressIndicator(color = colors.warning, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            else -> Text(
                "Connect",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tint.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------
// Help & troubleshooting
// ----------------------------------------------------------------------

private data class HelpTopic(val question: String, val answer: String)

private val HELP_TOPICS = listOf(
    HelpTopic(
        "Windows pairs, but the mouse and keyboard do nothing",
        "Windows reads the keyboard and mouse profile only while pairing. If the phone was paired before this app was open, " +
            "Windows treats it as a phone. On the PC open Bluetooth & devices, remove this phone, then in this app tap " +
            "Devices › Make phone discoverable and add it again from the PC. The first connection can take up to 30 seconds while Windows installs drivers."
    ),
    HelpTopic(
        "It disconnects on its own",
        "When you leave the app, the connection is released after one minute so nothing runs in the background. " +
            "Open the app and it reconnects automatically (Settings › Auto-reconnect). To stay connected while using other apps, turn on " +
            "Settings › Stay connected in background. Also keep Keep screen on enabled during long sessions, and keep the phone within about 10 m of the host."
    ),
    HelpTopic(
        "My phone's own Bluetooth keyboard, mouse or controller stopped working",
        "While this app is acting as a keyboard, Android pauses the phone's own Bluetooth keyboards, mice and game controllers. " +
            "Headphones, watches and cars are not affected. Leave the app and they work again within a minute. Turning off " +
            "Stay connected in background makes sure this always happens."
    ),
    HelpTopic(
        "Wrong characters are typed",
        "The app types like a US English keyboard. Set the computer's keyboard layout to English (US), or use the keys " +
            "directly instead of Direct Type for symbols."
    ),
    HelpTopic(
        "Shortcuts do the wrong thing",
        "Pick the right system in Settings › Host system. Copy, paste, switch app, swipes and the media remote all adapt to it."
    ),
    HelpTopic(
        "The phone says it isn't supported",
        "The Bluetooth HID Device profile has been part of Android since version 9, but a few manufacturers switch it off in firmware. " +
            "There is no workaround on those phones."
    ),
    HelpTopic(
        "Connecting fails after changing the app",
        "Hosts remember the keyboard layout from when they paired. After an update that changes it, remove the phone on the host and pair again."
    )
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        ScreenHeader(title = "Help", subtitle = "Pairing and troubleshooting", onBack = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(HELP_TOPICS, key = { it.question }) { topic -> HelpCard(topic) }
            }
        }
    }
}

@Composable
private fun HelpCard(topic: HelpTopic) {
    val colors = AppTheme.colors
    var expanded by rememberSaveable(topic.question) { mutableStateOf(false) }
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        if (expanded) 180f else 0f,
        spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "chevron"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, if (expanded) colors.accent.copy(alpha = 0.4f) else colors.outline, RoundedCornerShape(20.dp))
            .bounceClick(pressedScale = 0.99f) { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(topic.question, style = MaterialTheme.typography.titleSmall, color = colors.text, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, null, tint = colors.textMuted, modifier = Modifier.rotate(rotation))
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200, 60)) + expandVertically(spring(stiffness = 600f)),
            exit = fadeOut(tween(120)) + shrinkVertically(spring(stiffness = 700f))
        ) {
            Text(
                topic.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
