package ve.com.sagues.rafael.android_mouse

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class BluetoothMouseManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter

    private var hidDevice: BluetoothHidDevice? = null
    var connectedHost: BluetoothDevice? = null

    // Debug State exposed to UI
    private val _status = MutableStateFlow("Waiting to initialize...")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private fun log(message: String) {
        // Keep the last 15 logs
        _logs.value = (_logs.value + message).takeLast(15)
    }

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
                hidDevice = null
            }
        }
    }

    private fun registerApp() {
        log("Registering App as Mouse...")
        val sdpRecord = BluetoothHidDeviceAppSdpSettings(
            "Android Mouse", "My Custom Mouse", "ve.com.sagues.rafael",
            BluetoothHidDevice.SUBCLASS1_MOUSE,
            MouseConfig.MOUSE_REPORT_DESCRIPTOR
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

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            log("AppStatusChanged: Registered = $registered")
            if (registered) {
                _status.value = "Ready! Please Pair/Connect PC."
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    _status.value = "CONNECTED to ${device.name}"
                    log("Host Connected: ${device.name}")
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _status.value = "Connecting to ${device.name}..."
                    log("Connecting...")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedHost = null
                    _status.value = "Disconnected"
                    log("Host Disconnected: ${device.name}")
                }
            }
        }
    }

    fun moveMouse(dx: Float, dy: Float) {
        val host = connectedHost
        if (host == null) {
            // Uncommenting this will spam your logs, but it's useful to know if it's trying:
            // log("Move failed: No host connected")
            return
        }

        val moveX = dx.toInt().coerceIn(-127, 127).toByte()
        val moveY = dy.toInt().coerceIn(-127, 127).toByte()
        val report = byteArrayOf(0x00, moveX, moveY)

        val success = hidDevice?.sendReport(host, 0, report)
        if (success != true) {
            log("Failed to send report data.")
        }
    }

    fun setPermissionError() {
        _status.value = "Error: Permission Denied"
        log("Cannot initialize: Bluetooth permissions were denied by the user or system.")
    }
}