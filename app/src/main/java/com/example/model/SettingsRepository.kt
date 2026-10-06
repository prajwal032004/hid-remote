package com.example.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hid_remote_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            mouseSensitivity = prefs.getFloat(KEY_MOUSE_SENSITIVITY, defaults.mouseSensitivity),
            accelerationCurve = prefs.getFloat(KEY_ACCELERATION, defaults.accelerationCurve),
            scrollSensitivity = prefs.getFloat(KEY_SCROLL_SENSITIVITY, defaults.scrollSensitivity),
            naturalScroll = prefs.getBoolean(KEY_NATURAL_SCROLL, defaults.naturalScroll),
            scrollInertia = prefs.getBoolean(KEY_SCROLL_INERTIA, defaults.scrollInertia),
            tapToClick = prefs.getBoolean(KEY_TAP_TO_CLICK, defaults.tapToClick),
            tapToDrag = prefs.getBoolean(KEY_TAP_TO_DRAG, defaults.tapToDrag),
            leftHandedMode = prefs.getBoolean(KEY_LEFT_HANDED, defaults.leftHandedMode),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, defaults.hapticFeedback),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, defaults.keepScreenOn),
            autoReconnect = prefs.getBoolean(KEY_AUTO_RECONNECT, defaults.autoReconnect),
            stayConnectedInBackground = prefs.getBoolean(KEY_STAY_CONNECTED, defaults.stayConnectedInBackground),
            landscapeFullKeyboard = prefs.getBoolean(KEY_LANDSCAPE_KEYBOARD, defaults.landscapeFullKeyboard),
            fullKeyboardTouchpad = prefs.getBoolean(KEY_KEYBOARD_TOUCHPAD, defaults.fullKeyboardTouchpad),
            showMouseButtons = prefs.getBoolean(KEY_SHOW_MOUSE_BUTTONS, defaults.showMouseButtons),
            accent = AccentColor.fromName(prefs.getString(KEY_ACCENT, null)),
            amoledBlack = prefs.getBoolean(KEY_AMOLED, defaults.amoledBlack),
            hostOs = HostOs.fromName(prefs.getString(KEY_HOST_OS, null)),
            airMouseSensitivity = prefs.getFloat(KEY_AIR_MOUSE_SENSITIVITY, defaults.airMouseSensitivity),
            snippets = decodeSnippets(prefs.getString(KEY_SNIPPETS, null)),
            onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, defaults.onboardingDone),
            lastConnectedDeviceAddress = prefs.getString(KEY_LAST_DEVICE, null)
        )
    }

    fun updateSettings(newSettings: UserSettings) {
        if (newSettings == _settings.value) return
        prefs.edit().apply {
            putFloat(KEY_MOUSE_SENSITIVITY, newSettings.mouseSensitivity)
            putFloat(KEY_ACCELERATION, newSettings.accelerationCurve)
            putFloat(KEY_SCROLL_SENSITIVITY, newSettings.scrollSensitivity)
            putBoolean(KEY_NATURAL_SCROLL, newSettings.naturalScroll)
            putBoolean(KEY_SCROLL_INERTIA, newSettings.scrollInertia)
            putBoolean(KEY_TAP_TO_CLICK, newSettings.tapToClick)
            putBoolean(KEY_TAP_TO_DRAG, newSettings.tapToDrag)
            putBoolean(KEY_LEFT_HANDED, newSettings.leftHandedMode)
            putBoolean(KEY_HAPTIC, newSettings.hapticFeedback)
            putBoolean(KEY_KEEP_SCREEN_ON, newSettings.keepScreenOn)
            putBoolean(KEY_AUTO_RECONNECT, newSettings.autoReconnect)
            putBoolean(KEY_STAY_CONNECTED, newSettings.stayConnectedInBackground)
            putBoolean(KEY_LANDSCAPE_KEYBOARD, newSettings.landscapeFullKeyboard)
            putBoolean(KEY_KEYBOARD_TOUCHPAD, newSettings.fullKeyboardTouchpad)
            putBoolean(KEY_SHOW_MOUSE_BUTTONS, newSettings.showMouseButtons)
            putString(KEY_ACCENT, newSettings.accent.name)
            putBoolean(KEY_AMOLED, newSettings.amoledBlack)
            putString(KEY_HOST_OS, newSettings.hostOs.name)
            putFloat(KEY_AIR_MOUSE_SENSITIVITY, newSettings.airMouseSensitivity)
            putString(KEY_SNIPPETS, JSONArray(newSettings.snippets).toString())
            putBoolean(KEY_ONBOARDING_DONE, newSettings.onboardingDone)
            putString(KEY_LAST_DEVICE, newSettings.lastConnectedDeviceAddress)
            apply()
        }
        _settings.value = newSettings
    }

    fun setLastConnectedDevice(address: String) {
        if (_settings.value.lastConnectedDeviceAddress == address) return
        updateSettings(_settings.value.copy(lastConnectedDeviceAddress = address))
    }

    /** Restores every preference to its default but keeps the last host and saved snippets. */
    fun resetToDefaults() {
        val current = _settings.value
        updateSettings(
            UserSettings(
                snippets = current.snippets,
                onboardingDone = current.onboardingDone,
                lastConnectedDeviceAddress = current.lastConnectedDeviceAddress
            )
        )
    }

    private fun decodeSnippets(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { array.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_MOUSE_SENSITIVITY = "mouse_sensitivity"
        private const val KEY_ACCELERATION = "mouse_acceleration"
        private const val KEY_SCROLL_SENSITIVITY = "scroll_sensitivity"
        private const val KEY_NATURAL_SCROLL = "natural_scroll"
        private const val KEY_SCROLL_INERTIA = "scroll_inertia"
        private const val KEY_TAP_TO_CLICK = "tap_to_click"
        private const val KEY_TAP_TO_DRAG = "tap_to_drag"
        private const val KEY_LEFT_HANDED = "left_handed"
        private const val KEY_HAPTIC = "haptic_feedback"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
        private const val KEY_STAY_CONNECTED = "stay_connected_background"
        private const val KEY_LANDSCAPE_KEYBOARD = "landscape_full_keyboard"
        private const val KEY_KEYBOARD_TOUCHPAD = "full_keyboard_touchpad"
        private const val KEY_SHOW_MOUSE_BUTTONS = "show_mouse_buttons"
        private const val KEY_ACCENT = "accent_color"
        private const val KEY_AMOLED = "amoled_black"
        private const val KEY_HOST_OS = "host_os"
        private const val KEY_LAST_DEVICE = "last_device_address"
        private const val KEY_AIR_MOUSE_SENSITIVITY = "air_mouse_sensitivity"
        private const val KEY_SNIPPETS = "snippets"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}
