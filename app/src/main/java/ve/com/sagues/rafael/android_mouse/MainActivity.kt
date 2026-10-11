package ve.com.sagues.rafael.android_mouse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

        mouseManager = BluetoothMouseManager(this)

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            emptyArray()
        }

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val allGranted = perms.values.all { it }
            if (allGranted || permissions.isEmpty()) {
                mouseManager.init() // Success!
            } else {
                mouseManager.setPermissionError() // FAILED: Update the UI!
            }
        }

        if (permissions.isNotEmpty() && permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            // Ask for permission
            requestPermissionLauncher.launch(permissions)
        } else {
            // Already have permission
            mouseManager.init()
        }

        setContent {
            AndroidmouseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DebugGestureTracker(
                        mouseManager = mouseManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DebugGestureTracker(
    mouseManager: BluetoothMouseManager,
    modifier: Modifier = Modifier
) {
    // Read state from Manager
    val status by mouseManager.status.collectAsState()
    val logs by mouseManager.logs.collectAsState()

    // Read gestures for debug screen
    var currentDx by remember { mutableFloatStateOf(0f) }
    var currentDy by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier.fillMaxSize()) {

        // --- TOP: DEBUG INFO ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0))
                .padding(16.dp)
        ) {
            Text("Status: $status", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sent dx: $currentDx | dy: $currentDy")
        }

        // --- MIDDLE: TRACKPAD AREA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            currentDx = 0f
                            currentDy = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        currentDx = dragAmount.x
                        currentDy = dragAmount.y

                        mouseManager.moveMouse(currentDx, currentDy)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text("DRAG HERE", color = Color.Gray)
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