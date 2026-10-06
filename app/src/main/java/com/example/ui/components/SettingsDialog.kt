package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import android.content.Intent
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.model.AccentColor
import com.example.model.AppInfo
import com.example.model.HostOs
import com.example.model.UserSettings
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

private val ACCELERATION_PRESETS = listOf("Off" to 1.0f, "Low" to 1.25f, "Medium" to 1.5f, "High" to 1.8f)

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onUpdate: ((UserSettings) -> UserSettings) -> Unit,
    onReset: () -> Unit,
    onOpenHelp: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("settings_screen")
    ) {
        ScreenHeader(title = "Settings", subtitle = "Changes apply instantly", onBack = onBack)
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val twoColumns = maxWidth >= 840.dp
            val scroll = rememberScrollState()
            Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll),
                contentAlignment = Alignment.TopCenter
            ) {
                if (twoColumns) {
                    Row(
                        Modifier.widthIn(max = 1200.dp).padding(horizontal = 16.dp).padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            HeroCard(settings)
                            HostSection(settings, onUpdate)
                            PointerSection(settings, onUpdate)
                            ScrollSection(settings, onUpdate)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            GestureSection(settings, onUpdate)
                            KeyboardSection(settings, onUpdate)
                            ConnectionSection(settings, onUpdate, onOpenHelp)
                            AppearanceSection(settings, onUpdate)
                            GeneralSection(settings, onUpdate, onReset)
                            AboutCard()
                        }
                    }
                } else {
                    Column(
                        Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeroCard(settings)
                        HostSection(settings, onUpdate)
                        PointerSection(settings, onUpdate)
                        ScrollSection(settings, onUpdate)
                        GestureSection(settings, onUpdate)
                        KeyboardSection(settings, onUpdate)
                        ConnectionSection(settings, onUpdate, onOpenHelp)
                        AppearanceSection(settings, onUpdate)
                        GeneralSection(settings, onUpdate, onReset)
                        AboutCard()
                    }
                }
            }
        }
    }
}

@Composable
fun AppLogo(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 64.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
    ) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize().graphicsLayer { scaleX = 1.45f; scaleY = 1.45f })
    }
}

@Composable
private fun HeroCard(settings: UserSettings) {
    val colors = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(colors.accent.copy(alpha = 0.22f), colors.surface)))
            .border(1.dp, colors.accent.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLogo()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("HID Remote", style = MaterialTheme.typography.headlineSmall, color = colors.text)
            Text(
                "Version ${BuildConfig.VERSION_NAME} · ${settings.hostOs.label}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "by ${AppInfo.AUTHOR} · @${AppInfo.GITHUB_USER}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionTitle(title)
        AppCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp), content = content)
    }
}

