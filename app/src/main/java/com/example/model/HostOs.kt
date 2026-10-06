package com.example.model

import com.example.hid.HidConsumerKeys
import com.example.hid.HidKeyCodes

/** The operating system on the receiving end; shortcuts and gestures adapt to it. */
enum class HostOs(val label: String, val shortLabel: String) {
    WINDOWS("Windows", "Windows"),
    MAC("macOS", "macOS"),
    LINUX("Linux", "Linux"),
    ANDROID("Android / TV", "Android/TV");

    companion object {
        fun fromName(name: String?): HostOs = entries.firstOrNull { it.name == name } ?: WINDOWS
    }
}

enum class ShortcutAction(val label: String) {
    COPY("Copy"), PASTE("Paste"), CUT("Cut"), UNDO("Undo"), REDO("Redo"), SELECT_ALL("Select all"),
    FIND("Find"), SAVE("Save"),
    NEW_TAB("New tab"), CLOSE_TAB("Close tab"), REOPEN_TAB("Reopen tab"), REFRESH("Refresh"),
    ZOOM_IN("Zoom in"), ZOOM_OUT("Zoom out"),
    SWITCH_APP("Switch app"), SHOW_DESKTOP("Desktop"), CLOSE_WINDOW("Close window"), LOCK_SCREEN("Lock"),
    SEARCH("Search"), SCREENSHOT("Screenshot"), TASK_MANAGER("Task manager"), FILE_EXPLORER("Files"),
    TASK_VIEW("Task view"), DESKTOP_LEFT("Desktop left"), DESKTOP_RIGHT("Desktop right"),

    // Remote / presentation
    HOME("Home"), BACK("Back"), MENU("Menu"),
    SLIDE_NEXT("Next slide"), SLIDE_PREV("Previous slide"), SLIDE_START("Start show"),
    SLIDE_BLANK("Blank screen"), SLIDE_END("End show")
}

sealed interface HostCommand {
    data class Key(val modifiers: Int, val keyCode: Byte) : HostCommand
    data class Consumer(val usage: Int) : HostCommand
}

private const val CTRL = HidKeyCodes.MOD_LEFT_CTRL.toInt()
private const val SHIFT = HidKeyCodes.MOD_LEFT_SHIFT.toInt()
private const val ALT = HidKeyCodes.MOD_LEFT_ALT.toInt()
private const val GUI = HidKeyCodes.MOD_LEFT_GUI.toInt()

