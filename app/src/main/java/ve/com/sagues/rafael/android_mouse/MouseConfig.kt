package ve.com.sagues.rafael.android_mouse

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

object MouseConfig {

    // Report IDs (must match the report descriptor below)
    const val REPORT_ID_MOUSE = 1
    const val REPORT_ID_KEYBOARD = 2

    // Keyboard modifier bits (byte 0 of the keyboard report)
    const val MOD_LEFT_CTRL = 0x01
    const val MOD_LEFT_SHIFT = 0x02
    const val MOD_LEFT_ALT = 0x04
    const val MOD_LEFT_GUI = 0x08 // Windows key

    // HID keyboard usage IDs
    const val KEY_TAB = 0x2B
    const val KEY_D = 0x07
    const val KEY_S = 0x16
    const val KEY_N = 0x11
    const val KEY_LEFT_ARROW = 0x50
    const val KEY_RIGHT_ARROW = 0x4F
    const val KEY_ENTER = 0x28
    const val KEY_ESCAPE = 0x29
    const val KEY_BACKSPACE = 0x2A
    const val KEY_SPACE = 0x2C
    const val KEY_HOME = 0x4A
    const val KEY_END = 0x4D
    const val KEY_PAGE_UP = 0x4B
    const val KEY_PAGE_DOWN = 0x4E
    const val KEY_UP_ARROW = 0x52
    const val KEY_DOWN_ARROW = 0x51

    /** A printable character mapped to its HID usage ID (and whether Shift is required). */
    data class HidKey(val usage: Int, val shift: Boolean = false)

    /** Maps the characters produced by a software keyboard to HID usage IDs. */
    fun keyForChar(char: Char): HidKey? = when (char) {
        in 'a'..'z' -> HidKey(0x04 + (char - 'a'))
        in 'A'..'Z' -> HidKey(0x04 + (char - 'A'), shift = true)
        in '1'..'9' -> HidKey(0x1E + (char - '1'))
        '0' -> HidKey(0x27)
        ' ' -> HidKey(KEY_SPACE)
        '-' -> HidKey(0x2D)
        '_' -> HidKey(0x2D, shift = true)
        '=' -> HidKey(0x2E)
        '+' -> HidKey(0x2E, shift = true)
        '[' -> HidKey(0x2F)
        '{' -> HidKey(0x2F, shift = true)
        ']' -> HidKey(0x30)
        '}' -> HidKey(0x30, shift = true)
        '\\' -> HidKey(0x31)
        '|' -> HidKey(0x31, shift = true)
        ';' -> HidKey(0x33)
        ':' -> HidKey(0x33, shift = true)
        '\'' -> HidKey(0x34)
        '"' -> HidKey(0x34, shift = true)
        '`' -> HidKey(0x35)
        '~' -> HidKey(0x35, shift = true)
        ',' -> HidKey(0x36)
        '<' -> HidKey(0x36, shift = true)
        '.' -> HidKey(0x37)
        '>' -> HidKey(0x37, shift = true)
        '/' -> HidKey(0x38)
        '?' -> HidKey(0x38, shift = true)
        '!' -> HidKey(0x1E, shift = true)
        '@' -> HidKey(0x1F, shift = true)
        '#' -> HidKey(0x20, shift = true)
        '$' -> HidKey(0x21, shift = true)
        '%' -> HidKey(0x22, shift = true)
        '^' -> HidKey(0x23, shift = true)
        '&' -> HidKey(0x24, shift = true)
        '*' -> HidKey(0x25, shift = true)
        '(' -> HidKey(0x26, shift = true)
        ')' -> HidKey(0x27, shift = true)
        else -> null
    }

