package ve.com.sagues.rafael.android_mouse

import java.util.Locale

/**
 * Layout of the host computer's physical keyboard.
 *
 * HID keyboard reports carry physical key positions, not characters, so the same
 * report produces different letters on QWERTY, AZERTY and QWERTZ hosts. The user
 * selects which layout the host uses and characters are translated accordingly.
 */
enum class HostKeyboardLayout(val label: String, val hint: String) {
    QWERTY("QWERTY", "US / International"),
    AZERTY("AZERTY", "French / Belgian"),
    QWERTZ("QWERTZ", "German / Swiss");

    companion object {
        fun fromStorage(value: String?): HostKeyboardLayout =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: systemDefault()

        fun systemDefault(): HostKeyboardLayout {
            val locale = Locale.getDefault()
            return when {
                locale.language.equals("fr", ignoreCase = true) &&
                    !locale.country.equals("CH", ignoreCase = true) -> AZERTY
                locale.language.equals("de", ignoreCase = true) -> QWERTZ
                locale.country.equals("CH", ignoreCase = true) -> QWERTZ
                else -> QWERTY
            }
        }
    }
}

/** One key press (press + release) with optional modifier keys. */
data class HidKeystroke(val usage: Int, val modifiers: Int = 0)

private const val SHIFT = MouseConfig.MOD_LEFT_SHIFT
private const val ALT_GR = MouseConfig.MOD_RIGHT_ALT

private fun stroke(usage: Int, shift: Boolean = false, altGr: Boolean = false): HidKeystroke =
    HidKeystroke(usage, (if (shift) SHIFT else 0) or (if (altGr) ALT_GR else 0))

/**
 * Translates [char] into the physical keystrokes that produce it on this layout.
 * Returns an empty list for characters the layout cannot type directly.
 */
fun HostKeyboardLayout.keystrokesFor(char: Char): List<HidKeystroke> = when (this) {
    HostKeyboardLayout.QWERTY -> qwertyKeystrokes(char)
    HostKeyboardLayout.AZERTY -> azertyKeystrokes(char)
    HostKeyboardLayout.QWERTZ -> qwertzKeystrokes(char)
}

// -------------------- QWERTY (US) --------------------

private fun qwertyKeystrokes(char: Char): List<HidKeystroke> = when (char) {
    in 'a'..'z' -> listOf(stroke(0x04 + (char - 'a')))
    in 'A'..'Z' -> listOf(stroke(0x04 + (char - 'A'), shift = true))
    in '1'..'9' -> listOf(stroke(0x1E + (char - '1')))
    '0' -> listOf(stroke(0x27))
    ' ', '\u00A0', '\u202F' -> listOf(stroke(MouseConfig.KEY_SPACE))
    '\'', '‘', '’' -> listOf(stroke(0x34))
    '"', '“', '”' -> listOf(stroke(0x34, shift = true))
    '-' -> listOf(stroke(0x2D))
    '_' -> listOf(stroke(0x2D, shift = true))
    '=' -> listOf(stroke(0x2E))
    '+' -> listOf(stroke(0x2E, shift = true))
    '[' -> listOf(stroke(0x2F))
    '{' -> listOf(stroke(0x2F, shift = true))
    ']' -> listOf(stroke(0x30))
    '}' -> listOf(stroke(0x30, shift = true))
    '\\' -> listOf(stroke(0x31))
    '|' -> listOf(stroke(0x31, shift = true))
    ';' -> listOf(stroke(0x33))
    ':' -> listOf(stroke(0x33, shift = true))
    '`' -> listOf(stroke(0x35))
    '~' -> listOf(stroke(0x35, shift = true))
    ',' -> listOf(stroke(0x36))
    '<' -> listOf(stroke(0x36, shift = true))
    '.' -> listOf(stroke(0x37))
    '>' -> listOf(stroke(0x37, shift = true))
    '/' -> listOf(stroke(0x38))
    '?' -> listOf(stroke(0x38, shift = true))
    '!' -> listOf(stroke(0x1E, shift = true))
    '@' -> listOf(stroke(0x1F, shift = true))
    '#' -> listOf(stroke(0x20, shift = true))
    '$' -> listOf(stroke(0x21, shift = true))
    '%' -> listOf(stroke(0x22, shift = true))
    '^' -> listOf(stroke(0x23, shift = true))
    '&' -> listOf(stroke(0x24, shift = true))
    '*' -> listOf(stroke(0x25, shift = true))
    '(' -> listOf(stroke(0x26, shift = true))
    ')' -> listOf(stroke(0x27, shift = true))
    else -> emptyList()
}

// -------------------- AZERTY (French) --------------------

private fun azertyLetter(char: Char): Int = when (char) {
    'a' -> 0x14
    'q' -> 0x04
    'z' -> 0x1A
    'w' -> 0x1D
    'm' -> 0x33
    else -> 0x04 + (char - 'a')
}

private fun azertyLetterStroke(char: Char, uppercase: Boolean): HidKeystroke =
    stroke(azertyLetter(char), shift = uppercase)

