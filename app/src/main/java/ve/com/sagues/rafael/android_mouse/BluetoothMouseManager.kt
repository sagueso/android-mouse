package ve.com.sagues.rafael.android_mouse

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SavedHost(val address: String, val name: String)

@SuppressLint("MissingPermission")
class BluetoothMouseManager private constructor(private val context: Context) {

    companion object {
        private const val PREFS = "hid_mouse"
        private const val KEY_LAST_HOST = "last_host"
        private const val KEY_HOSTS = "saved_hosts"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_DELAY_MS = 3000L
        private const val SCROLL_DIVISOR = 60f
        private const val ZOOM_SENSITIVITY = 8f
        private const val DOUBLE_CLICK_GAP_MS = 40L

        @Volatile
        private var instance: BluetoothMouseManager? = null

        // Shared by the foreground service (owner) and the Activity (observer).
        fun getInstance(context: Context): BluetoothMouseManager =
            instance ?: synchronized(this) {
                instance ?: BluetoothMouseManager(context.applicationContext).also { instance = it }
            }
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val mainHandler = Handler(Looper.getMainLooper())

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private var initialized = false
    private var reconnectAttempts = 0

    // Mouse button state (bit0 left, bit1 right, bit2 middle).
    private var buttonMask = 0

    // The host we want to talk to / reconnect to (the "active" host).
    private val _activeHostAddress = MutableStateFlow(prefs.getString(KEY_LAST_HOST, null))
    val activeHostAddress: StateFlow<String?> = _activeHostAddress.asStateFlow()

    private var lastHostAddress: String?
        get() = _activeHostAddress.value
        set(value) {
            _activeHostAddress.value = value
            prefs.edit().apply {
                if (value == null) remove(KEY_LAST_HOST) else putString(KEY_LAST_HOST, value)
            }.apply()
        }

    // Devices we have successfully controlled before (PC, TV, ...).
    private val _savedHosts = MutableStateFlow(loadHosts())
    val savedHosts: StateFlow<List<SavedHost>> = _savedHosts.asStateFlow()

    // Debug State exposed to UI
    private val _status = MutableStateFlow("Waiting to initialize...")
    val status: StateFlow<String> = _status.asStateFlow()

    // True once the app is registered as a HID device.
    private val _registered = MutableStateFlow(false)
    val registered: StateFlow<Boolean> = _registered.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    // Sub-pixel movement is accumulated so slow gestures still register.
    private var pendingX = 0f
    private var pendingY = 0f
    private var pendingScrollX = 0f
    private var pendingScrollY = 0f
    private var pendingZoom = 0f

    private fun log(message: String) {
        // Keep the last 15 logs
        _logs.value = (_logs.value + message).takeLast(15)
    }

    fun hasSavedHost(): Boolean = _savedHosts.value.isNotEmpty()

    // -------------------- persistence --------------------

    private fun loadHosts(): List<SavedHost> {
        val raw = prefs.getString(KEY_HOSTS, null) ?: return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 2 && parts[0].isNotBlank()) SavedHost(parts[0], parts[1]) else null
        }
    }

    private fun persistHosts(hosts: List<SavedHost>) {
        prefs.edit()
            .putString(KEY_HOSTS, hosts.joinToString("\n") { "${it.address}|${it.name}" })
            .apply()
    }

    private fun upsertHost(device: BluetoothDevice) {
        val list = _savedHosts.value.toMutableList()
        val index = list.indexOfFirst { it.address == device.address }
        val host = SavedHost(device.address, device.name ?: device.address)
        if (index >= 0) list[index] = host else list.add(host)
        _savedHosts.value = list
        persistHosts(list)
    }

    fun forgetHost(address: String) {
        val list = _savedHosts.value.filterNot { it.address == address }
        _savedHosts.value = list
        persistHosts(list)

        val device = try {
            bluetoothAdapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            null
        }

        if (device != null) {
            if (hidDevice?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                hidDevice?.disconnect(device)
            }
            removeBond(device)
        }

        if (lastHostAddress == address) {
            connectedHost = null
            lastHostAddress = null
        }
        _status.value = "Host forgotten and unpaired."
        log("Removed $address from saved hosts and unpaired it.")
    }

    /**
     * [BluetoothDevice.removeBond] is not part of the public SDK, so it is called
     * reflectively (requires BLUETOOTH_CONNECT).
     */
    private fun removeBond(device: BluetoothDevice) {
        try {
            val removed = device.javaClass.getMethod("removeBond").invoke(device) as? Boolean
            if (removed == true) {
                log("Unpairing ${device.name}...")
            } else {
                log("Unpair request rejected for ${device.name}.")
            }
        } catch (e: Exception) {
            log("Could not unpair: ${e.javaClass.simpleName}")
        }
    }

    // -------------------- switching --------------------

    fun selectHost(address: String) {
        lastHostAddress = address
        reconnectAttempts = 0

        val hid = hidDevice
        if (hid == null) {
            _status.value = "Will connect to this host once registered."
            return
        }

        // A HID device serves one host at a time: drop the current one first.
        connectedHost?.let { if (it.address != address) hid.disconnect(it) }

        val device = try {
            bluetoothAdapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            log("Invalid host address $address.")
            return
        }
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            _status.value = "${device.name} is not paired anymore."
            log("${device.name} is not bonded - pair it again.")
            return
        }

        _status.value = "Connecting to ${device.name}..."
        log("Switching to ${device.name}...")
        hid.connect(device)
    }

    fun reconnect() {
        val address = lastHostAddress
        if (address == null) {
            _status.value = "No saved host to reconnect to."
            return
        }
        selectHost(address)
    }

    // -------------------- HID lifecycle --------------------

    @Synchronized
    fun init() {
        if (bluetoothAdapter == null) {
            _status.value = "Error: No BT Adapter"
            log("Init Failed: Device does not have Bluetooth.")
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            _status.value = "Error: BT is OFF"
            log("Init Failed: Bluetooth is turned off.")
            return
        }
        if (initialized) return
        initialized = true

        log("Init: Getting HID Profile Proxy...")
        _status.value = "Getting Profile Proxy..."
        bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                log("Success: HID Profile Connected!")
                hidDevice = proxy as BluetoothHidDevice
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                log("Error: HID Profile Disconnected!")
                _status.value = "Profile Disconnected"
                _registered.value = false
                hidDevice = null
                connectedHost = null
                initialized = false
            }
        }
    }

    private fun registerApp() {
        log("Registering App as Mouse + Keyboard...")
        val sdpRecord = BluetoothHidDeviceAppSdpSettings(
            "Android Mouse", "Android Mouse + Keyboard", "ve.com.sagues.rafael",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            MouseConfig.COMBO_REPORT_DESCRIPTOR
        )

        val qosOut = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX
        )

        val result = hidDevice?.registerApp(sdpRecord, null, qosOut, context.mainExecutor, hidCallback)
        if (result == true) {
            log("registerApp() returned true. Waiting for callback...")
            _status.value = "Waiting for PC Connection..."
        } else {
            log("Error: registerApp() returned false.")
            _status.value = "App Registration Failed"
        }
    }

    private fun connectToSavedHost() {
        val address = lastHostAddress ?: return
        val device = try {
            bluetoothAdapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            log("Saved host address is invalid.")
            return
        }
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            log("Saved host ${device.name} is no longer bonded - pair again.")
            return
        }
        if (hidDevice?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) return

        log("Reconnecting to ${device.name}...")
        _status.value = "Reconnecting to ${device.name}..."
        hidDevice?.connect(device)
    }

    private fun scheduleReconnect() {
        if (lastHostAddress == null) return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            log("Stopped reconnecting. Pick a host or pair again.")
            return
        }
        reconnectAttempts++
        mainHandler.postDelayed({ connectToSavedHost() }, RECONNECT_DELAY_MS * reconnectAttempts)
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            _registered.value = registered
            log("AppStatusChanged: Registered = $registered")
            if (!registered) {
                _status.value = "App Unregistered"
                return
            }

            // A bonded host plugged the virtual cable: connect if not already connected.
            if (pluggedDevice != null &&
                hidDevice?.getConnectionState(pluggedDevice) != BluetoothProfile.STATE_CONNECTED
            ) {
                log("Connecting to bonded host ${pluggedDevice.name}...")
                hidDevice?.connect(pluggedDevice)
            }

            // Auto-reconnect to the host used last time.
            connectToSavedHost()

            if (connectedHost == null && lastHostAddress == null) {
                _status.value = "Ready! Pair your phone with the PC."
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val name = device?.name ?: "unknown"
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    buttonMask = 0
                    reconnectAttempts = 0
                    if (device != null) {
                        upsertHost(device)
                        lastHostAddress = device.address
                    }
                    _status.value = "CONNECTED to $name"
                    log("Host Connected: $name")
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _status.value = "Connecting to $name..."
                    log("Connecting to $name...")
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    _status.value = "Disconnecting from $name..."
                    log("Disconnecting from $name...")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedHost = null
                    buttonMask = 0
                    _status.value = "Disconnected - reconnecting..."
                    log("Host Disconnected: $name")
                    scheduleReconnect()
                }
            }
        }
    }

    /** Result of [android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE]. */
    fun onDiscoverableResult(resultCode: Int) {
        if (resultCode > 0) {
            _status.value = "Discoverable for ${resultCode}s. Pair from your PC now."
            log("Discoverable enabled for ${resultCode}s.")
        } else {
            _status.value = "Not discoverable - the PC cannot pair."
            log("Discoverability request was denied.")
        }
    }

    // -------------------- reports --------------------

    private fun sendMouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int, pan: Int) {
        val host = connectedHost ?: return
        val hid = hidDevice ?: return
        val report = byteArrayOf(
            buttons.toByte(),
            dx.toByte(),
            dy.toByte(),
            wheel.toByte(),
            pan.toByte()
        )
        hid.sendReport(host, MouseConfig.REPORT_ID_MOUSE, report)
    }

    private fun sendKeyboardReport(modifiers: Int, key: Int) {
        val host = connectedHost ?: return
        val hid = hidDevice ?: return
        val report = byteArrayOf(
            modifiers.toByte(), 0, key.toByte(), 0, 0, 0, 0, 0
        )
        hid.sendReport(host, MouseConfig.REPORT_ID_KEYBOARD, report)
    }

    fun moveMouse(dx: Float, dy: Float) {
        if (connectedHost == null || hidDevice == null) return

        pendingX += dx
        pendingY += dy

        val moveX = pendingX.toInt().coerceIn(-127, 127)
        val moveY = pendingY.toInt().coerceIn(-127, 127)
        if (moveX == 0 && moveY == 0) return

        pendingX -= moveX
        pendingY -= moveY
        sendMouseReport(buttonMask, moveX, moveY, 0, 0)
    }

    fun scroll(dx: Float, dy: Float) {
        if (connectedHost == null || hidDevice == null) return

        // Drag up -> scroll up, drag right -> scroll right.
        pendingScrollY += dy / SCROLL_DIVISOR
        pendingScrollX += dx / SCROLL_DIVISOR

        val wheel = pendingScrollY.toInt()
        val pan = pendingScrollX.toInt()
        if (wheel == 0 && pan == 0) return

        pendingScrollY -= wheel
        pendingScrollX -= pan
        sendMouseReport(buttonMask, 0, 0, wheel.coerceIn(-127, 127), pan.coerceIn(-127, 127))
    }

    fun zoomBy(scale: Float) {
        if (scale == 1f) return
        if (connectedHost == null || hidDevice == null) return

        // Pinch out (scale > 1) -> Ctrl + wheel up (zoom in).
        pendingZoom += (scale - 1f) * ZOOM_SENSITIVITY
        val steps = pendingZoom.toInt()
        if (steps == 0) return

        pendingZoom -= steps
        sendKeyboardReport(MouseConfig.MOD_LEFT_CTRL, 0)
        sendMouseReport(buttonMask, 0, 0, steps.coerceIn(-127, 127), 0)
        sendKeyboardReport(0, 0)
    }

    fun leftClick() {
        sendMouseReport(buttonMask or 0x01, 0, 0, 0, 0)
        sendMouseReport(buttonMask and 0x01.inv(), 0, 0, 0, 0)
    }

    fun doubleClick() {
        leftClick()
        mainHandler.postDelayed({ leftClick() }, DOUBLE_CLICK_GAP_MS)
    }

    fun rightClick() {
        sendMouseReport(buttonMask or 0x02, 0, 0, 0, 0)
        sendMouseReport(buttonMask and 0x02.inv(), 0, 0, 0, 0)
    }

    fun leftButtonDown() {
        buttonMask = buttonMask or 0x01
        sendMouseReport(buttonMask, 0, 0, 0, 0)
    }

    fun leftButtonUp() {
        buttonMask = buttonMask and 0x01.inv()
        sendMouseReport(buttonMask, 0, 0, 0, 0)
    }

    /** Press and release a key combination, e.g. Win+Tab. */
    fun sendShortcut(modifiers: Int, key: Int) {
        sendKeyboardReport(modifiers, key)
        sendKeyboardReport(0, 0)
    }

    fun threeFingerSwipe(direction: SwipeDirection) {
        when (direction) {
            SwipeDirection.UP -> sendShortcut(MouseConfig.MOD_LEFT_GUI, MouseConfig.KEY_TAB)          // Task View
            SwipeDirection.DOWN -> sendShortcut(MouseConfig.MOD_LEFT_GUI, MouseConfig.KEY_D)          // Show desktop
            SwipeDirection.RIGHT -> sendShortcut(MouseConfig.MOD_LEFT_ALT, MouseConfig.KEY_TAB)       // Alt+Tab
            SwipeDirection.LEFT -> sendShortcut(
                MouseConfig.MOD_LEFT_ALT or MouseConfig.MOD_LEFT_SHIFT, MouseConfig.KEY_TAB         // Alt+Shift+Tab
            )
        }
    }

    fun threeFingerTap() {
        sendShortcut(MouseConfig.MOD_LEFT_GUI, MouseConfig.KEY_S) // Search
    }

    fun fourFingerSwipe(direction: SwipeDirection) {
        when (direction) {
            SwipeDirection.UP -> sendShortcut(MouseConfig.MOD_LEFT_GUI, MouseConfig.KEY_TAB)          // Task View
            SwipeDirection.DOWN -> sendShortcut(MouseConfig.MOD_LEFT_GUI, MouseConfig.KEY_D)          // Show desktop
            SwipeDirection.RIGHT -> sendShortcut(
                MouseConfig.MOD_LEFT_GUI or MouseConfig.MOD_LEFT_CTRL, MouseConfig.KEY_RIGHT_ARROW   // Next desktop
            )
            SwipeDirection.LEFT -> sendShortcut(
                MouseConfig.MOD_LEFT_GUI or MouseConfig.MOD_LEFT_CTRL, MouseConfig.KEY_LEFT_ARROW    // Prev desktop
            )
        }
    }

    fun fourFingerTap() {
        sendShortcut(MouseConfig.MOD_LEFT_GUI, MouseConfig.KEY_N) // Notification center
    }

    fun setPermissionError() {
        _status.value = "Error: Permission Denied"
        log("Cannot initialize: Bluetooth permissions were denied by the user or system.")
    }
}
