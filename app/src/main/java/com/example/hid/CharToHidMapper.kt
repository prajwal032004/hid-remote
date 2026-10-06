package com.example.hid

/**
 * Translates characters typed from text input or soft keyboard into USB HID KeyCode and Modifier.
 */
object CharToHidMapper {

    data class HidKeyEvent(
        val modifier: Byte,
        val keyCode: Byte
    )

    fun mapChar(char: Char): HidKeyEvent? {
        val shift = HidKeyCodes.MOD_LEFT_SHIFT
        val none = HidKeyCodes.MOD_NONE

        return when (char) {
            in 'a'..'z' -> HidKeyEvent(none, (HidKeyCodes.KEY_A + (char - 'a')).toByte())
            in 'A'..'Z' -> HidKeyEvent(shift, (HidKeyCodes.KEY_A + (char - 'A')).toByte())
            '1' -> HidKeyEvent(none, HidKeyCodes.KEY_1)
            '2' -> HidKeyEvent(none, HidKeyCodes.KEY_2)
            '3' -> HidKeyEvent(none, HidKeyCodes.KEY_3)
            '4' -> HidKeyEvent(none, HidKeyCodes.KEY_4)
            '5' -> HidKeyEvent(none, HidKeyCodes.KEY_5)
            '6' -> HidKeyEvent(none, HidKeyCodes.KEY_6)
            '7' -> HidKeyEvent(none, HidKeyCodes.KEY_7)
            '8' -> HidKeyEvent(none, HidKeyCodes.KEY_8)
            '9' -> HidKeyEvent(none, HidKeyCodes.KEY_9)
            '0' -> HidKeyEvent(none, HidKeyCodes.KEY_0)

            '!' -> HidKeyEvent(shift, HidKeyCodes.KEY_1)
            '@' -> HidKeyEvent(shift, HidKeyCodes.KEY_2)
            '#' -> HidKeyEvent(shift, HidKeyCodes.KEY_3)
            '$' -> HidKeyEvent(shift, HidKeyCodes.KEY_4)
            '%' -> HidKeyEvent(shift, HidKeyCodes.KEY_5)
            '^' -> HidKeyEvent(shift, HidKeyCodes.KEY_6)
            '&' -> HidKeyEvent(shift, HidKeyCodes.KEY_7)
            '*' -> HidKeyEvent(shift, HidKeyCodes.KEY_8)
            '(' -> HidKeyEvent(shift, HidKeyCodes.KEY_9)
            ')' -> HidKeyEvent(shift, HidKeyCodes.KEY_0)

            ' ' -> HidKeyEvent(none, HidKeyCodes.KEY_SPACE)
            '\n' -> HidKeyEvent(none, HidKeyCodes.KEY_ENTER)
            '\t' -> HidKeyEvent(none, HidKeyCodes.KEY_TAB)
            '\b' -> HidKeyEvent(none, HidKeyCodes.KEY_BACKSPACE)

            '-' -> HidKeyEvent(none, HidKeyCodes.KEY_MINUS)
            '_' -> HidKeyEvent(shift, HidKeyCodes.KEY_MINUS)
            '=' -> HidKeyEvent(none, HidKeyCodes.KEY_EQUAL)
            '+' -> HidKeyEvent(shift, HidKeyCodes.KEY_EQUAL)
            '[' -> HidKeyEvent(none, HidKeyCodes.KEY_LEFTBRACE)
            '{' -> HidKeyEvent(shift, HidKeyCodes.KEY_LEFTBRACE)
            ']' -> HidKeyEvent(none, HidKeyCodes.KEY_RIGHTBRACE)
            '}' -> HidKeyEvent(shift, HidKeyCodes.KEY_RIGHTBRACE)
            '\\' -> HidKeyEvent(none, HidKeyCodes.KEY_BACKSLASH)
            '|' -> HidKeyEvent(shift, HidKeyCodes.KEY_BACKSLASH)
            ';' -> HidKeyEvent(none, HidKeyCodes.KEY_SEMICOLON)
            ':' -> HidKeyEvent(shift, HidKeyCodes.KEY_SEMICOLON)
            '\'' -> HidKeyEvent(none, HidKeyCodes.KEY_APOSTROPHE)
            '"' -> HidKeyEvent(shift, HidKeyCodes.KEY_APOSTROPHE)
            '`' -> HidKeyEvent(none, HidKeyCodes.KEY_GRAVE)
            '~' -> HidKeyEvent(shift, HidKeyCodes.KEY_GRAVE)
            ',' -> HidKeyEvent(none, HidKeyCodes.KEY_COMMA)
            '<' -> HidKeyEvent(shift, HidKeyCodes.KEY_COMMA)
            '.' -> HidKeyEvent(none, HidKeyCodes.KEY_DOT)
            '>' -> HidKeyEvent(shift, HidKeyCodes.KEY_DOT)
            '/' -> HidKeyEvent(none, HidKeyCodes.KEY_SLASH)
            '?' -> HidKeyEvent(shift, HidKeyCodes.KEY_SLASH)

            else -> null
        }
    }
}