    /**
     * Composite HID report descriptor:
     *  - Report ID 1: Mouse (3 buttons, X, Y, vertical wheel, horizontal pan)
     *  - Report ID 2: Keyboard (8 modifier bits, reserved byte, 6 key slots)
     */
    val COMBO_REPORT_DESCRIPTOR = byteArrayOf(
        // ---------- Mouse (Report ID 1) ----------
        0x05, 0x01,                               // Usage Page (Generic Desktop)
        0x09, 0x02,                               // Usage (Mouse)
        0xA1.toByte(), 0x01,                      // Collection (Application)
        0x85.toByte(), REPORT_ID_MOUSE.toByte(),  //   Report ID (1)
        0x09, 0x01,                               //   Usage (Pointer)
        0xA1.toByte(), 0x00,                      //   Collection (Physical)
        0x05, 0x09,                               //     Usage Page (Button)
        0x19, 0x01,                               //     Usage Minimum (1)
        0x29, 0x03,                               //     Usage Maximum (3)
        0x15, 0x00,                               //     Logical Minimum (0)
        0x25, 0x01,                               //     Logical Maximum (1)
        0x95.toByte(), 0x03,                      //     Report Count (3)
        0x75, 0x01,                               //     Report Size (1)
        0x81.toByte(), 0x02,                      //     Input (Data,Var,Abs)
        0x95.toByte(), 0x01,                      //     Report Count (1)
        0x75, 0x05,                               //     Report Size (5)
        0x81.toByte(), 0x03,                      //     Input (Cnst,Var,Abs)  -- padding
        0x05, 0x01,                               //     Usage Page (Generic Desktop)
        0x09, 0x30,                               //     Usage (X)
        0x09, 0x31,                               //     Usage (Y)
        0x09, 0x38,                               //     Usage (Wheel)
        0x15, 0x81.toByte(),                      //     Logical Minimum (-127)
        0x25, 0x7F,                               //     Logical Maximum (127)
        0x75, 0x08,                               //     Report Size (8)
        0x95.toByte(), 0x03,                      //     Report Count (3)
        0x81.toByte(), 0x06,                      //     Input (Data,Var,Rel)
        0x05, 0x0C,                               //     Usage Page (Consumer)
        0x0A, 0x38, 0x02,                         //     Usage (AC Pan)
        0x15, 0x81.toByte(),                      //     Logical Minimum (-127)
        0x25, 0x7F,                               //     Logical Maximum (127)
        0x75, 0x08,                               //     Report Size (8)
        0x95.toByte(), 0x01,                      //     Report Count (1)
        0x81.toByte(), 0x06,                      //     Input (Data,Var,Rel)
        0xC0.toByte(),                            //   End Collection
        0xC0.toByte(),                            // End Collection

        // ---------- Keyboard (Report ID 2) ----------
        0x05, 0x01,                                  // Usage Page (Generic Desktop)
        0x09, 0x06,                                  // Usage (Keyboard)
        0xA1.toByte(), 0x01,                         // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD.toByte(),  //   Report ID (2)
        0x05, 0x07,                                  //   Usage Page (Keyboard/Keypad)
        0x19, 0xE0.toByte(),                         //   Usage Minimum (224)
        0x29, 0xE7.toByte(),                         //   Usage Maximum (231)
        0x15, 0x00,                                  //   Logical Minimum (0)
        0x25, 0x01,                                  //   Logical Maximum (1)
        0x75, 0x01,                                  //   Report Size (1)
        0x95.toByte(), 0x08,                         //   Report Count (8)
        0x81.toByte(), 0x02,                         //   Input (Data,Var,Abs)  -- modifiers
        0x95.toByte(), 0x01,                         //   Report Count (1)
        0x75, 0x08,                                  //   Report Size (8)
        0x81.toByte(), 0x03,                         //   Input (Cnst,Var,Abs)  -- reserved
        0x95.toByte(), 0x06,                         //   Report Count (6)
        0x75, 0x08,                                  //   Report Size (8)
        0x15, 0x00,                                  //   Logical Minimum (0)
        0x25, 0x65,                                  //   Logical Maximum (101)
        0x05, 0x07,                                  //   Usage Page (Keyboard/Keypad)
        0x19, 0x00,                                  //   Usage Minimum (0)
        0x29, 0x65,                                  //   Usage Maximum (101)
        0x81.toByte(), 0x00,                         //   Input (Data,Array,Abs)
        0xC0.toByte()                                // End Collection
    )
}
