package com.example.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothDeviceModel
import com.example.bluetooth.BluetoothHidManager
import com.example.bluetooth.HidConnectionState
import com.example.hid.CharToHidMapper
import com.example.hid.HidMouseButtons
import com.example.input.GyroPointer
import com.example.input.PointerAccelerator
import com.example.input.ScrollAccumulator
import com.example.input.TextDiff
import com.example.model.HostCommand
import com.example.model.SettingsRepository
import com.example.model.ShortcutAction
import com.example.model.UserSettings
import com.example.model.commandFor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Top-level destinations. [depth] drives the direction of the transition between them. */
enum class AppScreen(val depth: Int) {
    REMOTE(0), DEVICES(1), SETTINGS(1), MEDIA(1), KEYBOARD(1), HELP(2)
}

/** Direction of a three-finger swipe, in finger-motion terms. */
enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

/** Sticky modifier state: one-shot modifiers clear after the next key or click; locked ones stay. */
data class ModifierState(val oneShot: Int = 0, val locked: Int = 0) {
    val active: Int get() = oneShot or locked
}

class HidRemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val hidManager = BluetoothHidManager(application)
    private val sender = hidManager.sender
    private val settingsRepository = SettingsRepository(application)
    private val vibrator = application.getSystemService(Vibrator::class.java)
    private val sensorManager = application.getSystemService(SensorManager::class.java)
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val connectionState: StateFlow<HidConnectionState> = hidManager.connectionState
    val pairedDevices: StateFlow<List<BluetoothDeviceModel>> = hidManager.pairedDevices
    val settings: StateFlow<UserSettings> = settingsRepository.settings
    val capsLockOn: StateFlow<Boolean> = hidManager.capsLockOn

    private val localMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = merge(hidManager.events, localMessages)
        .shareIn(viewModelScope, SharingStarted.Eagerly)

    private val backStack = ArrayDeque<AppScreen>()
    private val _screen = MutableStateFlow(AppScreen.REMOTE)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _isKeyboardOpen = MutableStateFlow(false)
    val isKeyboardOpen: StateFlow<Boolean> = _isKeyboardOpen.asStateFlow()

    private val _isDragLockActive = MutableStateFlow(false)
    val isDragLockActive: StateFlow<Boolean> = _isDragLockActive.asStateFlow()

    private val _isTapDragActive = MutableStateFlow(false)
    val isTapDragActive: StateFlow<Boolean> = _isTapDragActive.asStateFlow()

    private val _modifiers = MutableStateFlow(ModifierState())
    val modifiers: StateFlow<ModifierState> = _modifiers.asStateFlow()

    /** True when the phone has a gyroscope, so the air mouse can be offered at all. */
    val isAirMouseAvailable: Boolean = gyroscope != null

    private val _isAirMouseActive = MutableStateFlow(false)
    val isAirMouseActive: StateFlow<Boolean> = _isAirMouseActive.asStateFlow()

    // Pointer pipeline (touch events arrive on the main thread)
    private val accelerator = PointerAccelerator()
    private val verticalScroll = ScrollAccumulator()
    private val horizontalScroll = ScrollAccumulator()
    private val gyroPointer = GyroPointer()
    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val out = gyroPointer.process(event.values[0], event.values[2], event.timestamp)
            if (out.dx != 0 || out.dy != 0) sender.move(out.dx, out.dy)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    private var physicalButtons = 0
    private var backgroundJob: Job? = null

    init {
        hidManager.initialize()

        viewModelScope.launch {
            settings.collect { s ->
                accelerator.sensitivity = s.mouseSensitivity
                accelerator.acceleration = s.accelerationCurve
                gyroPointer.sensitivity = s.airMouseSensitivity
                val dpPerTick = SCROLL_DP_PER_TICK / s.scrollSensitivity.coerceAtLeast(0.1f)
                verticalScroll.dpPerTick = dpPerTick
                horizontalScroll.dpPerTick = dpPerTick
            }
        }

        viewModelScope.launch {
            settings
                .map { if (it.autoReconnect) it.lastConnectedDeviceAddress else null }
                .distinctUntilChanged()
                .collect { hidManager.setReconnectTarget(it) }
        }

        viewModelScope.launch {
            connectionState.collect { state ->
                if (state is HidConnectionState.Connected) {
                    settingsRepository.setLastConnectedDevice(state.address)
                } else {
                    // Host is gone: every held input is implicitly released.
                    physicalButtons = 0
                    _isDragLockActive.value = false
                    _isTapDragActive.value = false
                    _modifiers.value = ModifierState()
                    setAirMouse(false)
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Touchpad
    // ------------------------------------------------------------------

    fun onPointerDown() {
        accelerator.reset()
        verticalScroll.reset()
        horizontalScroll.reset()
    }

    fun onPointerMove(dxDp: Float, dyDp: Float, eventTimeMs: Long) {
        val out = accelerator.process(dxDp, dyDp, eventTimeMs)
        sender.move(out.dx, out.dy)
    }

    /** Two-finger scroll deltas in dp, in finger-motion direction. */
    fun onScroll(dxDp: Float, dyDp: Float) {
        val natural = if (settings.value.naturalScroll) 1f else -1f
        // Natural: content follows the finger. HID wheel+ scrolls up, AC Pan+ scrolls right.
        val wheel = verticalScroll.add(dyDp * natural)
        val pan = horizontalScroll.add(-dxDp * natural)
        sender.scroll(wheel, pan)
    }

    /** The bar's scroll wheel behaves like a physical wheel: roll down to scroll down. */
    fun onWheelScroll(dyDp: Float) {
        sender.scroll(verticalScroll.add(-dyDp), 0)
    }

    fun onTap(fingers: Int, count: Int = 1) {
        val mask = when (fingers) {
            1 -> HidMouseButtons.LEFT
            2 -> HidMouseButtons.RIGHT
            else -> HidMouseButtons.MIDDLE
        }
        haptic(Haptic.CLICK)
        sender.click(mask.toInt(), count)
        consumeOneShotModifiers()
    }

    fun setTapDrag(active: Boolean) {
        if (_isTapDragActive.value == active) return
        _isTapDragActive.value = active
        if (active) haptic(Haptic.CLICK)
        pushHeldButtons()
        if (!active) consumeOneShotModifiers()
    }

    fun onSwipe(direction: SwipeDirection) {
        val action = when (direction) {
            SwipeDirection.UP -> ShortcutAction.TASK_VIEW
            SwipeDirection.DOWN -> ShortcutAction.SHOW_DESKTOP
            SwipeDirection.LEFT -> ShortcutAction.DESKTOP_RIGHT
            SwipeDirection.RIGHT -> ShortcutAction.DESKTOP_LEFT
        }
        performShortcut(action)
    }

    // ------------------------------------------------------------------
    // Air mouse (gyroscope pointer)
    // ------------------------------------------------------------------

    fun toggleAirMouse() {
        if (!_isAirMouseActive.value && connectionState.value !is HidConnectionState.Connected) {
            localMessages.tryEmit("Connect to a host first")
            return
        }
        setAirMouse(!_isAirMouseActive.value)
        if (_isAirMouseActive.value) {
            localMessages.tryEmit("Air mouse on: point the top of the phone at the screen. Tap the pad to click.")
        }
    }

    private fun setAirMouse(active: Boolean) {
        val sensor = gyroscope ?: return
        if (_isAirMouseActive.value == active) return
        _isAirMouseActive.value = active
        gyroPointer.reset()
        if (active) {
            haptic(Haptic.HEAVY)
            sensorManager?.registerListener(gyroListener, sensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            sensorManager?.unregisterListener(gyroListener)
        }
    }

    // ------------------------------------------------------------------
    // Physical mouse buttons bar
    // ------------------------------------------------------------------

    /** [leftSlot] is the on-screen position; left-handed mode swaps what each slot presses. */
    fun onBarButton(leftSlot: Boolean, pressed: Boolean) {
        val primary = leftSlot != settings.value.leftHandedMode
        val mask = (if (primary) HidMouseButtons.LEFT else HidMouseButtons.RIGHT).toInt()
        setPhysical(mask, pressed)
    }


    fun onMiddleClick() {
        haptic(Haptic.CLICK)
        sender.click(HidMouseButtons.MIDDLE.toInt())
    }

    private fun setPhysical(mask: Int, pressed: Boolean) {
        physicalButtons = if (pressed) physicalButtons or mask else physicalButtons and mask.inv()
        if (pressed) haptic(Haptic.CLICK)
        pushHeldButtons()
        if (!pressed) consumeOneShotModifiers()
    }

    fun toggleDragLock() {
        _isDragLockActive.value = !_isDragLockActive.value
        haptic(Haptic.HEAVY)
        pushHeldButtons()
    }

    private fun pushHeldButtons() {
        var mask = physicalButtons
        if (_isDragLockActive.value || _isTapDragActive.value) mask = mask or HidMouseButtons.LEFT.toInt()
        sender.setHeldButtons(mask)
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    fun sendKey(modifier: Byte, keyCode: Byte) {
        haptic(Haptic.TICK)
        sender.tapKey(modifier.toInt(), keyCode)
        consumeOneShotModifiers()
    }

    fun sendConsumerKey(usage: Int) {
        haptic(Haptic.TICK)
        sender.tapConsumer(usage)
    }

    /** Tap: off → one-shot → off. Long-press: lock (or unlock). */
    fun toggleModifier(modifier: Byte) {
        val bit = modifier.toInt() and 0xFF
        val m = _modifiers.value
        _modifiers.value = when {
            m.locked and bit != 0 -> m.copy(locked = m.locked and bit.inv())
            m.oneShot and bit != 0 -> m.copy(oneShot = m.oneShot and bit.inv())
            else -> m.copy(oneShot = m.oneShot or bit)
        }
        haptic(Haptic.TICK)
        sender.setStickyModifiers(_modifiers.value.active)
    }

    fun lockModifier(modifier: Byte) {
        val bit = modifier.toInt() and 0xFF
        val m = _modifiers.value
        _modifiers.value = if (m.locked and bit != 0) {
            m.copy(locked = m.locked and bit.inv(), oneShot = m.oneShot and bit.inv())
        } else {
            m.copy(locked = m.locked or bit, oneShot = m.oneShot and bit.inv())
        }
        haptic(Haptic.HEAVY)
        sender.setStickyModifiers(_modifiers.value.active)
    }

    fun clearModifiers() {
        _modifiers.value = ModifierState()
        sender.setStickyModifiers(0)
    }

    private fun consumeOneShotModifiers() {
        val m = _modifiers.value
        if (m.oneShot == 0) return
        _modifiers.value = m.copy(oneShot = 0)
        sender.setStickyModifiers(m.locked)
    }

    fun sendText(text: String, onComplete: (() -> Unit)? = null) {
        val events = text.mapNotNull { CharToHidMapper.mapChar(it) }
        if (events.isEmpty()) {
            onComplete?.invoke()
            return
        }
        sender.typeKeys(events) {
            onComplete?.let { done -> viewModelScope.launch { done() } }
        }
    }

    /** Mirrors a live text field onto the host, including IME corrections and deletions. */
    fun streamTextChange(old: String, new: String) {
        val keystrokes = TextDiff.keystrokes(old, new)
        if (keystrokes.isNotEmpty()) sendText(keystrokes)
    }

    /** Types the phone's clipboard text on the host. */
    fun typeClipboard(text: String?) {
        if (text.isNullOrEmpty()) {
            localMessages.tryEmit("The phone's clipboard is empty")
            return
        }
        if (connectionState.value !is HidConnectionState.Connected) {
            localMessages.tryEmit("Connect to a host first")
            return
        }
        val typable = text.count { CharToHidMapper.mapChar(it) != null }
        sendText(text)
        val skipped = text.length - typable
        localMessages.tryEmit(
            if (skipped > 0) "Typing $typable characters ($skipped unsupported skipped)" else "Typing $typable characters"
        )
    }

    /** Types a saved snippet on the host. */
    fun typeSnippet(text: String) {
        if (connectionState.value !is HidConnectionState.Connected) {
            localMessages.tryEmit("Connect to a host first")
            return
        }
        sendText(text)
    }

    fun saveSnippet(text: String) {
        val snippet = text.trim()
        if (snippet.isEmpty()) return
        val current = settings.value.snippets
        if (snippet in current) {
            localMessages.tryEmit("Already saved")
            return
        }
        updateSettings { it.copy(snippets = (listOf(snippet) + current).take(MAX_SNIPPETS)) }
        localMessages.tryEmit("Snippet saved")
    }

    fun deleteSnippet(text: String) {
        updateSettings { s -> s.copy(snippets = s.snippets.filterNot { it == text }) }
        localMessages.tryEmit("Snippet removed")
    }

    fun completeOnboarding() {
        updateSettings { it.copy(onboardingDone = true) }
    }

    fun performShortcut(action: ShortcutAction) {
        val os = settings.value.hostOs
        when (val command = os.commandFor(action)) {
            is HostCommand.Key -> {
                haptic(Haptic.CLICK)
                sender.tapKey(command.modifiers, command.keyCode)
            }
            is HostCommand.Consumer -> {
                haptic(Haptic.CLICK)
                sender.tapConsumer(command.usage)
            }
            null -> localMessages.tryEmit("Not available on ${os.label}")
        }
    }

    // ------------------------------------------------------------------
    // Dialogs & connection
    // ------------------------------------------------------------------

    fun navigate(to: AppScreen) {
        if (_screen.value == to) return
        if (to == AppScreen.REMOTE) {
            backStack.clear()
        } else {
            backStack.addLast(_screen.value)
        }
        if (to == AppScreen.DEVICES) hidManager.refreshPairedDevices()
        _screen.value = to
    }

    /** Returns false when already on the root screen (the system should handle Back). */
    fun navigateBack(): Boolean {
        if (_screen.value == AppScreen.REMOTE) return false
        _screen.value = backStack.removeLastOrNull() ?: AppScreen.REMOTE
        return true
    }

    fun refreshDevices() {
        hidManager.refreshPairedDevices()
    }

    fun toggleKeyboard() {
        _isKeyboardOpen.value = !_isKeyboardOpen.value
    }

    fun setKeyboardOpen(open: Boolean) {
        _isKeyboardOpen.value = open
    }

    fun connectDevice(device: BluetoothDevice) {
        if (_screen.value == AppScreen.DEVICES) navigateBack()
        hidManager.connect(device)
    }

    fun disconnect() {
        hidManager.disconnect()
    }

    fun retryInitialization() {
        hidManager.initialize()
    }

    fun discoverableIntent(): Intent = hidManager.discoverableIntent()

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        settingsRepository.updateSettings(transform(settings.value))
    }

    fun showMessage(message: String) {
        localMessages.tryEmit(message)
    }

    fun resetSettings() {
        settingsRepository.resetToDefaults()
        localMessages.tryEmit("Settings restored to defaults")
    }

    fun onAppForegrounded() {
        backgroundJob?.cancel()
        backgroundJob = null
        hidManager.initialize()
    }

    /**
     * Leaving the app must never leave a key or button stuck down on the host. After a grace
     * period (so a quick app switch or a trip to Bluetooth settings doesn't drop anything) the
     * HID registration is released: no background work, and the phone's own Bluetooth keyboards,
     * mice and controllers work normally again. Opening the app reconnects automatically.
     */
    fun onAppBackgrounded() {
        physicalButtons = 0
        _isDragLockActive.value = false
        _isTapDragActive.value = false
        _modifiers.value = ModifierState()
        setAirMouse(false)
        sender.releaseAll()

        backgroundJob?.cancel()
        val connected = connectionState.value is HidConnectionState.Connected
        if (connected && settings.value.stayConnectedInBackground) return
        backgroundJob = viewModelScope.launch {
            delay(BACKGROUND_GRACE_MS)
            hidManager.suspend()
        }
    }

    // ------------------------------------------------------------------

    private enum class Haptic { TICK, CLICK, HEAVY }

    private fun haptic(kind: Haptic) {
        if (!settings.value.hapticFeedback) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(
                    VibrationEffect.createPredefined(
                        when (kind) {
                            Haptic.TICK -> VibrationEffect.EFFECT_TICK
                            Haptic.CLICK -> VibrationEffect.EFFECT_CLICK
                            Haptic.HEAVY -> VibrationEffect.EFFECT_HEAVY_CLICK
                        }
                    )
                )
            } else {
                val ms = when (kind) {
                    Haptic.TICK -> 8L
                    Haptic.CLICK -> 15L
                    Haptic.HEAVY -> 30L
                }
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        setAirMouse(false)
        hidManager.cleanup()
    }

    companion object {
        /** Finger travel (dp) per wheel notch at scroll sensitivity 1.0. */
        const val SCROLL_DP_PER_TICK = 14f

        /** How long the host stays connected after the app leaves the screen. */
        const val BACKGROUND_GRACE_MS = 60_000L

        const val MAX_SNIPPETS = 30
    }
}
