package com.example.model

/** Accent colours offered in Settings (ARGB). All are light enough for dark text on top. */
enum class AccentColor(val label: String, val argb: Long) {
    CYAN("Cyan", 0xFF22D3EE),
    BLUE("Blue", 0xFF60A5FA),
    VIOLET("Violet", 0xFFA78BFA),
    ROSE("Rose", 0xFFFB7185),
    AMBER("Amber", 0xFFFBBF24),
    EMERALD("Emerald", 0xFF34D399);

    companion object {
        fun fromName(name: String?): AccentColor = entries.firstOrNull { it.name == name } ?: CYAN
    }
}

data class UserSettings(
    val mouseSensitivity: Float = 1.3f,
    val accelerationCurve: Float = 1.25f,
    val scrollSensitivity: Float = 1.0f,
    val naturalScroll: Boolean = true,
    val scrollInertia: Boolean = true,
    val tapToClick: Boolean = true,
    val tapToDrag: Boolean = true,
    val leftHandedMode: Boolean = false,
    val hapticFeedback: Boolean = true,
    val keepScreenOn: Boolean = true,
    val autoReconnect: Boolean = true,
    /** Keep the host connected after leaving the app. Off = release Bluetooth after a short grace period. */
    val stayConnectedInBackground: Boolean = false,
    /** Rotate to landscape when the full keyboard opens (phones only). */
    val landscapeFullKeyboard: Boolean = true,
    /** Show a touchpad beside the full keyboard when there is room. */
    val fullKeyboardTouchpad: Boolean = true,
    val showMouseButtons: Boolean = true,
    val accent: AccentColor = AccentColor.CYAN,
    val amoledBlack: Boolean = false,
    val hostOs: HostOs = HostOs.WINDOWS,
    /** Air mouse (gyroscope pointer) speed multiplier. */
    val airMouseSensitivity: Float = 1.0f,
    /** Saved phrases typed on the host with one tap from the Type tab. */
    val snippets: List<String> = emptyList(),
    /** False until the welcome screen has been completed once. */
    val onboardingDone: Boolean = false,
    val lastConnectedDeviceAddress: String? = null
)
