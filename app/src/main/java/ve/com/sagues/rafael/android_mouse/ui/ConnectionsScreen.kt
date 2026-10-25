package ve.com.sagues.rafael.android_mouse.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ve.com.sagues.rafael.android_mouse.BluetoothMouseManager
import ve.com.sagues.rafael.android_mouse.SavedHost
import ve.com.sagues.rafael.android_mouse.ui.theme.AccentAmber
import ve.com.sagues.rafael.android_mouse.ui.theme.Danger
import ve.com.sagues.rafael.android_mouse.ui.theme.Ink
import ve.com.sagues.rafael.android_mouse.ui.theme.Mint
import ve.com.sagues.rafael.android_mouse.ui.theme.SurfaceElevated
import ve.com.sagues.rafael.android_mouse.ui.theme.SurfaceHigh
import ve.com.sagues.rafael.android_mouse.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    mouseManager: BluetoothMouseManager,
    uiSettings: UiSettings,
    onBack: () -> Unit,
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedHosts by mouseManager.savedHosts.collectAsState()
    val connected by mouseManager.connectedHost.collectAsState()
    val latencyMs by mouseManager.latencyMs.collectAsState()
    val status by mouseManager.status.collectAsState()
    val showClickBar by uiSettings.showClickBar.collectAsState()
    val edgeLock by uiSettings.edgeLock.collectAsState()
    val battery = readBatteryPercent()

    var renameTarget by remember { mutableStateOf<SavedHost?>(null) }
    var iconTarget by remember { mutableStateOf<SavedHost?>(null) }
    var forgetTarget by remember { mutableStateOf<SavedHost?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text("Connections", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${savedHosts.size} paired ${if (savedHosts.size == 1) "device" else "devices"}",
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

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (connected != null) {
                    item(key = "active") {
                        ActiveConnectionCard(
                            host = connected!!,
                            latencyMs = latencyMs,
                            batteryPercent = battery,
                            onDisconnect = { mouseManager.disconnect() }
                        )
                    }
                } else if (status.contains("Connecting", ignoreCase = true)) {
                    item(key = "connecting") {
                        ConnectingCard(status = status)
                    }
                }

                if (savedHosts.isEmpty()) {
                    item(key = "empty") {
                        EmptyConnections(onAddDevice = onAddDevice)
                    }
                } else {
                    item(key = "header") {
                        Text(
                            text = "PAIRED DEVICES",
                            fontSize = 11.sp,
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }

                    items(savedHosts, key = { it.address }) { host ->
                        HostCard(
                            host = host,
                            isConnected = host.address == connected?.address,
                            isBonded = mouseManager.isBonded(host.address),
                            onConnect = { mouseManager.selectHost(host.address) },
                            onPairAgain = onAddDevice,
                            onRename = { renameTarget = host },
                            onChangeIcon = { iconTarget = host },
                            onForget = { forgetTarget = host }
                        )
                    }
                }

                item(key = "prefs") {
                    TrackpadPreferences(
                        showClickBar = showClickBar,
                        edgeLock = edgeLock,
                        onShowClickBarChange = uiSettings::setShowClickBar,
                        onEdgeLockChange = uiSettings::setEdgeLock
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddDevice,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = Mint,
            contentColor = Color(0xFF003824),
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add device", fontWeight = FontWeight.SemiBold) }
        )
    }

    renameTarget?.let { host ->
        RenameHostDialog(
            host = host,
            onConfirm = { newName ->
                mouseManager.renameHost(host.address, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    iconTarget?.let { host ->
        IconPickerDialog(
            host = host,
            onSelect = { kind ->
                mouseManager.setHostIcon(host.address, kind.name)
                iconTarget = null
            },
            onDismiss = { iconTarget = null }
        )
    }

    forgetTarget?.let { host ->
        AlertDialog(
            onDismissRequest = { forgetTarget = null },
            title = { Text("Forget ${host.name}?") },
            text = { Text("The device is removed from this app and unpaired from Android Bluetooth.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mouseManager.forgetHost(host.address)
                        forgetTarget = null
                    }
                ) {
                    Text("Forget", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { forgetTarget = null }) { Text("Cancel") }
            },
            containerColor = SurfaceHigh
        )
    }
}

@Composable
private fun ActiveConnectionCard(
    host: SavedHost,
    latencyMs: Int,
    batteryPercent: Int,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Mint.copy(alpha = 0.13f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, Mint.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeviceTile(
                kind = hostKind(host),
                modifier = Modifier.size(56.dp),
                background = Mint.copy(alpha = 0.18f),
                tint = Mint
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(color = Mint)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Active & connected", fontSize = 12.sp, color = Mint)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$latencyMs ms  •  ${if (batteryPercent >= 0) "$batteryPercent%" else "—"}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            IconButton(onClick = onDisconnect) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Disconnect",
                    tint = TextMuted
                )
            }
        }
    }
}

@Composable
private fun ConnectingCard(status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = AccentAmber.copy(alpha = 0.10f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(color = AccentAmber)
            Spacer(modifier = Modifier.width(12.dp))
            Text(status, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HostCard(
    host: SavedHost,
    isConnected: Boolean,
    isBonded: Boolean,
    onConnect: () -> Unit,
    onPairAgain: () -> Unit,
    onRename: () -> Unit,
    onChangeIcon: () -> Unit,
    onForget: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeviceTile(kind = hostKind(host))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isConnected) "Connected" else lastSeenLabel(host.lastConnectedAt),
                    fontSize = 11.sp,
                    color = if (isConnected) Mint else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isConnected) {
                if (isBonded) {
                    FilledTonalButton(
                        onClick = onConnect,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Connect", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onPairAgain,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Pair again", fontSize = 12.sp)
                    }
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = TextMuted
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Change icon") },
                        leadingIcon = { DeviceGlyph(hostKind(host), Modifier.size(20.dp)) },
                        onClick = {
                            menuExpanded = false
                            onChangeIcon()
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    DropdownMenuItem(
                        text = { Text("Forget device", color = Danger) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Danger) },
                        onClick = {
                            menuExpanded = false
                            onForget()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackpadPreferences(
    showClickBar: Boolean,
    edgeLock: Boolean,
    onShowClickBarChange: (Boolean) -> Unit,
    onEdgeLockChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "TRACKPAD",
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            PreferenceRow(
                title = "Show click bar",
                subtitle = "Physical left / right click buttons below the touch area.",
                checked = showClickBar,
                onCheckedChange = onShowClickBarChange
            )
            PreferenceRow(
                title = "Edge lock",
                subtitle = "Keeps edge swipes on the trackpad instead of triggering Android's back gesture.",
                checked = edgeLock,
                onCheckedChange = onEdgeLockChange
            )
        }
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = TextMuted)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EmptyConnections(onAddDevice: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyStateIllustration(modifier = Modifier.size(200.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "No devices connected yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Pair your computer or TV to start using this phone as a trackpad.",
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        FilledTonalButton(onClick = onAddDevice, shape = RoundedCornerShape(14.dp)) {
            Text("Pair a New Device")
        }
    }
}

@Composable
private fun RenameHostDialog(
    host: SavedHost,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(host.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename device") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Device name") },
                shape = RoundedCornerShape(12.dp)
            )
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

@Composable
private fun IconPickerDialog(
    host: SavedHost,
    onSelect: (DeviceKind) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change icon") },
        text = {
            Column {
                Text(host.name, fontSize = 12.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(10.dp))
                DeviceKind.entries.forEach { kind ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(kind) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DeviceGlyph(kind, Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(kind.label, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        RadioButton(
                            selected = hostKind(host) == kind,
                            onClick = { onSelect(kind) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        containerColor = SurfaceHigh
    )
}
