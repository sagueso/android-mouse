package ve.com.sagues.rafael.android_mouse

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import ve.com.sagues.rafael.android_mouse.ui.MouseApp
import ve.com.sagues.rafael.android_mouse.ui.theme.AndroidmouseTheme

class MainActivity : ComponentActivity() {

    private lateinit var mouseManager: BluetoothMouseManager

    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        mouseManager.onDiscoverableResult(result.resultCode)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Notification is optional; the service runs either way. */ }

    private val btPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        emptyArray()
    }

    private val btPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it } || btPermissions.isEmpty()) {
            startHidService()
        } else {
            mouseManager.setPermissionError()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mouseManager = BluetoothMouseManager.getInstance(this)

        // A HID device is only reachable by a host while it is discoverable.
        val makeDiscoverable: () -> Unit = {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_SECONDS)
            }
            discoverableLauncher.launch(intent)
        }

        val missingBtPermission = btPermissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (btPermissions.isNotEmpty() && missingBtPermission) {
            btPermissionLauncher.launch(btPermissions)
        } else {
            startHidService()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            AndroidmouseTheme {
                MouseApp(
                    mouseManager = mouseManager,
                    onRequestDiscoverable = makeDiscoverable
                )
            }
        }
    }

    private fun startHidService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, HidForegroundService::class.java)
        )
    }

    companion object {
        private const val DISCOVERABLE_SECONDS = 120
    }
}
