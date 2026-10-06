package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.WebAsset
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hid.CharToHidMapper
import com.example.hid.HidConsumerKeys
import com.example.hid.HidKeyCodes
import com.example.model.HostOs
import com.example.model.ShortcutAction
import com.example.model.altKeyLabel
import com.example.model.commandFor
import com.example.model.describe
import com.example.model.superKeyLabel
import com.example.ui.theme.AppTheme
import com.example.viewmodel.ModifierState

private val KEY_GAP = 6.dp
private const val SHIFT = 0x02

/**
 * Compact keyboard panel shown under (portrait) or beside (landscape) the touchpad.
 * Every tab has the same height, so switching tabs is a pure cross-fade with no layout jump.
 */
@Composable
fun RemoteKeyboardSheet(
    modifierState: ModifierState,
    capsLockOn: Boolean,
    hostOs: HostOs,
    onToggleModifier: (Byte) -> Unit,
    onLockModifier: (Byte) -> Unit,
    onClearModifiers: () -> Unit,
    onSendKey: (modifier: Byte, keyCode: Byte) -> Unit,
    onSendConsumerKey: (Int) -> Unit,
    onShortcut: (ShortcutAction) -> Unit,
    onSendText: (String, () -> Unit) -> Unit,
    onStreamText: (old: String, new: String) -> Unit,
    onTypeClipboard: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 46.dp,
    snippets: List<String> = emptyList(),
    onTypeSnippet: (String) -> Unit = {},
    onSaveSnippet: (String) -> Unit = {},
    onDeleteSnippet: (String) -> Unit = {}
) {
    val colors = AppTheme.colors
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val contentHeight = keyHeight * 5 + KEY_GAP * 4

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(28.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("keyboard_panel")
    ) {
        // Header: sticky modifiers, caps state, expand and close
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModifierChip("CTRL", HidKeyCodes.MOD_LEFT_CTRL, modifierState, onToggleModifier, onLockModifier)
                ModifierChip("SHIFT", HidKeyCodes.MOD_LEFT_SHIFT, modifierState, onToggleModifier, onLockModifier)
                ModifierChip(hostOs.altKeyLabel, HidKeyCodes.MOD_LEFT_ALT, modifierState, onToggleModifier, onLockModifier)
                ModifierChip(hostOs.superKeyLabel, HidKeyCodes.MOD_LEFT_GUI, modifierState, onToggleModifier, onLockModifier)
                AnimatedVisibility(
                    visible = modifierState.active != 0,
                    enter = fadeIn() + scaleIn(initialScale = 0.6f),
                    exit = fadeOut() + scaleOut(targetScale = 0.6f)
                ) {
                    IconAction(Icons.Rounded.Close, "Clear modifiers", onClearModifiers, size = 32.dp, tint = colors.warning)
                }
            }
            AnimatedVisibility(visible = capsLockOn, enter = fadeIn(), exit = fadeOut()) {
                TagLabel("CAPS", colors.success, Modifier.padding(horizontal = 6.dp))
            }
            IconAction(Icons.Rounded.OpenInFull, "Full keyboard", onExpand, size = 36.dp)
            Spacer(Modifier.width(6.dp))
            IconAction(
                Icons.Rounded.Close,
                "Close keyboard",
                onClose,
                size = 36.dp,
                modifier = Modifier.testTag("close_keyboard_button")
            )
        }

        Spacer(Modifier.height(10.dp))
        SegmentedControl(
            options = listOf("Keys", "Fn", "Shortcuts", "Type"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            height = 36.dp
        )
        Spacer(Modifier.height(10.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(200, delayMillis = 40)) togetherWith fadeOut(tween(120)) },
            label = "keyboard_tab"
        ) { tab ->
            Box(Modifier.fillMaxWidth().height(contentHeight)) {
                when (tab) {
                    0 -> KeysTab(modifierState, capsLockOn, onToggleModifier, onLockModifier, onSendKey, keyHeight)
                    1 -> FnTab(onSendKey, keyHeight)
                    2 -> ShortcutsTab(hostOs, onSendConsumerKey, onShortcut, keyHeight)
                    else -> DirectTypeTab(
                        onSendText, onStreamText, onTypeClipboard,
                        snippets, onTypeSnippet, onSaveSnippet, onDeleteSnippet
                    )
                }
            }
        }
    }
}

