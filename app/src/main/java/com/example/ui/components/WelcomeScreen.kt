package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.AppInfo
import com.example.ui.theme.AppTheme
import com.example.ui.theme.EmphasizedDecelerate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Highlight(val icon: ImageVector, val title: String, val body: String)

private val HIGHLIGHTS = listOf(
    Highlight(Icons.Rounded.TouchApp, "Precision touchpad", "Tap to click, two-finger scroll with momentum, three-finger swipes and drag."),
    Highlight(Icons.Rounded.Keyboard, "Full keyboard", "Live typing, voice dictation, function keys, snippets and OS-aware shortcuts."),
    Highlight(Icons.Rounded.SettingsRemote, "Media & TV remote", "D-pad, volume, playback and a presentation clicker for your slides."),
    Highlight(Icons.Rounded.Sensors, "Air mouse", "Point your phone at the screen and the cursor follows."),
    Highlight(Icons.Rounded.Lock, "Private by design", "Pure Bluetooth. No app on the computer, no internet, no ads, no tracking."),
)

/** First-run screen. [onGetStarted] finishes onboarding and asks for the Bluetooth permission. */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current
    // One entrance progress per block (logo, title, each highlight, button) for a staggered reveal.
    val blocks = HIGHLIGHTS.size + 3
    val progress = remember { List(blocks) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        progress.forEachIndexed { i, anim ->
            launch {
                delay(i * 70L)
                anim.animateTo(1f, tween(520, easing = EmphasizedDecelerate))
            }
        }
    }
    fun Modifier.reveal(index: Int) = graphicsLayer {
        val p = progress[index].value
        alpha = p
        translationY = (1f - p) * 28.dp.toPx()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to colors.accent.copy(alpha = 0.16f),
                    0.45f to colors.background,
                    1f to colors.background
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("welcome_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            AppLogo(Modifier.reveal(0), size = 96.dp)
            Spacer(Modifier.height(20.dp))
            Column(Modifier.reveal(1), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "HID Remote",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.text
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Turn your phone into a Bluetooth keyboard, mouse and TV remote for any computer.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "WINDOWS · MACOS · LINUX · CHROMEOS · ANDROID TV",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accent,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HIGHLIGHTS.forEachIndexed { i, item ->
                    HighlightRow(item, Modifier.reveal(i + 2))
                }
            }
            Spacer(Modifier.height(28.dp))
            Column(Modifier.reveal(blocks - 1), horizontalAlignment = Alignment.CenterHorizontally) {
                PrimaryButton(
                    text = "Get started",
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth().testTag("get_started_button")
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Next, allow \"Nearby devices\" so the phone can act as a Bluetooth keyboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .bounceClick(pressedScale = 0.96f) {
                            try {
                                uriHandler.openUri(AppInfo.GITHUB_PROFILE)
                            } catch (_: Exception) {
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Made by ", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    Text(AppInfo.AUTHOR, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = colors.text)
                    Text(" · ", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    Text("@${AppInfo.GITHUB_USER}", style = MaterialTheme.typography.bodySmall, color = colors.accent)
                }
            }
        }
    }
}

@Composable
private fun HighlightRow(item: Highlight, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    AppCard(modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(item.icon, colors.accent, size = 42.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = colors.text)
                Spacer(Modifier.size(2.dp))
                Text(item.body, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
    }
}
