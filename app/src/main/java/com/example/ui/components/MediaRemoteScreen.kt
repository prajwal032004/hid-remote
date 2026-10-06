package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.bluetooth.HidConnectionState
import com.example.hid.HidConsumerKeys
import com.example.hid.HidKeyCodes
import com.example.model.HostOs
import com.example.model.ShortcutAction
import com.example.ui.theme.AppTheme

/**
 * TV-style remote: D-pad, Home/Back/Menu, volume and track rockers, plus a presentation clicker.
 * Buttons map to the right keys for the selected host system.
 */
@Composable
fun MediaRemoteScreen(
    connectionState: HidConnectionState,
    hostOs: HostOs,
    onSendKey: (modifier: Byte, keyCode: Byte) -> Unit,
    onSendConsumerKey: (Int) -> Unit,
    onShortcut: (ShortcutAction) -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        ScreenHeader(
            title = "Remote",
            subtitle = connectionState.title() + " · " + hostOs.label,
            onBack = onBack
        )
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val landscape = maxWidth > maxHeight && maxWidth >= 560.dp
            val availableHeight = maxHeight
            val dpad: @Composable (Dp) -> Unit = { size ->
                DPad(size, onSendKey)
            }
            if (landscape) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        NavRow(onShortcut)
                        Spacer(Modifier.height(12.dp))
                        dpad(min(availableHeight - 90.dp, 300.dp).coerceAtLeast(160.dp))
                    }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                    ) {
                        MediaCluster(onSendConsumerKey)
                        PresentationCard(onShortcut)
                        ExtrasRow(onSendKey, onSendConsumerKey, onShortcut)
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier
                            .widthIn(max = 520.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        NavRow(onShortcut)
                        dpad(min(this@BoxWithConstraints.maxWidth * 0.78f, 300.dp))
                        MediaCluster(onSendConsumerKey)
                        PresentationCard(onShortcut)
                        ExtrasRow(onSendKey, onSendConsumerKey, onShortcut)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavRow(onShortcut: (ShortcutAction) -> Unit) {
    Row(Modifier.fillMaxWidth().widthIn(max = 420.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PillKey(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.weight(1f)) { onShortcut(ShortcutAction.BACK) }
        PillKey(Icons.Rounded.Home, "Home", Modifier.weight(1f)) { onShortcut(ShortcutAction.HOME) }
        PillKey(Icons.Rounded.Menu, "Menu", Modifier.weight(1f)) { onShortcut(ShortcutAction.MENU) }
    }
}

@Composable
private fun PillKey(icon: ImageVector, label: String, modifier: Modifier = Modifier, repeatable: Boolean = false, onPress: () -> Unit) {
    KeyButton(
        modifier = modifier.height(52.dp),
        style = KeyStyle.NORMAL,
        cornerRadius = 50.dp,
        repeatable = repeatable,
        description = label,
        onPress = onPress
    ) { color ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Circular D-pad: four arrow segments around an OK button. Arrows repeat while held. */
@Composable
private fun DPad(size: Dp, onSendKey: (Byte, Byte) -> Unit) {
    val colors = AppTheme.colors
    val arrow = size * 0.3f
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.outline, CircleShape)
            .padding(size * 0.04f)
    ) {
        KeyButton(
            Modifier.align(Alignment.TopCenter).size(arrow * 1.3f, arrow),
            cornerRadius = 50.dp, repeatable = true, description = "Up",
            onPress = { onSendKey(0, HidKeyCodes.KEY_UP) }
        ) { Icon(Icons.Rounded.KeyboardArrowUp, null, tint = it, modifier = Modifier.size(arrow * 0.55f)) }
        KeyButton(
            Modifier.align(Alignment.BottomCenter).size(arrow * 1.3f, arrow),
            cornerRadius = 50.dp, repeatable = true, description = "Down",
            onPress = { onSendKey(0, HidKeyCodes.KEY_DOWN) }
        ) { Icon(Icons.Rounded.KeyboardArrowDown, null, tint = it, modifier = Modifier.size(arrow * 0.55f)) }
        KeyButton(
            Modifier.align(Alignment.CenterStart).size(arrow, arrow * 1.3f),
            cornerRadius = 50.dp, repeatable = true, description = "Left",
            onPress = { onSendKey(0, HidKeyCodes.KEY_LEFT) }
        ) { Icon(Icons.Rounded.KeyboardArrowLeft, null, tint = it, modifier = Modifier.size(arrow * 0.55f)) }
        KeyButton(
            Modifier.align(Alignment.CenterEnd).size(arrow, arrow * 1.3f),
            cornerRadius = 50.dp, repeatable = true, description = "Right",
            onPress = { onSendKey(0, HidKeyCodes.KEY_RIGHT) }
        ) { Icon(Icons.Rounded.KeyboardArrowRight, null, tint = it, modifier = Modifier.size(arrow * 0.55f)) }
        KeyButton(
            Modifier.align(Alignment.Center).size(size * 0.36f),
            style = KeyStyle.ACCENT, cornerRadius = size, description = "OK",
            onPress = { onSendKey(0, HidKeyCodes.KEY_ENTER) }
        ) { Text("OK", color = it, style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun MediaCluster(onSendConsumerKey: (Int) -> Unit) {
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Rocker(
                topIcon = Icons.AutoMirrored.Rounded.VolumeUp, topLabel = "Volume up",
                bottomIcon = Icons.AutoMirrored.Rounded.VolumeDown, bottomLabel = "Volume down",
                caption = "VOL",
                onTop = { onSendConsumerKey(HidConsumerKeys.VOLUME_UP) },
                onBottom = { onSendConsumerKey(HidConsumerKeys.VOLUME_DOWN) },
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyButton(
                    Modifier.size(76.dp),
                    style = KeyStyle.ACCENT, cornerRadius = 38.dp, description = "Play or pause",
                    onPress = { onSendConsumerKey(HidConsumerKeys.PLAY_PAUSE) }
                ) { Icon(Icons.Rounded.PlayArrow, null, tint = it, modifier = Modifier.size(36.dp)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyButton(Modifier.size(34.dp), cornerRadius = 17.dp, description = "Mute", onPress = { onSendConsumerKey(HidConsumerKeys.MUTE) }) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeOff, null, tint = it, modifier = Modifier.size(16.dp))
                    }
                    KeyButton(Modifier.size(34.dp), cornerRadius = 17.dp, description = "Stop", onPress = { onSendConsumerKey(HidConsumerKeys.STOP) }) {
                        Icon(Icons.Rounded.Stop, null, tint = it, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Rocker(
                topIcon = Icons.Rounded.SkipNext, topLabel = "Next track",
                bottomIcon = Icons.Rounded.SkipPrevious, bottomLabel = "Previous track",
                caption = "TRACK",
                onTop = { onSendConsumerKey(HidConsumerKeys.NEXT_TRACK) },
                onBottom = { onSendConsumerKey(HidConsumerKeys.PREV_TRACK) },
                repeatable = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Rocker(
    topIcon: ImageVector,
    topLabel: String,
    bottomIcon: ImageVector,
    bottomLabel: String,
    caption: String,
    onTop: () -> Unit,
    onBottom: () -> Unit,
    modifier: Modifier = Modifier,
    repeatable: Boolean = true
) {
    val colors = AppTheme.colors
    Column(
        modifier
            .height(132.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceHigh)
            .border(1.dp, colors.outline, RoundedCornerShape(50))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeyButton(
            Modifier.fillMaxWidth().weight(1f),
            cornerRadius = 50.dp, repeatable = repeatable, description = topLabel, onPress = onTop
        ) { Icon(topIcon, null, tint = it, modifier = Modifier.size(22.dp)) }
        Text(caption, style = MaterialTheme.typography.labelSmall, color = colors.textMuted, modifier = Modifier.padding(vertical = 2.dp))
        KeyButton(
            Modifier.fillMaxWidth().weight(1f),
            cornerRadius = 50.dp, repeatable = repeatable, description = bottomLabel, onPress = onBottom
        ) { Icon(bottomIcon, null, tint = it, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun PresentationCard(onShortcut: (ShortcutAction) -> Unit) {
    val colors = AppTheme.colors
    AppCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)) {
            Icon(Icons.Rounded.Slideshow, null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Presentation", style = MaterialTheme.typography.titleSmall, color = colors.text)
        }
        Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton(Modifier.weight(1.4f).fillMaxHeight(), cornerRadius = 18.dp, description = "Previous slide", onPress = { onShortcut(ShortcutAction.SLIDE_PREV) }) {
                Icon(Icons.Rounded.ChevronLeft, null, tint = it, modifier = Modifier.size(30.dp))
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyButton(Modifier.fillMaxWidth().weight(1f), style = KeyStyle.ACTION, cornerRadius = 12.dp, description = "Start show", onPress = { onShortcut(ShortcutAction.SLIDE_START) }) {
                    Text("Start", color = it, style = MaterialTheme.typography.labelMedium)
                }
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KeyButton(Modifier.weight(1f).fillMaxHeight(), style = KeyStyle.ACTION, cornerRadius = 12.dp, description = "Blank screen", onPress = { onShortcut(ShortcutAction.SLIDE_BLANK) }) {
                        Icon(Icons.Rounded.VisibilityOff, null, tint = it, modifier = Modifier.size(15.dp))
                    }
                    KeyButton(Modifier.weight(1f).fillMaxHeight(), style = KeyStyle.ACTION, cornerRadius = 12.dp, description = "End show", onPress = { onShortcut(ShortcutAction.SLIDE_END) }) {
                        Text("End", color = it, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            KeyButton(Modifier.weight(1.4f).fillMaxHeight(), style = KeyStyle.ACCENT, cornerRadius = 18.dp, description = "Next slide", onPress = { onShortcut(ShortcutAction.SLIDE_NEXT) }) {
                Icon(Icons.Rounded.ChevronRight, null, tint = it, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun ExtrasRow(onSendKey: (Byte, Byte) -> Unit, onSendConsumerKey: (Int) -> Unit, onShortcut: (ShortcutAction) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KeyButton(Modifier.weight(1f).fillMaxHeight(), cornerRadius = 50.dp, repeatable = true, description = "Brightness down", onPress = { onSendConsumerKey(HidConsumerKeys.BRIGHTNESS_DOWN) }) {
            Icon(Icons.Rounded.BrightnessLow, null, tint = it, modifier = Modifier.size(20.dp))
        }
        KeyButton(Modifier.weight(1f).fillMaxHeight(), cornerRadius = 50.dp, repeatable = true, description = "Brightness up", onPress = { onSendConsumerKey(HidConsumerKeys.BRIGHTNESS_UP) }) {
            Icon(Icons.Rounded.BrightnessHigh, null, tint = it, modifier = Modifier.size(20.dp))
        }
        KeyButton(Modifier.weight(1f).fillMaxHeight(), cornerRadius = 50.dp, description = "Search", onPress = { onShortcut(ShortcutAction.SEARCH) }) {
            Icon(Icons.Rounded.Search, null, tint = it, modifier = Modifier.size(20.dp))
        }
        // F toggles full screen in YouTube, Netflix, VLC and most web players.
        KeyButton(Modifier.weight(1f).fillMaxHeight(), cornerRadius = 50.dp, description = "Toggle full screen video", onPress = { onSendKey(0, HidKeyCodes.KEY_F) }) {
            Icon(Icons.Rounded.Fullscreen, null, tint = it, modifier = Modifier.size(20.dp))
        }
    }
}

