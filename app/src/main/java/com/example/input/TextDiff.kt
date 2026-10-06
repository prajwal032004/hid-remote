package com.example.input

/**
 * Computes the keystrokes that turn [old] into [new] on the host: one backspace for every
 * character after the common prefix, then the replacement text. This keeps the host in sync
 * with IME autocorrect, swipe typing, voice input and deletions, not just appended characters.
 */
object TextDiff {
    fun keystrokes(old: String, new: String): String {
        var prefix = 0
        val max = minOf(old.length, new.length)
        while (prefix < max && old[prefix] == new[prefix]) prefix++
        val deletions = old.length - prefix
        return buildString(deletions + new.length - prefix) {
            repeat(deletions) { append('\b') }
            append(new, prefix, new.length)
        }
    }
}
