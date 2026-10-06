package com.example

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bluetooth.HidConnectionState
import com.example.ui.components.CompatibilityWarningCard
import com.example.ui.components.ConnectionStatusBar
import com.example.ui.components.DevicesScreen
import com.example.ui.components.FullKeyboardScreen
import com.example.ui.components.HelpScreen
import com.example.ui.components.MediaRemoteScreen
import com.example.ui.components.MouseButtonsBar
import com.example.ui.components.RemoteKeyboardSheet
import com.example.ui.components.SettingsScreen
import com.example.ui.components.IconAction
import com.example.ui.components.TouchpadView
import com.example.ui.components.WelcomeScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.EmphasizedEasing
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.HidRemoteViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so system bar icons must always be light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            val viewModel: HidRemoteViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            MyApplicationTheme(accent = settings.accent, amoled = settings.amoledBlack) {
                HidRemoteApp(
                    viewModel = viewModel,
                    onEnableBluetoothRequested = {
                        try {
                            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        } catch (_: Exception) {
                        }
                    }
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun HidRemoteApp(
    viewModel: HidRemoteViewModel = viewModel(),
    onEnableBluetoothRequested: () -> Unit
) {
    val colors = AppTheme.colors
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val modifierState by viewModel.modifiers.collectAsStateWithLifecycle()
    val capsLockOn by viewModel.capsLockOn.collectAsStateWithLifecycle()
    val isDragLockActive by viewModel.isDragLockActive.collectAsStateWithLifecycle()
    val isTapDragActive by viewModel.isTapDragActive.collectAsStateWithLifecycle()
    val isAirMouseActive by viewModel.isAirMouseActive.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    // Keep-screen-on follows the view, so it only applies while the app is visible.
    val view = LocalView.current
    DisposableEffect(view, settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // Take the Bluetooth HID role only while the app is on screen (see onAppBackgrounded).
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForegrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackgrounded() }
    // Pick up permission or Bluetooth changes made in system settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.retryInitialization() }

    BackHandler(enabled = screen != AppScreen.REMOTE) { viewModel.navigateBack() }

    // Full keyboard: landscape on phones and hide the system bars for maximum key size.
    val context = LocalContext.current
    val isPhone = LocalConfiguration.current.smallestScreenWidthDp < 600
    val fullKeyboard = screen == AppScreen.KEYBOARD
    DisposableEffect(fullKeyboard, settings.landscapeFullKeyboard, isPhone) {
        val activity = context.findActivity()
        val window = activity?.window
        if (fullKeyboard && activity != null && window != null) {
            if (isPhone && settings.landscapeFullKeyboard) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (fullKeyboard && activity != null && window != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.retryInitialization()
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_CANCELED) {
            viewModel.showMessage("Visible for 5 minutes. Now add this phone from the computer's Bluetooth settings.")
        }
    }

    fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        permissionsLauncher.launch(permissions)
    }

    val retry: () -> Unit = {
        when (connectionState) {
            HidConnectionState.BluetoothOff -> onEnableBluetoothRequested()
            HidConnectionState.PermissionsRequired -> requestPermissions()
            else -> viewModel.retryInitialization()
        }
    }

    val touchpad: @Composable (Modifier, Boolean, Boolean) -> Unit = { modifier, compact, showButtons ->
        Column(modifier) {
            Box(Modifier.weight(1f)) {
                TouchpadView(
                    isConnected = connectionState is HidConnectionState.Connected,
                    isDragLockActive = isDragLockActive,
                    isTapDragActive = isTapDragActive,
                    tapToClick = settings.tapToClick,
                    tapToDrag = settings.tapToDrag,
                    scrollInertia = settings.scrollInertia,
                    onPointerDown = { viewModel.onPointerDown() },
                    onPointerMove = { dx, dy, t -> viewModel.onPointerMove(dx, dy, t) },
                    onScroll = { dx, dy -> viewModel.onScroll(dx, dy) },
                    onTap = { fingers, count -> viewModel.onTap(fingers, count) },
                    onTapDrag = { viewModel.setTapDrag(it) },
                    onSwipe = { viewModel.onSwipe(it) },
                    modifier = Modifier.fillMaxSize()
                )
                if (viewModel.isAirMouseAvailable && connectionState is HidConnectionState.Connected) {
                    IconAction(
                        Icons.Rounded.Sensors,
                        if (isAirMouseActive) "Turn off air mouse" else "Turn on air mouse",
                        { viewModel.toggleAirMouse() },
                        active = isAirMouseActive,
                        size = if (compact) 38.dp else 44.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    )
                }
            }
            if (settings.showMouseButtons && showButtons) {
                Spacer(Modifier.height(8.dp))
                MouseButtonsBar(
                    leftHandedMode = settings.leftHandedMode,
                    isDragLockActive = isDragLockActive,
                    onSlotPress = { leftSlot, pressed -> viewModel.onBarButton(leftSlot, pressed) },
                    onMiddleClick = { viewModel.onMiddleClick() },
                    onWheelScroll = { viewModel.onWheelScroll(it) },
                    onToggleDragLock = { viewModel.toggleDragLock() },
                    buttonHeight = if (compact) 46.dp else 60.dp,
                    compact = compact
                )
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        AnimatedContent(
            targetState = settings.onboardingDone,
            transitionSpec = {
                (fadeIn(tween(320, delayMillis = 80)) + scaleIn(tween(420, easing = EmphasizedEasing), initialScale = 0.96f)) togetherWith
                    fadeOut(tween(200))
            },
            label = "onboarding"
        ) { onboarded ->
            if (!onboarded) {
                WelcomeScreen(onGetStarted = {
                    viewModel.completeOnboarding()
                    requestPermissions()
                })
            } else {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val spec = if (targetState == AppScreen.KEYBOARD || initialState == AppScreen.KEYBOARD) {
                            (fadeIn(tween(260, delayMillis = 60)) + scaleIn(tween(340, easing = EmphasizedEasing), initialScale = 0.94f)) togetherWith
                                fadeOut(tween(140))
                        } else {
                            val forward = targetState.depth > initialState.depth
                            val dir = if (forward) 1 else -1
                            (slideInHorizontally(tween(380, easing = EmphasizedEasing)) { w -> dir * w / 4 } + fadeIn(tween(260, delayMillis = 60))) togetherWith
                                (slideOutHorizontally(tween(380, easing = EmphasizedEasing)) { w -> -dir * w / 10 } + fadeOut(tween(160)))
                        }
                        spec.apply { targetContentZIndex = targetState.depth.toFloat() }
                    },
                    label = "screen"
                ) { target ->
                    when (target) {
                        AppScreen.REMOTE -> RemoteScreen(
                            viewModel = viewModel,
                            connectionState = connectionState,
                            modifierState = modifierState,
                            capsLockOn = capsLockOn,
                            settings = settings,
                            onRetry = retry,
                            onEnableBluetooth = onEnableBluetoothRequested,
                            onRequestPermissions = { requestPermissions() },
                            onTypeClipboard = { viewModel.typeClipboard(clipboard.getText()?.text) },
                            touchpad = touchpad
                        )
                        AppScreen.DEVICES -> DevicesScreen(
                            pairedDevices = pairedDevices,
                            connectionState = connectionState,
                            hostOs = settings.hostOs,
                            lastDeviceAddress = settings.lastConnectedDeviceAddress,
                            onSelectDevice = { viewModel.connectDevice(it) },
                            onDisconnect = { viewModel.disconnect() },
                            onRefresh = { viewModel.refreshDevices() },
                            onMakeDiscoverable = {
                                try {
                                    discoverableLauncher.launch(viewModel.discoverableIntent())
                                } catch (_: Exception) {
                                    viewModel.showMessage("Couldn't make the phone discoverable. Use Bluetooth settings instead.")
                                }
                            },
                            onOpenHelp = { viewModel.navigate(AppScreen.HELP) },
                            onBack = { viewModel.navigateBack() }
                        )
                        AppScreen.SETTINGS -> SettingsScreen(
                            settings = settings,
                            onUpdate = { viewModel.updateSettings(it) },
                            onReset = { viewModel.resetSettings() },
                            onOpenHelp = { viewModel.navigate(AppScreen.HELP) },
                            onBack = { viewModel.navigateBack() }
                        )
                        AppScreen.MEDIA -> MediaRemoteScreen(
                            connectionState = connectionState,
                            hostOs = settings.hostOs,
                            onSendKey = { mod, key -> viewModel.sendKey(mod, key) },
                            onSendConsumerKey = { viewModel.sendConsumerKey(it) },
                            onShortcut = { viewModel.performShortcut(it) },
                            onBack = { viewModel.navigateBack() }
                        )
                        AppScreen.KEYBOARD -> FullKeyboardScreen(
                            connectionState = connectionState,
                            modifierState = modifierState,
                            capsLockOn = capsLockOn,
                            hostOs = settings.hostOs,
                            showTouchpad = settings.fullKeyboardTouchpad,
                            onToggleTouchpad = { viewModel.updateSettings { it.copy(fullKeyboardTouchpad = !it.fullKeyboardTouchpad) } },
                            onToggleModifier = { viewModel.toggleModifier(it) },
                            onLockModifier = { viewModel.lockModifier(it) },
                            onSendKey = { mod, key -> viewModel.sendKey(mod, key) },
                            onClose = { viewModel.navigateBack() },
                            touchpad = { modifier -> touchpad(modifier, true, true) }
                        )
                        AppScreen.HELP -> HelpScreen(onBack = { viewModel.navigateBack() })
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
                .widthIn(max = 560.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = colors.surfaceHigher,
                contentColor = colors.text,
                actionColor = colors.accent,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun RemoteScreen(
    viewModel: HidRemoteViewModel,
    connectionState: HidConnectionState,
    modifierState: com.example.viewmodel.ModifierState,
    capsLockOn: Boolean,
    settings: com.example.model.UserSettings,
    onRetry: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onRequestPermissions: () -> Unit,
    onTypeClipboard: () -> Unit,
    touchpad: @Composable (Modifier, Boolean, Boolean) -> Unit
) {
    val isKeyboardOpen by viewModel.isKeyboardOpen.collectAsStateWithLifecycle()
    val isWarningState = connectionState is HidConnectionState.BluetoothOff ||
        connectionState is HidConnectionState.PermissionsRequired ||
        connectionState is HidConnectionState.NotSupported ||
        connectionState is HidConnectionState.Error

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Side-by-side on landscape phones, tablets and unfolded foldables; stacked otherwise.
        val landscape = maxWidth > maxHeight && maxWidth >= 560.dp
        val shortScreen = maxHeight < 480.dp
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        val keyboard: @Composable (Modifier, Dp) -> Unit = { modifier, keyHeight ->
            RemoteKeyboardSheet(
                modifierState = modifierState,
                capsLockOn = capsLockOn,
                hostOs = settings.hostOs,
                onToggleModifier = { viewModel.toggleModifier(it) },
                onLockModifier = { viewModel.lockModifier(it) },
                onClearModifiers = { viewModel.clearModifiers() },
                onSendKey = { mod, key -> viewModel.sendKey(mod, key) },
                onSendConsumerKey = { viewModel.sendConsumerKey(it) },
                onShortcut = { viewModel.performShortcut(it) },
                onSendText = { text, done -> viewModel.sendText(text, done) },
                onStreamText = { old, new -> viewModel.streamTextChange(old, new) },
                onTypeClipboard = onTypeClipboard,
                onExpand = { viewModel.navigate(AppScreen.KEYBOARD) },
                onClose = { viewModel.setKeyboardOpen(false) },
                modifier = modifier,
                keyHeight = keyHeight,
                snippets = settings.snippets,
                onTypeSnippet = { viewModel.typeSnippet(it) },
                onSaveSnippet = { viewModel.saveSnippet(it) },
                onDeleteSnippet = { viewModel.deleteSnippet(it) }
            )
        }

        Column(Modifier.fillMaxSize()) {
            ConnectionStatusBar(
                connectionState = connectionState,
                isKeyboardOpen = isKeyboardOpen,
                onToggleKeyboard = { viewModel.toggleKeyboard() },
                onOpenSettings = { viewModel.navigate(AppScreen.SETTINGS) },
                onOpenDevicePicker = { viewModel.navigate(AppScreen.DEVICES) },
                onOpenMedia = { viewModel.navigate(AppScreen.MEDIA) },
                onRetry = onRetry,
                compact = shortScreen
            )

            AnimatedVisibility(
                visible = isWarningState,
                enter = fadeIn(tween(250)) + expandVertically(spring(stiffness = 500f)),
                exit = fadeOut(tween(150)) + shrinkVertically(spring(stiffness = 600f))
            ) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                    CompatibilityWarningCard(
                        state = connectionState,
                        onRequestEnableBluetooth = onEnableBluetooth,
                        onRequestPermissions = onRequestPermissions,
                        onRetry = { viewModel.retryInitialization() },
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                }
            }

            if (landscape) {
                // Header, tabs and paddings of the panel take ~136 dp; the rest is 5 key rows.
                val keyHeight = ((screenHeight - 76.dp - 136.dp) / 5).coerceIn(30.dp, 46.dp)
                Row(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                ) {
                    touchpad(Modifier.weight(1f).fillMaxHeight(), shortScreen, true)
                    AnimatedVisibility(
                        visible = isKeyboardOpen,
                        enter = fadeIn(tween(220, 60)) + expandHorizontally(tween(340, easing = EmphasizedEasing)),
                        exit = fadeOut(tween(140)) + shrinkHorizontally(tween(300, easing = EmphasizedEasing))
                    ) {
                        Row(Modifier.fillMaxHeight()) {
                            Spacer(Modifier.width(12.dp))
                            keyboard(
                                Modifier.width(min(screenWidth * 0.56f, 640.dp)).fillMaxHeight(),
                                keyHeight
                            )
                        }
                    }
                }
            } else {
                val keyHeight = if (screenHeight < 700.dp) 40.dp else 46.dp
                // On short screens the keyboard replaces the mouse buttons instead of squeezing the pad.
                val buttonsFit = !isKeyboardOpen || screenHeight >= 640.dp
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    touchpad(Modifier.weight(1f).widthIn(max = 900.dp), false, buttonsFit)
                    AnimatedVisibility(
                        visible = isKeyboardOpen,
                        enter = fadeIn(tween(220, 60)) + expandVertically(tween(340, easing = EmphasizedEasing), expandFrom = Alignment.Top),
                        exit = fadeOut(tween(140)) + shrinkVertically(tween(280, easing = EmphasizedEasing), shrinkTowards = Alignment.Top)
                    ) {
                        Column(Modifier.widthIn(max = 900.dp)) {
                            Spacer(Modifier.height(8.dp))
                            keyboard(Modifier.fillMaxWidth(), keyHeight)
                        }
                    }
                }
            }
        }
    }
}