/** Returns the native key combination for [action] on this host, or null if it has none. */
fun HostOs.commandFor(action: ShortcutAction): HostCommand? {
    val mac = this == HostOs.MAC
    val primary = if (mac) GUI else CTRL
    fun key(mods: Int, code: Byte) = HostCommand.Key(mods, code)
    fun consumer(usage: Int) = HostCommand.Consumer(usage)

    return when (action) {
        ShortcutAction.COPY -> key(primary, HidKeyCodes.KEY_C)
        ShortcutAction.PASTE -> key(primary, HidKeyCodes.KEY_V)
        ShortcutAction.CUT -> key(primary, HidKeyCodes.KEY_X)
        ShortcutAction.UNDO -> key(primary, HidKeyCodes.KEY_Z)
        ShortcutAction.REDO -> if (this == HostOs.WINDOWS) key(CTRL, HidKeyCodes.KEY_Y) else key(primary or SHIFT, HidKeyCodes.KEY_Z)
        ShortcutAction.SELECT_ALL -> key(primary, HidKeyCodes.KEY_A)
        ShortcutAction.FIND -> key(primary, HidKeyCodes.KEY_F)
        ShortcutAction.SAVE -> key(primary, HidKeyCodes.KEY_S)
        ShortcutAction.NEW_TAB -> key(primary, HidKeyCodes.KEY_T)
        ShortcutAction.CLOSE_TAB -> key(primary, HidKeyCodes.KEY_W)
        ShortcutAction.REOPEN_TAB -> key(primary or SHIFT, HidKeyCodes.KEY_T)
        ShortcutAction.REFRESH -> when (this) {
            HostOs.WINDOWS, HostOs.LINUX -> key(0, HidKeyCodes.KEY_F5)
            HostOs.MAC, HostOs.ANDROID -> key(primary, HidKeyCodes.KEY_R)
        }
        ShortcutAction.ZOOM_IN -> key(primary, HidKeyCodes.KEY_EQUAL)
        ShortcutAction.ZOOM_OUT -> key(primary, HidKeyCodes.KEY_MINUS)
        ShortcutAction.SWITCH_APP -> key(if (mac) GUI else ALT, HidKeyCodes.KEY_TAB)
        ShortcutAction.SHOW_DESKTOP -> when (this) {
            HostOs.WINDOWS, HostOs.LINUX -> key(GUI, HidKeyCodes.KEY_D)
            HostOs.MAC -> key(0, HidKeyCodes.KEY_F11)
            HostOs.ANDROID -> consumer(HidConsumerKeys.AC_HOME)
        }
        ShortcutAction.CLOSE_WINDOW -> when (this) {
            HostOs.WINDOWS, HostOs.LINUX -> key(ALT, HidKeyCodes.KEY_F4)
            HostOs.MAC -> key(GUI, HidKeyCodes.KEY_W)
            HostOs.ANDROID -> consumer(HidConsumerKeys.AC_BACK)
        }
        ShortcutAction.LOCK_SCREEN -> when (this) {
            HostOs.WINDOWS, HostOs.LINUX -> key(GUI, HidKeyCodes.KEY_L)
            HostOs.MAC -> key(CTRL or GUI, HidKeyCodes.KEY_Q)
            HostOs.ANDROID -> null
        }
        ShortcutAction.SEARCH -> when (this) {
            HostOs.WINDOWS -> key(GUI, HidKeyCodes.KEY_S)
            HostOs.MAC -> key(GUI, HidKeyCodes.KEY_SPACE)
            HostOs.LINUX -> key(GUI, HidKeyCodes.NONE)
            HostOs.ANDROID -> consumer(HidConsumerKeys.AC_SEARCH)
        }
        ShortcutAction.SCREENSHOT -> when (this) {
            HostOs.WINDOWS -> key(GUI or SHIFT, HidKeyCodes.KEY_S)
            HostOs.MAC -> key(GUI or SHIFT, HidKeyCodes.KEY_4)
            HostOs.LINUX, HostOs.ANDROID -> key(0, HidKeyCodes.KEY_SYSRQ)
        }
        ShortcutAction.TASK_MANAGER -> when (this) {
            HostOs.WINDOWS -> key(CTRL or SHIFT, HidKeyCodes.KEY_ESCAPE)
            HostOs.MAC -> key(GUI or ALT, HidKeyCodes.KEY_ESCAPE)
            HostOs.LINUX, HostOs.ANDROID -> null
        }
        ShortcutAction.FILE_EXPLORER -> when (this) {
            HostOs.WINDOWS, HostOs.LINUX -> key(GUI, HidKeyCodes.KEY_E)
            HostOs.MAC -> key(GUI or ALT, HidKeyCodes.KEY_SPACE)
            HostOs.ANDROID -> null
        }
        ShortcutAction.TASK_VIEW -> when (this) {
            HostOs.WINDOWS -> key(GUI, HidKeyCodes.KEY_TAB)
            HostOs.MAC -> key(CTRL, HidKeyCodes.KEY_UP)
            HostOs.LINUX -> key(GUI, HidKeyCodes.NONE)
            HostOs.ANDROID -> key(ALT, HidKeyCodes.KEY_TAB)
        }
        ShortcutAction.DESKTOP_LEFT -> when (this) {
            HostOs.WINDOWS -> key(CTRL or GUI, HidKeyCodes.KEY_LEFT)
            HostOs.MAC -> key(CTRL, HidKeyCodes.KEY_LEFT)
            HostOs.LINUX -> key(CTRL or ALT, HidKeyCodes.KEY_LEFT)
            HostOs.ANDROID -> null
        }
        ShortcutAction.DESKTOP_RIGHT -> when (this) {
            HostOs.WINDOWS -> key(CTRL or GUI, HidKeyCodes.KEY_RIGHT)
            HostOs.MAC -> key(CTRL, HidKeyCodes.KEY_RIGHT)
            HostOs.LINUX -> key(CTRL or ALT, HidKeyCodes.KEY_RIGHT)
            HostOs.ANDROID -> null
        }
        ShortcutAction.HOME -> when (this) {
            HostOs.WINDOWS, HostOs.LINUX -> key(GUI, HidKeyCodes.NONE)
            HostOs.MAC -> key(GUI, HidKeyCodes.KEY_SPACE)
            HostOs.ANDROID -> consumer(HidConsumerKeys.AC_HOME)
        }
        ShortcutAction.BACK -> when (this) {
            HostOs.ANDROID -> consumer(HidConsumerKeys.AC_BACK)
            else -> key(0, HidKeyCodes.KEY_ESCAPE)
        }
        ShortcutAction.MENU -> when (this) {
            HostOs.ANDROID -> consumer(HidConsumerKeys.MENU)
            HostOs.MAC -> null
            else -> key(0, HidKeyCodes.KEY_APPLICATION)
        }
        ShortcutAction.SLIDE_NEXT -> key(0, HidKeyCodes.KEY_PAGEDOWN)
        ShortcutAction.SLIDE_PREV -> key(0, HidKeyCodes.KEY_PAGEUP)
        ShortcutAction.SLIDE_START -> when (this) {
            HostOs.MAC -> key(GUI or ALT, HidKeyCodes.KEY_P)
            else -> key(0, HidKeyCodes.KEY_F5)
        }
        ShortcutAction.SLIDE_BLANK -> key(0, HidKeyCodes.KEY_B)
        ShortcutAction.SLIDE_END -> key(0, HidKeyCodes.KEY_ESCAPE)
    }
}