/** Dead circumflex (^) followed by the vowel. */
private fun azertyCircumflex(char: Char, uppercase: Boolean): List<HidKeystroke> =
    listOf(stroke(0x2F), azertyLetterStroke(char, uppercase))

/** Dead diaeresis (¨) followed by the vowel. */
private fun azertyDiaeresis(char: Char, uppercase: Boolean): List<HidKeystroke> =
    listOf(stroke(0x2F, shift = true), azertyLetterStroke(char, uppercase))

/** Some accented capitals are typed with Caps Lock on a French layout. */
private fun azertyCapsAccented(usage: Int): List<HidKeystroke> = listOf(
    stroke(MouseConfig.KEY_CAPS_LOCK),
    stroke(usage),
    stroke(MouseConfig.KEY_CAPS_LOCK)
)

private fun azertyKeystrokes(char: Char): List<HidKeystroke> = when (char) {
    in 'a'..'z' -> listOf(azertyLetterStroke(char, uppercase = false))
    in 'A'..'Z' -> listOf(azertyLetterStroke(char.lowercaseChar(), uppercase = true))

    'é' -> listOf(stroke(0x1F))
    'è' -> listOf(stroke(0x24))
    'à' -> listOf(stroke(0x27))
    'ç' -> listOf(stroke(0x26))
    'ù' -> listOf(stroke(0x34))
    'É' -> azertyCapsAccented(0x1F)
    'È' -> azertyCapsAccented(0x24)
    'À' -> azertyCapsAccented(0x27)
    'Ç' -> azertyCapsAccented(0x26)
    'Ù' -> azertyCapsAccented(0x34)

    'â' -> azertyCircumflex('a', uppercase = false)
    'ê' -> azertyCircumflex('e', uppercase = false)
    'î' -> azertyCircumflex('i', uppercase = false)
    'ô' -> azertyCircumflex('o', uppercase = false)
    'û' -> azertyCircumflex('u', uppercase = false)
    'Â' -> azertyCircumflex('a', uppercase = true)
    'Ê' -> azertyCircumflex('e', uppercase = true)
    'Î' -> azertyCircumflex('i', uppercase = true)
    'Ô' -> azertyCircumflex('o', uppercase = true)
    'Û' -> azertyCircumflex('u', uppercase = true)

    'ë' -> azertyDiaeresis('e', uppercase = false)
    'ï' -> azertyDiaeresis('i', uppercase = false)
    'ü' -> azertyDiaeresis('u', uppercase = false)
    'ÿ' -> azertyDiaeresis('y', uppercase = false)
    'Ë' -> azertyDiaeresis('e', uppercase = true)
    'Ï' -> azertyDiaeresis('i', uppercase = true)
    'Ü' -> azertyDiaeresis('u', uppercase = true)

    '1' -> listOf(stroke(0x1E, shift = true))
    '2' -> listOf(stroke(0x1F, shift = true))
    '3' -> listOf(stroke(0x20, shift = true))
    '4' -> listOf(stroke(0x21, shift = true))
    '5' -> listOf(stroke(0x22, shift = true))
    '6' -> listOf(stroke(0x23, shift = true))
    '7' -> listOf(stroke(0x24, shift = true))
    '8' -> listOf(stroke(0x25, shift = true))
    '9' -> listOf(stroke(0x26, shift = true))
    '0' -> listOf(stroke(0x27, shift = true))

    '²' -> listOf(stroke(0x35))
    '&' -> listOf(stroke(0x1E))
    '"', '“', '”' -> listOf(stroke(0x20))
    '\'', '‘', '’' -> listOf(stroke(0x21))
    '(' -> listOf(stroke(0x22))
    '-' -> listOf(stroke(0x23))
    '_' -> listOf(stroke(0x25))
    ')' -> listOf(stroke(0x2D))
    '=' -> listOf(stroke(0x2E))
    '$' -> listOf(stroke(0x30))
    '*' -> listOf(stroke(0x32))
    ',' -> listOf(stroke(0x10))
    ';' -> listOf(stroke(0x36))
    ':' -> listOf(stroke(0x37))
    '!' -> listOf(stroke(0x38))
    '<' -> listOf(stroke(MouseConfig.KEY_ISO_BACKSLASH))

    '°' -> listOf(stroke(0x2D, shift = true))
    '+' -> listOf(stroke(0x2E, shift = true))
    '£' -> listOf(stroke(0x30, shift = true))
    'µ' -> listOf(stroke(0x32, shift = true))
    '%' -> listOf(stroke(0x34, shift = true))
    '?' -> listOf(stroke(0x10, shift = true))
    '.' -> listOf(stroke(0x36, shift = true))
    '/' -> listOf(stroke(0x37, shift = true))
    '§' -> listOf(stroke(0x38, shift = true))
    '>' -> listOf(stroke(MouseConfig.KEY_ISO_BACKSLASH, shift = true))

    '@' -> listOf(stroke(0x27, altGr = true))
    '#' -> listOf(stroke(0x20, altGr = true))
    '{' -> listOf(stroke(0x21, altGr = true))
    '[' -> listOf(stroke(0x22, altGr = true))
    '|' -> listOf(stroke(0x23, altGr = true))
    '\\' -> listOf(stroke(0x25, altGr = true))
    ']' -> listOf(stroke(0x2D, altGr = true))
    '}' -> listOf(stroke(0x2E, altGr = true))
    '€' -> listOf(stroke(0x08, altGr = true))

    // Dead keys used on their own need a space to insert the symbol itself.
    '^' -> listOf(stroke(0x2F), stroke(MouseConfig.KEY_SPACE))
    '¨' -> listOf(stroke(0x2F, shift = true), stroke(MouseConfig.KEY_SPACE))
    '~' -> listOf(stroke(0x1F, altGr = true), stroke(MouseConfig.KEY_SPACE))
    '`' -> listOf(stroke(0x24, altGr = true), stroke(MouseConfig.KEY_SPACE))

    ' ', '\u00A0', '\u202F' -> listOf(stroke(MouseConfig.KEY_SPACE))
    else -> emptyList()
}