@Composable
private fun ModifierChip(
    label: String,
    bit: Byte,
    state: ModifierState,
    onToggle: (Byte) -> Unit,
    onLock: (Byte) -> Unit
) {
    val mask = bit.toInt() and 0xFF
    val style = when {
        state.locked and mask != 0 -> KeyStyle.LOCKED
        state.oneShot and mask != 0 -> KeyStyle.ONE_SHOT
        else -> KeyStyle.NORMAL
    }
    KeyButton(
        modifier = Modifier.height(32.dp).width(if (label.length > 4) 60.dp else 50.dp),
        style = style,
        cornerRadius = 50.dp,
        description = "$label modifier",
        onLongPress = { onLock(bit) },
        onPress = { onToggle(bit) }
    ) { color ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (style == KeyStyle.LOCKED) {
                Icon(Icons.Rounded.Lock, null, tint = color, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(2.dp))
            }
            Text(label, color = color, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun KeyRow(height: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        content = content
    )
}

private fun sendChar(c: Char, onSendKey: (Byte, Byte) -> Unit) {
    val event = CharToHidMapper.mapChar(c) ?: return
    onSendKey(event.modifier, event.keyCode)
}

@Composable
private fun KeysTab(
    modifierState: ModifierState,
    capsLockOn: Boolean,
    onToggleModifier: (Byte) -> Unit,
    onLockModifier: (Byte) -> Unit,
    onSendKey: (Byte, Byte) -> Unit,
    keyHeight: Dp
) {
    var symbols by rememberSaveable { mutableStateOf(false) }
    val shiftActive = modifierState.active and SHIFT != 0
    val upper = shiftActive != capsLockOn
    val font = if (keyHeight < 40.dp) 13.sp else 16.sp
    val shiftStyle = when {
        modifierState.locked and SHIFT != 0 -> KeyStyle.LOCKED
        modifierState.oneShot and SHIFT != 0 -> KeyStyle.ONE_SHOT
        else -> KeyStyle.ACTION
    }

    Column(verticalArrangement = Arrangement.spacedBy(KEY_GAP)) {
        if (!symbols) {
            KeyRow(keyHeight) {
                val shifted = "!@#$%^&*()"
                "1234567890".forEachIndexed { i, c ->
                    // Shift + digit types the symbol on the host, so show it.
                    TextKey(if (shiftActive) shifted[i].toString() else c.toString(), Modifier.weight(1f), fontSize = font) {
                        sendChar(c, onSendKey)
                    }
                }
            }
            KeyRow(keyHeight) {
                "qwertyuiop".forEach { c -> LetterKey(c, upper, font, onSendKey) }
            }
            KeyRow(keyHeight) {
                Spacer(Modifier.weight(0.5f))
                "asdfghjkl".forEach { c -> LetterKey(c, upper, font, onSendKey) }
                Spacer(Modifier.weight(0.5f))
            }
            KeyRow(keyHeight) {
                IconKey(
                    Icons.Rounded.KeyboardArrowUp, "Shift", Modifier.weight(1.5f), style = shiftStyle,
                    onLongPress = { onLockModifier(HidKeyCodes.MOD_LEFT_SHIFT) }
                ) { onToggleModifier(HidKeyCodes.MOD_LEFT_SHIFT) }
                "zxcvbnm".forEach { c -> LetterKey(c, upper, font, onSendKey) }
                IconKey(Icons.AutoMirrored.Rounded.Backspace, "Backspace", Modifier.weight(1.5f), repeatable = true) {
                    onSendKey(0, HidKeyCodes.KEY_BACKSPACE)
                }
            }
        } else {
            KeyRow(keyHeight) { "!@#$%^&*()".forEach { c -> TextKey(c.toString(), Modifier.weight(1f), fontSize = font) { sendChar(c, onSendKey) } } }
            KeyRow(keyHeight) { "`~-_=+[]{}".forEach { c -> TextKey(c.toString(), Modifier.weight(1f), fontSize = font) { sendChar(c, onSendKey) } } }
            KeyRow(keyHeight) { "\\|;:'\",.<>".forEach { c -> TextKey(c.toString(), Modifier.weight(1f), fontSize = font) { sendChar(c, onSendKey) } } }
            KeyRow(keyHeight) {
                TextKey("Tab", Modifier.weight(1.5f), style = KeyStyle.ACTION, fontSize = font * 0.85f) { onSendKey(0, HidKeyCodes.KEY_TAB) }
                TextKey("/", Modifier.weight(1f), fontSize = font) { sendChar('/', onSendKey) }
                TextKey("?", Modifier.weight(1f), fontSize = font) { sendChar('?', onSendKey) }
                TextKey("Home", Modifier.weight(1.2f), style = KeyStyle.ACTION, fontSize = font * 0.75f) { onSendKey(0, HidKeyCodes.KEY_HOME) }
                TextKey("End", Modifier.weight(1.2f), style = KeyStyle.ACTION, fontSize = font * 0.75f) { onSendKey(0, HidKeyCodes.KEY_END) }
                TextKey("Del", Modifier.weight(1.2f), style = KeyStyle.ACTION, fontSize = font * 0.75f, repeatable = true) { onSendKey(0, HidKeyCodes.KEY_DELETE) }
                IconKey(Icons.AutoMirrored.Rounded.Backspace, "Backspace", Modifier.weight(1.5f), repeatable = true) {
                    onSendKey(0, HidKeyCodes.KEY_BACKSPACE)
                }
            }
        }
        KeyRow(keyHeight) {
            TextKey(if (symbols) "ABC" else "?123", Modifier.weight(1.4f), style = KeyStyle.ACTION, fontSize = font * 0.8f) { symbols = !symbols }
            TextKey("Esc", Modifier.weight(1.1f), style = KeyStyle.ACTION, fontSize = font * 0.8f) { onSendKey(0, HidKeyCodes.KEY_ESCAPE) }
            TextKey("space", Modifier.weight(3.2f), fontSize = font * 0.8f, repeatable = true) { onSendKey(0, HidKeyCodes.KEY_SPACE) }
            IconKey(Icons.AutoMirrored.Rounded.ArrowBack, "Left", Modifier.weight(1f), iconSize = 17.dp, repeatable = true) { onSendKey(0, HidKeyCodes.KEY_LEFT) }
            IconKey(Icons.Rounded.ArrowUpward, "Up", Modifier.weight(1f), iconSize = 17.dp, repeatable = true) { onSendKey(0, HidKeyCodes.KEY_UP) }
            IconKey(Icons.Rounded.ArrowDownward, "Down", Modifier.weight(1f), iconSize = 17.dp, repeatable = true) { onSendKey(0, HidKeyCodes.KEY_DOWN) }
            IconKey(Icons.AutoMirrored.Rounded.ArrowForward, "Right", Modifier.weight(1f), iconSize = 17.dp, repeatable = true) { onSendKey(0, HidKeyCodes.KEY_RIGHT) }
            IconKey(Icons.AutoMirrored.Rounded.KeyboardReturn, "Enter", Modifier.weight(1.6f), style = KeyStyle.ACCENT) { onSendKey(0, HidKeyCodes.KEY_ENTER) }
        }
    }
}

@Composable
private fun RowScope.LetterKey(c: Char, upper: Boolean, font: androidx.compose.ui.unit.TextUnit, onSendKey: (Byte, Byte) -> Unit) {
    // Always send the plain key; the host applies Shift/Caps itself, exactly like hardware.
    TextKey(if (upper) c.uppercaseChar().toString() else c.toString(), Modifier.weight(1f), fontSize = font) {
        sendChar(c, onSendKey)
    }
}

@Composable
private fun FnTab(onSendKey: (Byte, Byte) -> Unit, keyHeight: Dp) {
    val font = if (keyHeight < 40.dp) 12.sp else 14.sp
    fun key(code: Byte): () -> Unit = { onSendKey(0, code) }
    Column(verticalArrangement = Arrangement.spacedBy(KEY_GAP)) {
        KeyRow(keyHeight) {
            listOf(HidKeyCodes.KEY_F1, HidKeyCodes.KEY_F2, HidKeyCodes.KEY_F3, HidKeyCodes.KEY_F4, HidKeyCodes.KEY_F5, HidKeyCodes.KEY_F6)
                .forEachIndexed { i, code -> TextKey("F${i + 1}", Modifier.weight(1f), style = KeyStyle.ACTION, fontSize = font, onPress = key(code)) }
        }
        KeyRow(keyHeight) {
            listOf(HidKeyCodes.KEY_F7, HidKeyCodes.KEY_F8, HidKeyCodes.KEY_F9, HidKeyCodes.KEY_F10, HidKeyCodes.KEY_F11, HidKeyCodes.KEY_F12)
                .forEachIndexed { i, code -> TextKey("F${i + 7}", Modifier.weight(1f), style = KeyStyle.ACTION, fontSize = font, onPress = key(code)) }
        }
        KeyRow(keyHeight) {
            TextKey("Ins", Modifier.weight(1f), fontSize = font, onPress = key(HidKeyCodes.KEY_INSERT))
            TextKey("Home", Modifier.weight(1f), fontSize = font, onPress = key(HidKeyCodes.KEY_HOME))
            TextKey("PgUp", Modifier.weight(1f), fontSize = font, repeatable = true, onPress = key(HidKeyCodes.KEY_PAGEUP))
            TextKey("Del", Modifier.weight(1f), fontSize = font, repeatable = true, onPress = key(HidKeyCodes.KEY_DELETE))
            TextKey("End", Modifier.weight(1f), fontSize = font, onPress = key(HidKeyCodes.KEY_END))
            TextKey("PgDn", Modifier.weight(1f), fontSize = font, repeatable = true, onPress = key(HidKeyCodes.KEY_PAGEDOWN))
        }
        KeyRow(keyHeight) {
            TextKey("Esc", Modifier.weight(1f), style = KeyStyle.ACTION, fontSize = font, onPress = key(HidKeyCodes.KEY_ESCAPE))
            TextKey("Tab", Modifier.weight(1f), style = KeyStyle.ACTION, fontSize = font, onPress = key(HidKeyCodes.KEY_TAB))
            TextKey("Caps", Modifier.weight(1f), style = KeyStyle.ACTION, fontSize = font, onPress = key(HidKeyCodes.KEY_CAPSLOCK))
            TextKey("PrtSc", Modifier.weight(1f), style = KeyStyle.ACTION, fontSize = font, onPress = key(HidKeyCodes.KEY_SYSRQ))
            TextKey("ScrLk", Modifier.weight(1f), style = KeyStyle.ACTION, fontSize = font, onPress = key(HidKeyCodes.KEY_SCROLLLOCK))
            TextKey("Pause", Modifier.weight(1f), style = KeyStyle.ACTION, fontSize = font, onPress = key(HidKeyCodes.KEY_PAUSE))
        }
        KeyRow(keyHeight) {
            TextKey("Menu", Modifier.weight(1f), style = KeyStyle.ACTION, fontSize = font, onPress = key(HidKeyCodes.KEY_APPLICATION))
            IconKey(Icons.AutoMirrored.Rounded.ArrowBack, "Left", Modifier.weight(1f), repeatable = true, onPress = key(HidKeyCodes.KEY_LEFT))
            IconKey(Icons.Rounded.ArrowUpward, "Up", Modifier.weight(1f), repeatable = true, onPress = key(HidKeyCodes.KEY_UP))
            IconKey(Icons.Rounded.ArrowDownward, "Down", Modifier.weight(1f), repeatable = true, onPress = key(HidKeyCodes.KEY_DOWN))
            IconKey(Icons.AutoMirrored.Rounded.ArrowForward, "Right", Modifier.weight(1f), repeatable = true, onPress = key(HidKeyCodes.KEY_RIGHT))
            IconKey(Icons.AutoMirrored.Rounded.KeyboardReturn, "Enter", Modifier.weight(1f), style = KeyStyle.ACCENT, onPress = key(HidKeyCodes.KEY_ENTER))
        }
    }
}

/** Icon for each shortcut tile. */
private fun ShortcutAction.icon(): ImageVector = when (this) {
    ShortcutAction.COPY -> Icons.Rounded.ContentCopy
    ShortcutAction.PASTE -> Icons.Rounded.ContentPaste
    ShortcutAction.CUT -> Icons.Rounded.ContentCut
    ShortcutAction.UNDO -> Icons.AutoMirrored.Rounded.Undo
    ShortcutAction.REDO -> Icons.AutoMirrored.Rounded.Redo
    ShortcutAction.SELECT_ALL -> Icons.Rounded.SelectAll
    ShortcutAction.FIND, ShortcutAction.SEARCH -> Icons.Rounded.Search
    ShortcutAction.SAVE -> Icons.Rounded.Save
    ShortcutAction.NEW_TAB -> Icons.Rounded.Add
    ShortcutAction.CLOSE_TAB -> Icons.Rounded.Close
    ShortcutAction.REOPEN_TAB -> Icons.Rounded.Tab
    ShortcutAction.REFRESH -> Icons.Rounded.Refresh
    ShortcutAction.ZOOM_IN -> Icons.Rounded.ZoomIn
    ShortcutAction.ZOOM_OUT -> Icons.Rounded.ZoomOut
    ShortcutAction.SWITCH_APP -> Icons.Rounded.SwapHoriz
    ShortcutAction.SHOW_DESKTOP -> Icons.Rounded.DesktopWindows
    ShortcutAction.CLOSE_WINDOW -> Icons.Rounded.WebAsset
    ShortcutAction.LOCK_SCREEN -> Icons.Rounded.Lock
    ShortcutAction.SCREENSHOT -> Icons.Rounded.Screenshot
    ShortcutAction.TASK_MANAGER -> Icons.Rounded.Dashboard
    ShortcutAction.FILE_EXPLORER -> Icons.Rounded.Folder
    else -> Icons.Rounded.GridView
}

private val TILE_ACTIONS = listOf(
    ShortcutAction.COPY, ShortcutAction.PASTE, ShortcutAction.CUT, ShortcutAction.UNDO,
    ShortcutAction.REDO, ShortcutAction.SELECT_ALL, ShortcutAction.FIND, ShortcutAction.SAVE,
    ShortcutAction.SWITCH_APP, ShortcutAction.SHOW_DESKTOP, ShortcutAction.TASK_VIEW, ShortcutAction.SEARCH,
    ShortcutAction.NEW_TAB, ShortcutAction.CLOSE_TAB, ShortcutAction.REOPEN_TAB, ShortcutAction.REFRESH,
    ShortcutAction.ZOOM_IN, ShortcutAction.ZOOM_OUT, ShortcutAction.SCREENSHOT, ShortcutAction.FILE_EXPLORER,
    ShortcutAction.TASK_MANAGER, ShortcutAction.CLOSE_WINDOW, ShortcutAction.LOCK_SCREEN
)

@Composable
private fun ShortcutsTab(
    hostOs: HostOs,
    onSendConsumerKey: (Int) -> Unit,
    onShortcut: (ShortcutAction) -> Unit,
    keyHeight: Dp
) {
    val colors = AppTheme.colors
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = (maxWidth / 96.dp).toInt().coerceIn(3, 8)
        val actions = TILE_ACTIONS.filter { hostOs.commandFor(it) != null }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KEY_GAP)
        ) {
            KeyRow(keyHeight) {
                IconKey(Icons.AutoMirrored.Rounded.VolumeOff, "Mute", Modifier.weight(1f)) { onSendConsumerKey(HidConsumerKeys.MUTE) }
                IconKey(Icons.AutoMirrored.Rounded.VolumeDown, "Volume down", Modifier.weight(1f), repeatable = true) { onSendConsumerKey(HidConsumerKeys.VOLUME_DOWN) }
                IconKey(Icons.AutoMirrored.Rounded.VolumeUp, "Volume up", Modifier.weight(1f), repeatable = true) { onSendConsumerKey(HidConsumerKeys.VOLUME_UP) }
                IconKey(Icons.Rounded.SkipPrevious, "Previous track", Modifier.weight(1f)) { onSendConsumerKey(HidConsumerKeys.PREV_TRACK) }
                IconKey(Icons.Rounded.PlayArrow, "Play or pause", Modifier.weight(1f), style = KeyStyle.ACCENT) { onSendConsumerKey(HidConsumerKeys.PLAY_PAUSE) }
                IconKey(Icons.Rounded.SkipNext, "Next track", Modifier.weight(1f)) { onSendConsumerKey(HidConsumerKeys.NEXT_TRACK) }
            }
            actions.chunked(columns).forEach { rowActions ->
                Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                    rowActions.forEach { action ->
                        ShortcutTile(
                            label = action.label,
                            combo = hostOs.describe(action),
                            icon = action.icon(),
                            modifier = Modifier.weight(1f),
                            onClick = { onShortcut(action) }
                        )
                    }
                    repeat(columns - rowActions.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Text(
                "Combos shown for ${hostOs.label}. Change the host system in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
private fun ShortcutTile(label: String, combo: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceHigh)
            .border(1.dp, colors.outline, RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = colors.text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(combo, color = colors.textMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DirectTypeTab(
    onSendText: (String, () -> Unit) -> Unit,
    onStreamText: (old: String, new: String) -> Unit,
    onTypeClipboard: () -> Unit,
    snippets: List<String>,
    onTypeSnippet: (String) -> Unit,
    onSaveSnippet: (String) -> Unit,
    onDeleteSnippet: (String) -> Unit
) {
    val colors = AppTheme.colors
    var textInput by rememberSaveable { mutableStateOf("") }
    var isSending by rememberSaveable { mutableStateOf(false) }
    var liveTyping by rememberSaveable { mutableStateOf(false) }

    fun sendBatch() {
        if (textInput.isEmpty() || isSending) return
        isSending = true
        onSendText(textInput) {
            textInput = ""
            isSending = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Live typing", color = colors.text, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (liveTyping) "Every change, including autocorrect and voice, appears on the host instantly."
                    else "Compose here, then send in one go. Works with voice typing.",
                    color = colors.textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = liveTyping,
                onCheckedChange = {
                    liveTyping = it
                    textInput = ""
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onAccent,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedTrackColor = colors.surfaceHigh,
                    uncheckedBorderColor = colors.outlineStrong
                ),
                modifier = Modifier.testTag("live_typing_switch")
            )
        }

        OutlinedTextField(
            value = textInput,
            onValueChange = { newVal ->
                if (liveTyping) onStreamText(textInput, newVal)
                textInput = newVal
            },
            placeholder = {
                Text(
                    if (liveTyping) "Start typing. It appears on the host." else "Type or dictate text…",
                    color = colors.textMuted
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("text_input_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceHigh,
                unfocusedContainerColor = colors.surfaceHigh,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.outline,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                cursorColor = colors.accent
            ),
            shape = RoundedCornerShape(16.dp),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = if (liveTyping) ImeAction.Done else ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = { sendBatch() },
                onDone = {
                    // Enter in live mode goes to the host and starts a fresh line locally.
                    onStreamText("", "\n")
                    textInput = ""
                }
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!liveTyping) {
                PrimaryButton(
                    text = if (isSending) "Sending…" else "Send",
                    icon = Icons.AutoMirrored.Rounded.Send,
                    onClick = { sendBatch() },
                    enabled = textInput.isNotEmpty() && !isSending,
                    modifier = Modifier.weight(1f).testTag("send_text_button")
                )
            }
            PrimaryButton(
                text = "Type clipboard",
                icon = Icons.Rounded.ContentPaste,
                onClick = onTypeClipboard,
                tonal = true,
                modifier = Modifier.weight(1f)
            )
            AnimatedVisibility(visible = textInput.isNotBlank() && !liveTyping, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                IconAction(Icons.Rounded.BookmarkAdd, "Save as snippet", { onSaveSnippet(textInput) })
            }
            AnimatedVisibility(visible = textInput.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                // Clears the local field only; nothing is deleted on the host.
                IconAction(Icons.Rounded.Close, "Clear field", { textInput = "" })
            }
        }

        SnippetsSection(snippets, onTypeSnippet, onDeleteSnippet)
    }
}

/** Saved phrases: tap to type on the host, long-press to delete. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SnippetsSection(
    snippets: List<String>,
    onTypeSnippet: (String) -> Unit,
    onDeleteSnippet: (String) -> Unit
) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Icon(Icons.Rounded.Bookmarks, null, tint = colors.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Snippets", color = colors.text, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(8.dp))
        Text(
            if (snippets.isEmpty()) "Type something above and tap the bookmark to save it"
            else "Tap to type · long-press to delete",
            color = colors.textMuted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (snippets.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.testTag("snippets_row")
        ) {
            snippets.forEach { snippet ->
                Text(
                    snippet.replace('\n', '⏎'),
                    color = colors.text,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.surfaceHigh)
                        .border(1.dp, colors.outline, RoundedCornerShape(50))
                        .combinedClickable(
                            role = Role.Button,
                            onLongClick = { onDeleteSnippet(snippet) },
                            onClick = { onTypeSnippet(snippet) }
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
