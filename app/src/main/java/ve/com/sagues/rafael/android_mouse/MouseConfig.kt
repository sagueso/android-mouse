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
    const val MOD_RIGHT_ALT = 0x40 // AltGr on most layouts

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
    const val KEY_CAPS_LOCK = 0x39
    const val KEY_HOME = 0x4A
    const val KEY_END = 0x4D
    const val KEY_PAGE_UP = 0x4B
    const val KEY_PAGE_DOWN = 0x4E
    const val KEY_UP_ARROW = 0x52
    const val KEY_DOWN_ARROW = 0x51

    /** ISO extra key next to the left Shift key ("<>" on AZERTY, "<>" on QWERTZ). */
    const val KEY_ISO_BACKSLASH = 0x64

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
