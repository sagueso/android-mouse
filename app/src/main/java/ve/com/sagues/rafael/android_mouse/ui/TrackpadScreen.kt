package ve.com.sagues.rafael.android_mouse.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import ve.com.sagues.rafael.android_mouse.BluetoothMouseManager
import ve.com.sagues.rafael.android_mouse.HostKeyboardLayout
import ve.com.sagues.rafael.android_mouse.MouseConfig
import ve.com.sagues.rafael.android_mouse.SavedHost
import ve.com.sagues.rafael.android_mouse.trackpadGestures
import ve.com.sagues.rafael.android_mouse.ui.theme.AccentAmber
import ve.com.sagues.rafael.android_mouse.ui.theme.Ink
import ve.com.sagues.rafael.android_mouse.ui.theme.Mint
import ve.com.sagues.rafael.android_mouse.ui.theme.SurfaceElevated
import ve.com.sagues.rafael.android_mouse.ui.theme.SurfaceHigh
import ve.com.sagues.rafael.android_mouse.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackpadScreen(
    mouseManager: BluetoothMouseManager,
    uiSettings: UiSettings,
    onOpenConnections: () -> Unit,
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connected by mouseManager.connectedHost.collectAsState()
    val savedHosts by mouseManager.savedHosts.collectAsState()
    val status by mouseManager.status.collectAsState()
    val registered by mouseManager.registered.collectAsState()
    val latencyMs by mouseManager.latencyMs.collectAsState()
    val showClickBar by uiSettings.showClickBar.collectAsState()
    val edgeLock by uiSettings.edgeLock.collectAsState()
    val keyboardLayout by uiSettings.keyboardLayout.collectAsState()

    val batteryPercent = readBatteryPercent()

    var showSwitchSheet by remember { mutableStateOf(false) }
    var showKeyboard by remember { mutableStateOf(false) }

    EdgeLockEffect(enabled = edgeLock)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        TrackpadHeader(
            connected = connected,
            status = if (registered) status else "Starting Bluetooth…",
            latencyMs = latencyMs,
            batteryPercent = batteryPercent,
            onPillClick = { showSwitchSheet = true },
            onKeyboardClick = { showKeyboard = true },
            onSettingsClick = onOpenConnections
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF17171A), Color(0xFF0D0D0F)),
                        center = Offset.Unspecified,
                        radius = 1_200f
                    )
                )
                .dotGrid(spacing = 24.dp)
                .then(
                    if (connected != null) {
                        Modifier.trackpadGestures(
                            onMove = { dx, dy -> mouseManager.moveMouse(dx, dy) },
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
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (connected == null) {
                DisconnectedHint(onAddDevice = onAddDevice)
            }
        }

        AnimatedVisibility(visible = showClickBar) {
            ClickBar(
                onLeftPress = { mouseManager.leftButtonDown() },
                onLeftRelease = { mouseManager.leftButtonUp() },
                onRightClick = { mouseManager.rightClick() },
                onScroll = { dy -> mouseManager.scroll(0f, dy) }
            )
        }
    }

    if (showSwitchSheet) {
        QuickSwitchSheet(
            hosts = savedHosts,
            connectedAddress = connected?.address,
            onSelect = { host ->
                mouseManager.selectHost(host.address)
                showSwitchSheet = false
            },
            onManage = {
                showSwitchSheet = false
                onOpenConnections()
            },
            onAddDevice = {
                showSwitchSheet = false
                onAddDevice()
            },
            onDismiss = { showSwitchSheet = false }
        )
    }

    if (showKeyboard) {
        KeyboardDialog(
            hostName = connected?.name,
            layout = keyboardLayout,
            onLayoutChange = uiSettings::setKeyboardLayout,
            onSendText = { mouseManager.sendText(it, keyboardLayout) },
            onKey = { key, modifiers -> mouseManager.sendKey(key, modifiers) },
            onDismiss = { showKeyboard = false }
        )
    }
}