@Composable
private fun HostSection(settings: UserSettings, onUpdate: ((UserSettings) -> UserSettings) -> Unit) {
    val colors = AppTheme.colors
    SettingsCard("Host system") {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Computer, colors.accent, size = 38.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Connected computer runs", style = MaterialTheme.typography.titleSmall, color = colors.text)
                Text("Shortcuts, swipes and the remote adapt to it", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
        SegmentedControl(
            options = HostOs.entries.map { it.shortLabel },
            selectedIndex = settings.hostOs.ordinal,
            onSelect = { i -> onUpdate { it.copy(hostOs = HostOs.entries[i]) } },
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PointerSection(settings: UserSettings, onUpdate: ((UserSettings) -> UserSettings) -> Unit) {
    val colors = AppTheme.colors
    var sensitivity by remember(settings.mouseSensitivity) { mutableFloatStateOf(settings.mouseSensitivity) }
    SettingsCard("Pointer") {
        SettingSliderRow(
            icon = Icons.Rounded.Speed,
            title = "Pointer speed",
            valueLabel = String.format(Locale.US, "%.1f×", sensitivity),
            value = sensitivity,
            valueRange = 0.5f..3.0f,
            steps = 24,
            onValueChange = { sensitivity = it },
            onValueChangeFinished = { onUpdate { it.copy(mouseSensitivity = sensitivity) } }
        )
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Mouse, colors.accent, size = 38.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Acceleration", style = MaterialTheme.typography.titleSmall, color = colors.text)
                Text("Fast flicks travel further; slow moves stay precise", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
        val selected = ACCELERATION_PRESETS.indexOfFirst { abs(it.second - settings.accelerationCurve) < 0.1f }.coerceAtLeast(0)
        SegmentedControl(
            options = ACCELERATION_PRESETS.map { it.first },
            selectedIndex = selected,
            onSelect = { i -> onUpdate { it.copy(accelerationCurve = ACCELERATION_PRESETS[i].second) } },
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        var airSpeed by remember(settings.airMouseSensitivity) { mutableFloatStateOf(settings.airMouseSensitivity) }
        SettingSliderRow(
            icon = Icons.Rounded.Sensors,
            title = "Air mouse speed",
            valueLabel = String.format(Locale.US, "%.1f×", airSpeed),
            value = airSpeed,
            valueRange = 0.3f..3.0f,
            steps = 26,
            onValueChange = { airSpeed = it },
            onValueChangeFinished = { onUpdate { it.copy(airMouseSensitivity = airSpeed) } }
        )
        SettingSwitchRow(
            Icons.Rounded.SwapHoriz, "Left-handed buttons", "Swap left and right click",
            settings.leftHandedMode, { v -> onUpdate { it.copy(leftHandedMode = v) } }
        )
        SettingSwitchRow(
            Icons.Rounded.TouchApp, "Show mouse buttons", "Physical-style buttons under the touchpad",
            settings.showMouseButtons, { v -> onUpdate { it.copy(showMouseButtons = v) } }
        )
    }
}

@Composable
private fun ScrollSection(settings: UserSettings, onUpdate: ((UserSettings) -> UserSettings) -> Unit) {
    var scroll by remember(settings.scrollSensitivity) { mutableFloatStateOf(settings.scrollSensitivity) }
    SettingsCard("Scrolling") {
        SettingSliderRow(
            icon = Icons.Rounded.UnfoldMore,
            title = "Scroll speed",
            valueLabel = String.format(Locale.US, "%.1f×", scroll),
            value = scroll,
            valueRange = 0.5f..3.0f,
            steps = 24,
            onValueChange = { scroll = it },
            onValueChangeFinished = { onUpdate { it.copy(scrollSensitivity = scroll) } }
        )
        SettingSwitchRow(
            Icons.Rounded.Swipe, "Natural scrolling", "Content follows your fingers, like on a phone",
            settings.naturalScroll, { v -> onUpdate { it.copy(naturalScroll = v) } }
        )
        SettingSwitchRow(
            Icons.Rounded.Gesture, "Scroll momentum", "A two-finger flick glides to a stop",
            settings.scrollInertia, { v -> onUpdate { it.copy(scrollInertia = v) } }
        )
    }
}

@Composable
private fun GestureSection(settings: UserSettings, onUpdate: ((UserSettings) -> UserSettings) -> Unit) {
    SettingsCard("Gestures") {
        SettingSwitchRow(
            Icons.Rounded.TouchApp, "Tap to click", "1 finger = left, 2 = right, 3 = middle click",
            settings.tapToClick, { v -> onUpdate { it.copy(tapToClick = v) } }
        )
        SettingSwitchRow(
            Icons.Rounded.Gesture, "Tap and drag", "Tap, then touch again and slide to drag",
            settings.tapToDrag && settings.tapToClick, { v -> onUpdate { it.copy(tapToDrag = v) } },
            enabled = settings.tapToClick
        )
    }
}

@Composable
private fun KeyboardSection(settings: UserSettings, onUpdate: ((UserSettings) -> UserSettings) -> Unit) {
    SettingsCard("Full keyboard") {
        SettingSwitchRow(
            Icons.Rounded.ScreenRotation, "Open in landscape", "Rotate phones sideways for the full-size keyboard",
            settings.landscapeFullKeyboard, { v -> onUpdate { it.copy(landscapeFullKeyboard = v) } }
        )
        SettingSwitchRow(
            Icons.Rounded.VerticalSplit, "Touchpad beside keys", "When the screen is wide enough",
            settings.fullKeyboardTouchpad, { v -> onUpdate { it.copy(fullKeyboardTouchpad = v) } }
        )
    }
}

@Composable
private fun ConnectionSection(
    settings: UserSettings,
    onUpdate: ((UserSettings) -> UserSettings) -> Unit,
    onOpenHelp: () -> Unit
) {
    val colors = AppTheme.colors
    SettingsCard("Connection") {
        SettingSwitchRow(
            Icons.Rounded.Autorenew, "Auto-reconnect", "Reconnect to the last computer when the app opens",
            settings.autoReconnect, { v -> onUpdate { it.copy(autoReconnect = v) } }
        )
        SettingSwitchRow(
            Icons.Rounded.PowerSettingsNew, "Stay connected in background",
            if (settings.stayConnectedInBackground) "Keeps the host connected while you use other apps"
            else "Off: Bluetooth is released 1 min after you leave, so nothing runs in the background",
            settings.stayConnectedInBackground, { v -> onUpdate { it.copy(stayConnectedInBackground = v) } }
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .bounceClick(pressedScale = 0.98f, onClick = onOpenHelp)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(Icons.AutoMirrored.Rounded.HelpOutline, colors.textSecondary, size = 38.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Help & troubleshooting", style = MaterialTheme.typography.titleSmall, color = colors.text)
                Text("Windows pairing, disconnects, wrong characters", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.textMuted)
        }
    }
}

@Composable
private fun AppearanceSection(settings: UserSettings, onUpdate: ((UserSettings) -> UserSettings) -> Unit) {
    val colors = AppTheme.colors
    SettingsCard("Appearance") {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Palette, colors.accent, size = 38.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Accent colour", style = MaterialTheme.typography.titleSmall, color = colors.text)
                Text(settings.accent.label, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AccentColor.entries.forEach { accent ->
                AccentSwatch(accent, selected = accent == settings.accent) { onUpdate { it.copy(accent = accent) } }
            }
        }
        SettingSwitchRow(
            if (settings.amoledBlack) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
            "Pure black background", "Deeper blacks and lower power use on OLED screens",
            settings.amoledBlack, { v -> onUpdate { it.copy(amoledBlack = v) } }
        )
    }
}

@Composable
private fun AccentSwatch(accent: AccentColor, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val color = Color(accent.argb)
    val ring by animateColorAsState(if (selected) color else Color.Transparent, tween(220), label = "ring")
    val inner by animateFloatAsState(if (selected) 0.72f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "swatch")
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(2.dp, ring, CircleShape)
            .bounceClick(role = Role.RadioButton, pressedScale = 0.88f, onClick = onClick)
            .semantics { contentDescription = "${accent.label} accent" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(44.dp)
                .graphicsLayer { scaleX = inner; scaleY = inner }
                .clip(CircleShape)
                .background(color)
        )
        AnimatedVisibility(selected, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Icon(Icons.Rounded.Check, null, tint = colors.onAccent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun GeneralSection(
    settings: UserSettings,
    onUpdate: ((UserSettings) -> UserSettings) -> Unit,
    onReset: () -> Unit
) {
    val colors = AppTheme.colors
    var confirmReset by remember { mutableStateOf(false) }
    LaunchedEffect(confirmReset) {
        if (confirmReset) {
            delay(3_000)
            confirmReset = false
        }
    }
    SettingsCard("General") {
        SettingSwitchRow(
            Icons.Rounded.Vibration, "Haptic feedback", "A gentle tick on clicks and keys",
            settings.hapticFeedback, { v -> onUpdate { it.copy(hapticFeedback = v) } }
        )
        SettingSwitchRow(
            Icons.Rounded.LightMode, "Keep screen on", "Stop the display sleeping while the app is open",
            settings.keepScreenOn, { v -> onUpdate { it.copy(keepScreenOn = v) } }
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .bounceClick(pressedScale = 0.98f) {
                    if (confirmReset) {
                        confirmReset = false
                        onReset()
                    } else {
                        confirmReset = true
                    }
                }
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(Icons.Rounded.RestartAlt, colors.danger, size = 38.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (confirmReset) "Tap again to confirm" else "Reset to defaults",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (confirmReset) colors.danger else colors.text
                )
                Text("Your paired computers are kept", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun AboutCard() {
    val colors = AppTheme.colors
    Column {
        SectionTitle("About")
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(colors.surface, colors.accent.copy(alpha = 0.14f))
                    )
                )
                .border(1.dp, colors.outline, RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("about_card"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppLogo(size = 56.dp)
            Spacer(Modifier.height(12.dp))
            Text("HID Remote", style = MaterialTheme.typography.titleLarge, color = colors.text)
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.accentFaint)
                    .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Made with ", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                Icon(Icons.Rounded.Favorite, contentDescription = "love", tint = colors.danger, modifier = Modifier.size(16.dp))
                Text(" by ", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                Text(AppInfo.AUTHOR, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.text)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Native Bluetooth keyboard, mouse and media remote for Windows, macOS, Linux, ChromeOS and Android TV. " +
                    "No software needed on the computer. No internet, no ads, no tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Spacer(Modifier.height(12.dp))
        AboutLinks()
    }
}

@Composable
private fun AboutLinks() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    fun open(url: String) {
        try {
            uriHandler.openUri(url)
        } catch (_: Exception) {
        }
    }
    AppCard(Modifier.fillMaxWidth().testTag("about_links"), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
        LinkRow(Icons.Rounded.Person, AppInfo.AUTHOR, "github.com/${AppInfo.GITHUB_USER}") { open(AppInfo.GITHUB_PROFILE) }
        LinkRow(Icons.Rounded.Code, "Source code", "Open source on GitHub · give it a star") { open(AppInfo.REPO_URL) }
        LinkRow(Icons.Rounded.NewReleases, "Check for updates", "Latest release and changelog") { open(AppInfo.RELEASES_URL) }
        LinkRow(Icons.Rounded.BugReport, "Report a bug", "Or suggest a feature") { open(AppInfo.ISSUES_URL) }
        LinkRow(Icons.Rounded.Share, "Share HID Remote", "Send the download link to a friend", external = false) {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "HID Remote")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "HID Remote turns an Android phone into a Bluetooth keyboard, mouse and TV remote. " +
                        "No software needed on the computer. ${AppInfo.REPO_URL}"
                )
            }
            try {
                context.startActivity(Intent.createChooser(send, "Share HID Remote"))
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String, external: Boolean = true, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, colors.accent, size = 38.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
        Icon(
            if (external) Icons.AutoMirrored.Rounded.OpenInNew else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            null,
            tint = colors.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}