/** Label for the "super" modifier chip on this host. */
val HostOs.superKeyLabel: String
    get() = when (this) {
        HostOs.WINDOWS -> "WIN"
        HostOs.MAC -> "CMD"
        HostOs.LINUX -> "SUPER"
        HostOs.ANDROID -> "META"
    }

/** Label for the Alt modifier on this host. */
val HostOs.altKeyLabel: String
    get() = if (this == HostOs.MAC) "OPT" else "ALT"

/** Human-readable key combo for [action] on this host, e.g. "Ctrl + C" or "⌘ C". */
fun HostOs.describe(action: ShortcutAction): String = when (val cmd = commandFor(action)) {
    null -> "Unavailable"
    is HostCommand.Consumer -> when (cmd.usage) {
        HidConsumerKeys.AC_HOME -> "Home"
        HidConsumerKeys.AC_BACK -> "Back"
        HidConsumerKeys.AC_SEARCH -> "Search"
        HidConsumerKeys.MENU -> "Menu"
        else -> "Media key"
    }
    is HostCommand.Key -> {
        val mac = this == HostOs.MAC
        val parts = buildList {
            if (cmd.modifiers and CTRL != 0) add(if (mac) "⌃" else "Ctrl")
            if (cmd.modifiers and ALT != 0) add(if (mac) "⌥" else "Alt")
            if (cmd.modifiers and SHIFT != 0) add(if (mac) "⇧" else "Shift")
            if (cmd.modifiers and GUI != 0) add(if (mac) "⌘" else superKeyLabel.lowercase().replaceFirstChar { it.uppercase() })
            keyName(cmd.keyCode)?.let { add(it) }
        }
        parts.joinToString(if (mac) " " else " + ")
    }
}

private fun keyName(code: Byte): String? = when (code) {
    HidKeyCodes.NONE -> null
    in HidKeyCodes.KEY_A..HidKeyCodes.KEY_Z -> ('A' + (code - HidKeyCodes.KEY_A)).toString()
    in HidKeyCodes.KEY_1..HidKeyCodes.KEY_9 -> ('1' + (code - HidKeyCodes.KEY_1)).toString()
    HidKeyCodes.KEY_0 -> "0"
    in HidKeyCodes.KEY_F1..HidKeyCodes.KEY_F12 -> "F${code - HidKeyCodes.KEY_F1 + 1}"
    HidKeyCodes.KEY_TAB -> "Tab"
    HidKeyCodes.KEY_ESCAPE -> "Esc"
    HidKeyCodes.KEY_SPACE -> "Space"
    HidKeyCodes.KEY_ENTER -> "Enter"
    HidKeyCodes.KEY_EQUAL -> "="
    HidKeyCodes.KEY_MINUS -> "−"
    HidKeyCodes.KEY_SYSRQ -> "PrtSc"
    HidKeyCodes.KEY_PAGEUP -> "PgUp"
    HidKeyCodes.KEY_PAGEDOWN -> "PgDn"
    HidKeyCodes.KEY_APPLICATION -> "Menu"
    HidKeyCodes.KEY_LEFT -> "←"
    HidKeyCodes.KEY_RIGHT -> "→"
    HidKeyCodes.KEY_UP -> "↑"
    HidKeyCodes.KEY_DOWN -> "↓"
    else -> "Key"
}
