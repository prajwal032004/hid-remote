package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.bluetooth.HidConnectionState
import com.example.model.HostOs
import com.example.ui.components.ConnectionStatusBar
import com.example.ui.components.FullKeyboardScreen
import com.example.ui.components.MediaRemoteScreen
import com.example.ui.components.MouseButtonsBar
import com.example.ui.components.RemoteKeyboardSheet
import com.example.ui.components.TouchpadView
import com.example.ui.theme.DarkBackground
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class RemoteUiScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun touchpad_screen() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Column(Modifier.fillMaxSize().background(DarkBackground)) {
                    ConnectionStatusBar(
                        connectionState = HidConnectionState.ReadyToConnect,
                        isKeyboardOpen = false,
                        onToggleKeyboard = {},
                        onOpenSettings = {},
                        onOpenDevicePicker = {},
                        onOpenMedia = {},
                        onRetry = {}
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 4.dp)) {
                        TouchpadView(
                            isConnected = false,
                            isDragLockActive = true,
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
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.height(8.dp))
                        MouseButtonsBar(
                            leftHandedMode = false,
                            isDragLockActive = true,
                            onSlotPress = { _, _ -> },
                            onMiddleClick = {},
                            onWheelScroll = {},
                            onToggleDragLock = {}
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/touchpad_screen.png")
    }

    @Test
    fun keyboard_sheet_mac() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().background(DarkBackground).padding(12.dp), contentAlignment = Alignment.BottomCenter) {
                    RemoteKeyboardSheet(
                        modifierState = ModifierState(oneShot = 0x01, locked = 0x08),
                        capsLockOn = true,
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
                        onClose = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/keyboard_sheet_mac.png")
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land")
    fun full_keyboard_landscape() {
        composeTestRule.setContent {
            MyApplicationTheme {
                FullKeyboardScreen(
                    connectionState = HidConnectionState.ReadyToConnect,
                    modifierState = ModifierState(oneShot = 0x02),
                    capsLockOn = false,
                    hostOs = HostOs.WINDOWS,
                    showTouchpad = true,
                    onToggleTouchpad = {},
                    onToggleModifier = {},
                    onLockModifier = {},
                    onSendKey = { _, _ -> },
                    onClose = {},
                    touchpad = { modifier ->
                        TouchpadView(
                            isConnected = false,
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
                            modifier = modifier
                        )
                    }
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/full_keyboard_landscape.png")
    }

    @Test
    fun media_remote() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().background(DarkBackground)) {
                    MediaRemoteScreen(
                        connectionState = HidConnectionState.ReadyToConnect,
                        hostOs = HostOs.ANDROID,
                        onSendKey = { _, _ -> },
                        onSendConsumerKey = {},
                        onShortcut = {},
                        onBack = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/media_remote.png")
    }
}
