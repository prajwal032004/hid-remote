package com.example

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.bluetooth.BluetoothDeviceModel
import com.example.bluetooth.DeviceType
import com.example.bluetooth.HidConnectionState
import com.example.model.HostOs
import com.example.model.UserSettings
import com.example.ui.components.ConnectionStatusBar
import com.example.ui.components.DevicesScreen
import com.example.ui.components.FullKeyboardScreen
import com.example.ui.components.IconAction
import com.example.ui.components.MediaRemoteScreen
import com.example.ui.components.MouseButtonsBar
import com.example.ui.components.RemoteKeyboardSheet
import com.example.ui.components.SettingsScreen
import com.example.ui.components.TouchpadView
import com.example.ui.components.WelcomeScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ModifierState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the app's screens into docs/screenshots for the README.
 * Regenerate with: ./gradlew :app:testDebugUnitTest --tests "*ReadmeScreenshotTest*"
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ReadmeScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun device(address: String): BluetoothDevice {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.getSystemService(BluetoothManager::class.java).adapter.getRemoteDevice(address)
    }

    private val connected by lazy {
        HidConnectionState.Connected(device("3C:22:FB:10:4A:01"), "Prajwal's MacBook Pro", "3C:22:FB:10:4A:01")
    }

    private fun capture(name: String) {
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = "../docs/screenshots/$name.png")
    }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) { content() }
            }
        }
        capture(name)
    }

    @Composable
    private fun Touchpad(modifier: Modifier, airMouse: Boolean = false) {
        Box(modifier) {
            TouchpadView(
                isConnected = true,
                isDragLockActive = false,
                isTapDragActive = false,
                tapToClick = true,
                tapToDrag = true,
                scrollInertia = true,
                onPointerDown = {},
                onPointerMove = { _, _, _ -> },
                onScroll = { _, _ -> },
                onTap = { _, _ -> },
                onTapDrag = {},
                onSwipe = {},
                modifier = Modifier.fillMaxSize()
            )
            IconAction(
                Icons.Rounded.Sensors, "Air mouse", {}, active = airMouse,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
            )
        }
    }

    @Composable
    private fun StatusBar(keyboardOpen: Boolean) {
        ConnectionStatusBar(
            connectionState = connected,
            isKeyboardOpen = keyboardOpen,
            onToggleKeyboard = {},
            onOpenSettings = {},
            onOpenDevicePicker = {},
            onOpenMedia = {},
            onRetry = {}
        )
    }

    @Composable
    private fun KeyboardSheet(modifier: Modifier = Modifier, snippets: List<String> = emptyList()) {
        RemoteKeyboardSheet(
            modifierState = ModifierState(oneShot = 0x01),
            capsLockOn = false,
            hostOs = HostOs.MAC,
            onToggleModifier = {},
            onLockModifier = {},
            onClearModifiers = {},
            onSendKey = { _, _ -> },
            onSendConsumerKey = {},
            onShortcut = {},
            onSendText = { _, _ -> },
            onStreamText = { _, _ -> },
            onTypeClipboard = {},
            onExpand = {},
            onClose = {},
            modifier = modifier,
            snippets = snippets
        )
    }

    @Test
    fun welcome() = shoot("01_welcome") { WelcomeScreen(onGetStarted = {}) }

    @Test
    fun touchpad() = shoot("02_touchpad") {
        Column(Modifier.fillMaxSize().padding(top = 28.dp)) {
            StatusBar(keyboardOpen = false)
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 12.dp, bottom = 20.dp)) {
                Touchpad(Modifier.weight(1f), airMouse = true)
                Spacer(Modifier.height(8.dp))
                MouseButtonsBar(
                    leftHandedMode = false,
                    isDragLockActive = false,
                    onSlotPress = { _, _ -> },
                    onMiddleClick = {},
                    onWheelScroll = {},
                    onToggleDragLock = {}
                )
            }
        }
    }

    @Test
    fun keyboard() = shoot("03_keyboard") {
        Column(Modifier.fillMaxSize().padding(top = 28.dp)) {
            StatusBar(keyboardOpen = true)
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 12.dp, bottom = 20.dp)) {
                Touchpad(Modifier.weight(1f))
                Spacer(Modifier.height(8.dp))
                KeyboardSheet()
            }
        }
    }

    @Test
    fun snippets() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(
                    Modifier.fillMaxSize().background(AppTheme.colors.background).padding(start = 12.dp, end = 12.dp, top = 40.dp, bottom = 20.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column {
                        StatusBar(keyboardOpen = true)
                        Spacer(Modifier.height(8.dp))
                        Touchpad(Modifier.weight(1f))
                        Spacer(Modifier.height(8.dp))
                        KeyboardSheet(
                            snippets = listOf("prajwal@example.com", "Thanks, talk soon!", "git status", "https://github.com/prajwal032004", "Regards, Prajwal")
                        )
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("Type").performClick()
        composeTestRule.onNode(hasScrollAction() and hasAnyDescendant(hasTestTag("snippets_row")))
            .performScrollToNode(hasTestTag("snippets_row"))
        capture("04_snippets")
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-420dpi")
    fun fullKeyboard() = shoot("05_full_keyboard") {
        FullKeyboardScreen(
            connectionState = connected,
            modifierState = ModifierState(oneShot = 0x02),
            capsLockOn = false,
            hostOs = HostOs.WINDOWS,
            showTouchpad = true,
            onToggleTouchpad = {},
            onToggleModifier = {},
            onLockModifier = {},
            onSendKey = { _, _ -> },
            onClose = {},
            touchpad = { Touchpad(it) }
        )
    }

    @Test
    fun mediaRemote() = shoot("06_media_remote") {
        Box(Modifier.padding(top = 28.dp)) {
            MediaRemoteScreen(
                connectionState = connected,
                hostOs = HostOs.ANDROID,
                onSendKey = { _, _ -> },
                onSendConsumerKey = {},
                onShortcut = {},
                onBack = {}
            )
        }
    }

    @Test
    fun devices() = shoot("07_devices") {
        val hosts = listOf(
            BluetoothDeviceModel("Prajwal's MacBook Pro", "3C:22:FB:10:4A:01", true, DeviceType.COMPUTER, device("3C:22:FB:10:4A:01")),
            BluetoothDeviceModel("Living Room TV", "A4:30:7A:22:9C:5E", true, DeviceType.TV, device("A4:30:7A:22:9C:5E")),
            BluetoothDeviceModel("DESKTOP-WORK", "70:9C:D1:4F:21:B3", true, DeviceType.COMPUTER, device("70:9C:D1:4F:21:B3")),
        )
        Box(Modifier.padding(top = 28.dp)) {
            DevicesScreen(
                pairedDevices = hosts,
                connectionState = connected,
                hostOs = HostOs.MAC,
                lastDeviceAddress = connected.address,
                onSelectDevice = {},
                onDisconnect = {},
                onRefresh = {},
                onMakeDiscoverable = {},
                onOpenHelp = {},
                onBack = {}
            )
        }
    }

    @Test
    fun settings() = shoot("08_settings") {
        Box(Modifier.padding(top = 28.dp)) {
            SettingsScreen(settings = UserSettings(), onUpdate = {}, onReset = {}, onOpenHelp = {}, onBack = {})
        }
    }

    @Test
    fun about() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().background(AppTheme.colors.background).padding(top = 28.dp)) {
                    SettingsScreen(settings = UserSettings(), onUpdate = {}, onReset = {}, onOpenHelp = {}, onBack = {})
                }
            }
        }
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("about_links"))
        capture("09_about")
    }
}
