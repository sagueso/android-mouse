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
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ve.com.sagues.rafael.android_mouse.ui.theme.AndroidmouseTheme

class MainActivity : ComponentActivity() {

    private lateinit var mouseManager: BluetoothMouseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mouseManager = BluetoothMouseManager.getInstance(this)

        val discoverableLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            mouseManager.onDiscoverableResult(result.resultCode)
        }

        // A HID device is only reachable by a host while it is discoverable.
        val makeDiscoverable: () -> Unit = {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            discoverableLauncher.launch(intent)
        }

        val notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* Notification is optional; the service runs either way. */ }

        val btPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            emptyArray()
        }

        val btPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            if (perms.values.all { it } || btPermissions.isEmpty()) {
                startHidService() // Success!
            } else {
                mouseManager.setPermissionError() // FAILED: Update the UI!
            }
        }

        val missingBtPermission = btPermissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (btPermissions.isNotEmpty() && missingBtPermission) {
            // Ask for permission
            btPermissionLauncher.launch(btPermissions)
        } else {
            // Already have permission
            startHidService()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            AndroidmouseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DebugGestureTracker(
                        mouseManager = mouseManager,
                        onRequestDiscoverable = makeDiscoverable,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun startHidService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, HidForegroundService::class.java)
        )
    }
}

@Composable
fun DebugGestureTracker(
    mouseManager: BluetoothMouseManager,
    onRequestDiscoverable: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Read state from Manager
    val status by mouseManager.status.collectAsState()
    val logs by mouseManager.logs.collectAsState()
    val registered by mouseManager.registered.collectAsState()
    val savedHosts by mouseManager.savedHosts.collectAsState()
    val activeAddress by mouseManager.activeHostAddress.collectAsState()

    // On the very first run (no saved host) automatically ask to become discoverable so
    // the host can pair. Afterwards we reconnect to the active host without pairing again.
    var discoverabilityRequested by remember { mutableStateOf(false) }
    LaunchedEffect(registered) {
        if (registered && !discoverabilityRequested && !mouseManager.hasSavedHost()) {
            discoverabilityRequested = true
            onRequestDiscoverable()
        }
    }

    // Read gestures for debug screen
    var currentDx by remember { mutableFloatStateOf(0f) }
    var currentDy by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier.fillMaxSize()) {

        // --- TOP: DEBUG INFO + HOST SWITCHER ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0))
                .padding(16.dp)
        ) {
            Text("Status: $status", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sent dx: $currentDx | dy: $currentDy")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onRequestDiscoverable) { Text("Discoverable") }
                OutlinedButton(
                    onClick = { mouseManager.reconnect() },
                    enabled = activeAddress != null
                ) { Text("Reconnect") }
                OutlinedButton(
                    onClick = { activeAddress?.let { mouseManager.forgetHost(it) } },
                    enabled = activeAddress != null
                ) { Text("Forget & Unpair") }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (savedHosts.isEmpty())
                    "No saved hosts yet. Tap Discoverable and pair from your PC/TV."
                else
                    "Saved hosts (tap to switch):",
                fontSize = 12.sp
            )
            if (savedHosts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedHosts, key = { it.address }) { host ->
                        FilterChip(
                            selected = host.address == activeAddress,
                            onClick = { mouseManager.selectHost(host.address) },
                            label = { Text(host.name) }
                        )
                    }
                }
            }
        }

        // --- MIDDLE: TRACKPAD AREA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .trackpadGestures(
                    onMove = { dx, dy ->
                        currentDx = dx
                        currentDy = dy
                        mouseManager.moveMouse(dx, dy)
                    },
                    onScroll = { dx, dy -> mouseManager.scroll(dx, dy) },
                    onZoom = { scale -> mouseManager.zoomBy(scale) },
                    onLeftClick = { mouseManager.leftClick() },
                    onDoubleClick = { mouseManager.doubleClick() },
                    onRightClick = { mouseManager.rightClick() },
                    onDragStart = { mouseManager.leftButtonDown() },
                    onDragEnd = { mouseManager.leftButtonUp() },
                    onThreeFingerSwipe = { dir -> mouseManager.threeFingerSwipe(dir) },
                    onThreeFingerTap = { mouseManager.threeFingerTap() },
                    onFourFingerSwipe = { dir -> mouseManager.fourFingerSwipe(dir) },
                    onFourFingerTap = { mouseManager.fourFingerTap() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("TRACKPAD", color = Color.Gray)
        }

        // --- BOTTOM: LOG CONSOLE ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Black)
                .padding(8.dp)
        ) {
            Text("LIVE LOGS:", color = Color.Green, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))

            logs.forEach { logMessage ->
                Text(
                    text = "> $logMessage",
                    color = Color.Green,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