// -------------------- QWERTZ (German) --------------------

private fun qwertzLetter(char: Char): Int = when (char) {
    'y' -> 0x1D
    'z' -> 0x1C
    else -> 0x04 + (char - 'a')
}

private fun qwertzLetterStroke(char: Char, uppercase: Boolean): HidKeystroke =
    stroke(qwertzLetter(char), shift = uppercase)

private fun qwertzKeystrokes(char: Char): List<HidKeystroke> = when (char) {
    in 'a'..'z' -> listOf(qwertzLetterStroke(char, uppercase = false))
    in 'A'..'Z' -> listOf(qwertzLetterStroke(char.lowercaseChar(), uppercase = true))

    'ü' -> listOf(stroke(0x2F))
    'ö' -> listOf(stroke(0x33))
    'ä' -> listOf(stroke(0x34))
    'Ü' -> listOf(stroke(0x2F, shift = true))
    'Ö' -> listOf(stroke(0x33, shift = true))
    'Ä' -> listOf(stroke(0x34, shift = true))
    'ß' -> listOf(stroke(0x2D))
    'ẞ' -> listOf(stroke(0x2D, shift = true))

    in '1'..'9' -> listOf(stroke(0x1E + (char - '1')))
    '0' -> listOf(stroke(0x27))

    '^' -> listOf(stroke(0x35), stroke(MouseConfig.KEY_SPACE))
    '´' -> listOf(stroke(0x2E), stroke(MouseConfig.KEY_SPACE))
    '`' -> listOf(stroke(0x2E, shift = true), stroke(MouseConfig.KEY_SPACE))
    '°' -> listOf(stroke(0x35, shift = true))

    '!' -> listOf(stroke(0x1E, shift = true))
    '"', '“', '”' -> listOf(stroke(0x1F, shift = true))
    '§' -> listOf(stroke(0x20, shift = true))
    '$' -> listOf(stroke(0x21, shift = true))
    '%' -> listOf(stroke(0x22, shift = true))
    '&' -> listOf(stroke(0x23, shift = true))
    '/' -> listOf(stroke(0x24, shift = true))
    '(' -> listOf(stroke(0x25, shift = true))
    ')' -> listOf(stroke(0x26, shift = true))
    '=' -> listOf(stroke(0x27, shift = true))
    '?' -> listOf(stroke(0x2D, shift = true))
    '*' -> listOf(stroke(0x30, shift = true))
    '\'', '‘', '’' -> listOf(stroke(0x32, shift = true))
    '>' -> listOf(stroke(MouseConfig.KEY_ISO_BACKSLASH, shift = true))
    ';' -> listOf(stroke(0x36, shift = true))
    ':' -> listOf(stroke(0x37, shift = true))
    '_' -> listOf(stroke(0x38, shift = true))

    '+' -> listOf(stroke(0x30))
    '#' -> listOf(stroke(0x32))
    '<' -> listOf(stroke(MouseConfig.KEY_ISO_BACKSLASH))
    ',' -> listOf(stroke(0x36))
    '.' -> listOf(stroke(0x37))
    '-' -> listOf(stroke(0x38))

    '@' -> listOf(stroke(0x14, altGr = true))
    '€' -> listOf(stroke(0x08, altGr = true))
    '{' -> listOf(stroke(0x24, altGr = true))
    '[' -> listOf(stroke(0x25, altGr = true))
    ']' -> listOf(stroke(0x26, altGr = true))
    '}' -> listOf(stroke(0x27, altGr = true))
    '\\' -> listOf(stroke(0x2D, altGr = true))
    '~' -> listOf(stroke(0x30, altGr = true))
    '|' -> listOf(stroke(MouseConfig.KEY_ISO_BACKSLASH, altGr = true))
    'µ' -> listOf(stroke(0x10, altGr = true))
    '²' -> listOf(stroke(0x1F, altGr = true))
    '³' -> listOf(stroke(0x20, altGr = true))

    ' ', '\u00A0', '\u202F' -> listOf(stroke(MouseConfig.KEY_SPACE))
    else -> emptyList()
}
