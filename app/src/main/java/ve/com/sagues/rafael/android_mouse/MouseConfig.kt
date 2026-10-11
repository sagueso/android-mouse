package ve.com.sagues.rafael.android_mouse

object MouseConfig {
    // This standard byte array tells the OS to expect a mouse with 3 buttons and X/Y movement
    val MOUSE_REPORT_DESCRIPTOR = byteArrayOf(
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x02, // Usage (Mouse)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x09, 0x01, //   Usage (Pointer)
        0xA1.toByte(), 0x00, //   Collection (Physical)
        0x05, 0x09, //     Usage Page (Button)
        0x19, 0x01, //     Usage Minimum (1)
        0x29, 0x03, //     Usage Maximum (3)
        0x15, 0x00, //     Logical Minimum (0)
        0x25, 0x01, //     Logical Maximum (1)
        0x95.toByte(), 0x03, //     Report Count (3)
        0x75, 0x01, //     Report Size (1)
        0x81.toByte(), 0x02, //     Input (Data, Variable, Absolute)
        0x95.toByte(), 0x01, //     Report Count (1)
        0x75, 0x05, //     Report Size (5)
        0x81.toByte(), 0x03, //     Input (Constant)
        0x05, 0x01, //     Usage Page (Generic Desktop)
        0x09, 0x30, //     Usage (X)
        0x09, 0x31, //     Usage (Y)
        0x15, 0x81.toByte(), //     Logical Minimum (-127)
        0x25, 0x7F, //     Logical Maximum (127)
        0x75, 0x08, //     Report Size (8)
        0x95.toByte(), 0x02, //     Report Count (2)
        0x81.toByte(), 0x06, //     Input (Data, Variable, Relative)
        0xC0.toByte(), //   End Collection
        0xC0.toByte()  // End Collection
    )
}