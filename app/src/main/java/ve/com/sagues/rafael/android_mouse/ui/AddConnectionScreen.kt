package ve.com.sagues.rafael.android_mouse.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ve.com.sagues.rafael.android_mouse.BluetoothMouseManager
import ve.com.sagues.rafael.android_mouse.ui.theme.Ink
import ve.com.sagues.rafael.android_mouse.ui.theme.Mint
import ve.com.sagues.rafael.android_mouse.ui.theme.SurfaceElevated
import ve.com.sagues.rafael.android_mouse.ui.theme.SurfaceHigh
import ve.com.sagues.rafael.android_mouse.ui.theme.TextMuted

private const val DISCOVERABLE_DURATION_SECONDS = 120

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConnectionScreen(
    mouseManager: BluetoothMouseManager,
    onBack: () -> Unit,
    onRequestDiscoverable: () -> Unit,
    onConnected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connected by mouseManager.connectedHost.collectAsState()
    val discoverableSeconds by mouseManager.discoverableSeconds.collectAsState()
    val phoneName by mouseManager.phoneName.collectAsState()

    var successHost by remember { mutableStateOf<String?>(null) }
    var showRename by remember { mutableStateOf(false) }
    var discoveryRequested by remember { mutableStateOf(false) }
    val wasConnectedOnEntry = remember { connected != null }

    LaunchedEffect(connected) {
        val host = connected
        if (host != null && successHost == null && (discoveryRequested || !wasConnectedOnEntry)) {
            successHost = host.name
        }
    }

    LaunchedEffect(successHost) {
        if (successHost != null) {
            delay(1_500)
            onConnected()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Add New Connection", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Pair this phone with a host",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp)
        ) {
            val pairedName = successHost

            if (pairedName != null) {
                ConnectedSuccessCard(hostName = pairedName)
            } else {
                DiscoverableCard(
                    active = discoverableSeconds > 0,
                    seconds = discoverableSeconds,
                    phoneName = phoneName,
                    onRequestDiscoverable = {
                        discoveryRequested = true
                        onRequestDiscoverable()
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                InstructionsCard(
                    phoneName = phoneName,
                    onRenamePhone = { showRename = true }
                )

                Spacer(modifier = Modifier.height(14.dp))

                PairingTipsCard()
            }
        }
    }

    if (showRename) {
        RenamePhoneDialog(
            currentName = phoneName,
            onConfirm = { newName ->
                mouseManager.renamePhone(newName)
                showRename = false
            },
            onDismiss = { showRename = false }
        )
    }
}

@Composable
private fun DiscoverableCard(
    active: Boolean,
    seconds: Int,
    phoneName: String,
    onRequestDiscoverable: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Mint.copy(alpha = 0.10f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, Mint.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(148.dp),
                contentAlignment = Alignment.Center
            ) {
                BluetoothRadar(
                    active = active,
                    modifier = Modifier.fillMaxSize(),
                    color = Mint
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (active) {
                Text(
                    text = "Discoverable as \"$phoneName\"",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                CountdownRing(seconds = seconds, total = DISCOVERABLE_DURATION_SECONDS)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Keep this screen open while you pair from the computer.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Make this phone discoverable",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Because the phone acts as a Bluetooth mouse, the host does the scanning. Start discovery here, then add the device from your PC, Mac or TV.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRequestDiscoverable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Make Phone Discoverable",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownRing(seconds: Int, total: Int) {
    Box(
        modifier = Modifier.size(84.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { (seconds.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            color = Mint,
            trackColor = Color.White.copy(alpha = 0.10f),
            strokeWidth = 5.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$seconds",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text("seconds", fontSize = 9.sp, color = TextMuted)
        }
    }
}

@Composable
private fun ConnectedSuccessCard(hostName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Mint.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, Mint.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 44.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SuccessBadge(visible = true, modifier = Modifier.size(96.dp), color = Mint)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Connected to $hostName",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Opening the trackpad…",
                fontSize = 12.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun InstructionsCard(
    phoneName: String,
    onRenamePhone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "HOW TO PAIR",
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(14.dp))

            StepRow(
                number = 1,
                title = "Enable discovery",
                body = "Tap “Make Phone Discoverable” above and keep this screen open."
            )
            StepRow(
                number = 2,
                title = "Open host Bluetooth settings",
                body = "On your PC or Mac, open Settings → Bluetooth & Devices → Add Device."
            )
            StepRow(
                number = 3,
                title = "Select this phone",
                body = "Select “$phoneName” from the list of available devices.",
                action = {
                    TextButton(onClick = onRenamePhone, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rename phone", fontSize = 12.sp)
                    }
                }
            )
            StepRow(
                number = 4,
                title = "Confirm the pairing code",
                body = "Accept the matching code prompt on both the phone and the computer.",
                isLast = true
            )
        }
    }
}

@Composable
private fun StepRow(
    number: Int,
    title: String,
    body: String,
    isLast: Boolean = false,
    action: (@Composable () -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = CircleShape,
                color = Mint.copy(alpha = 0.16f),
                contentColor = Mint
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$number",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(54.dp)
                        .background(Color.White.copy(alpha = 0.10f))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 8.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(body, fontSize = 12.sp, color = TextMuted)
            action?.invoke()
        }
    }
}

@Composable
private fun PairingTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceHigh.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Not seeing your computer?", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Check that Bluetooth is on for both devices, that the phone screen stays on, and that the host is not already paired with another mouse-only profile. Remove an old pairing on the host if it keeps failing.",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun RenamePhoneDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename this phone") },
        text = {
            Column {
                Text(
                    text = "This is the name the host will show while scanning for devices.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Bluetooth name") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = SurfaceHigh
    )
}
