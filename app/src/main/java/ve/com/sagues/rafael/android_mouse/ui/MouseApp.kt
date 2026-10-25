package ve.com.sagues.rafael.android_mouse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import ve.com.sagues.rafael.android_mouse.BluetoothMouseManager
import ve.com.sagues.rafael.android_mouse.ui.theme.Ink

private enum class Screen { TRACKPAD, CONNECTIONS, ADD_CONNECTION }

/**
 * Three page app shell:
 *  Trackpad  <->  Connections  ->  Add connection  -> (auto) Trackpad
 */
@Composable
fun MouseApp(
    mouseManager: BluetoothMouseManager,
    onRequestDiscoverable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiSettings = remember(context) { UiSettings(context) }

    var screenName by rememberSaveable { mutableStateOf(Screen.TRACKPAD.name) }
    val screen = Screen.valueOf(screenName)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mouseManager) {
        mouseManager.connectionEvents.collect { hostName ->
            launch { snackbarHostState.showSnackbar("Connected to $hostName") }
        }
    }

    BackHandler(enabled = screen != Screen.TRACKPAD) {
        screenName = when (screen) {
            Screen.ADD_CONNECTION -> Screen.CONNECTIONS.name
            else -> Screen.TRACKPAD.name
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                },
                label = "app-screen"
            ) { target ->
                when (target) {
                    Screen.TRACKPAD -> TrackpadScreen(
                        mouseManager = mouseManager,
                        uiSettings = uiSettings,
                        onOpenConnections = { screenName = Screen.CONNECTIONS.name },
                        onAddDevice = { screenName = Screen.ADD_CONNECTION.name }
                    )

                    Screen.CONNECTIONS -> ConnectionsScreen(
                        mouseManager = mouseManager,
                        uiSettings = uiSettings,
                        onBack = { screenName = Screen.TRACKPAD.name },
                        onAddDevice = { screenName = Screen.ADD_CONNECTION.name }
                    )

                    Screen.ADD_CONNECTION -> AddConnectionScreen(
                        mouseManager = mouseManager,
                        onBack = { screenName = Screen.CONNECTIONS.name },
                        onRequestDiscoverable = onRequestDiscoverable,
                        onConnected = { screenName = Screen.TRACKPAD.name }
                    )
                }
            }
        }
    }
}