@Composable
private fun TrackpadHeader(
    connected: SavedHost?,
    status: String,
    latencyMs: Int,
    batteryPercent: Int,
    onPillClick: () -> Unit,
    onKeyboardClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                onClick = onPillClick,
                shape = RoundedCornerShape(22.dp),
                color = if (connected != null) Mint.copy(alpha = 0.14f) else SurfaceHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusDot(
                        color = if (connected != null) Mint else AccentAmber,
                        pulsing = connected != null
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Column {
                        Text(
                            text = connected?.let { "Connected to: ${it.name}" } ?: "Not connected",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (connected != null) {
                                "$latencyMs ms  •  ${if (batteryPercent >= 0) "$batteryPercent%" else "—"}"
                            } else {
                                status
                            },
                            fontSize = 10.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        IconButton(onClick = onKeyboardClick) {
            KeyboardGlyph(modifier = Modifier.size(24.dp))
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Connections",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun DisconnectedHint(onAddDevice: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BluetoothGlyph(
            modifier = Modifier.size(34.dp),
            tint = TextMuted.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "No host connected",
            color = TextMuted.copy(alpha = 0.8f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        FilledTonalButton(onClick = onAddDevice) {
            Text("Pair a device")
        }
    }
}

@Composable
private fun ClickBar(
    onLeftPress: () -> Unit,
    onLeftRelease: () -> Unit,
    onRightClick: () -> Unit,
    onScroll: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 10.dp)
            .height(86.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClickPad(
            modifier = Modifier.weight(7f).fillMaxHeight(),
            label = "LEFT CLICK",
            onPress = onLeftPress,
            onRelease = onLeftRelease
        )
        ScrollStrip(
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            onScroll = onScroll
        )
        ClickPad(
            modifier = Modifier.weight(3f).fillMaxHeight(),
            label = "RIGHT",
            onPress = {},
            onRelease = onRightClick
        )
    }
}

@Composable
private fun ClickPad(
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val color by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(90),
        label = "click-pad"
    )
    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    onPress()
                    try {
                        tryAwaitRelease()
                    } finally {
                        pressed = false
                        onRelease()
                    }
                }
            )
        },
        shape = shape,
        color = androidx.compose.ui.graphics.lerp(SurfaceElevated, Mint.copy(alpha = 0.35f), color),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScrollStrip(
    onScroll: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SurfaceHigh)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    onScroll(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickSwitchSheet(
    hosts: List<SavedHost>,
    connectedAddress: String?,
    onSelect: (SavedHost) -> Unit,
    onManage: () -> Unit,
    onAddDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text("Switch device", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "A phone can drive one host at a time.",
                fontSize = 12.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (hosts.isEmpty()) {
                Text(
                    text = "No paired devices yet.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                hosts.forEach { host ->
                    val isActive = host.address == connectedAddress
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { closeThen { onSelect(host) } }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DeviceTile(
                            kind = hostKind(host),
                            modifier = Modifier.size(40.dp),
                            background = if (isActive) Mint.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(host.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (host.address == connectedAddress) "Connected" else "Tap to switch",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        if (isActive) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Active",
                                tint = Mint
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { closeThen(onManage) }, modifier = Modifier.weight(1f)) {
                    Text("Manage")
                }
                Button(onClick = { closeThen(onAddDevice) }, modifier = Modifier.weight(1f)) {
                    Text("Pair new device")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyboardDialog(
    hostName: String?,
    layout: HostKeyboardLayout,
    onLayoutChange: (HostKeyboardLayout) -> Unit,
    onSendText: (String) -> Unit,
    onKey: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = SurfaceHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KeyboardGlyph(modifier = Modifier.size(22.dp), tint = Mint)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keyboard", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = hostName?.let { "Typing to $it" } ?: "Not connected",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    KeyboardLayoutSelector(
                        selected = layout,
                        onSelect = onLayoutChange
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Match this to the computer's keyboard layout so characters arrive correctly.",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("Type here, then send", fontSize = 13.sp) },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (text.isNotEmpty()) {
                                onSendText(text)
                                text = ""
                            }
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickKey("⌫") { onKey(MouseConfig.KEY_BACKSPACE, 0) }
                    QuickKey("↵") { onKey(MouseConfig.KEY_ENTER, 0) }
                    QuickKey("Tab") { onKey(MouseConfig.KEY_TAB, 0) }
                    QuickKey("Esc") { onKey(MouseConfig.KEY_ESCAPE, 0) }
                    QuickKey("←") { onKey(MouseConfig.KEY_LEFT_ARROW, 0) }
                    QuickKey("↑") { onKey(MouseConfig.KEY_UP_ARROW, 0) }
                    QuickKey("↓") { onKey(MouseConfig.KEY_DOWN_ARROW, 0) }
                    QuickKey("→") { onKey(MouseConfig.KEY_RIGHT_ARROW, 0) }
                    QuickKey("Win") { onKey(0, MouseConfig.MOD_LEFT_GUI) }
                    QuickKey("Alt+Tab") {
                        onKey(MouseConfig.KEY_TAB, MouseConfig.MOD_LEFT_ALT)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            if (text.isNotEmpty()) {
                                onSendText(text)
                                text = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
private fun QuickKey(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 6.dp
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Text(label, fontSize = 12.sp)
    }
}

internal fun hostKind(host: SavedHost): DeviceKind =
    DeviceKind.fromStorage(host.icon) ?: DeviceKind.guess(host.name)
