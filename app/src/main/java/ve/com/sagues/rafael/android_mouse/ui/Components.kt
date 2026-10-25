package ve.com.sagues.rafael.android_mouse.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.doOnLayout

/** Subtle dot texture used by the touch surface to give it depth. */
fun Modifier.dotGrid(
    spacing: Dp = 26.dp,
    dotRadius: Dp = 0.9.dp,
    dotColor: Color = Color.White.copy(alpha = 0.045f)
): Modifier = drawBehind {
    val step = spacing.toPx()
    val radius = dotRadius.toPx()
    if (step <= 0f) return@drawBehind
    var y = step / 2f
    while (y < size.height) {
        var x = step / 2f
        while (x < size.width) {
            drawCircle(dotColor, radius, Offset(x, y))
            x += step
        }
        y += step
    }
}

/** Thick card surface used across the app. */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "status-dot")
    val glow by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status-dot-glow"
    )
    val alpha = if (pulsing) glow else 1f
    Canvas(modifier = modifier.size(14.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color.copy(alpha = 0.18f * alpha + 0.08f), radius = size.minDimension / 2f, center = center)
        drawCircle(color.copy(alpha = alpha), radius = size.minDimension / 4.4f, center = center)
    }
}

/** Prevents Android's system back gesture from stealing swipes on the touch surface. */
@Composable
fun EdgeLockEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled, view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (enabled) {
                view.doOnLayout {
                    view.systemGestureExclusionRects = listOf(
                        android.graphics.Rect(0, 0, view.width, view.height)
                    )
                }
            } else {
                view.systemGestureExclusionRects = emptyList()
            }
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.systemGestureExclusionRects = emptyList()
            }
        }
    }
}

@Composable
fun readBatteryPercent(): Int {
    val context = LocalContext.current
    fun read(): Int {
        val manager = context.getSystemService(BatteryManager::class.java)
        return manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: -1
    }

    var percent by remember { mutableIntStateOf(read()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                percent = if (level >= 0 && scale > 0) (level * 100 / scale) else read()
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return percent
}

fun lastSeenLabel(timestamp: Long): String {
    if (timestamp <= 0L) return "Not connected yet"
    val relative = DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
    return "Last connected $relative"
}

/** Round tinted tile that holds a device glyph. */
@Composable
fun DeviceTile(
    kind: DeviceKind,
    modifier: Modifier = Modifier,
    background: Color = Color.White.copy(alpha = 0.06f),
    tint: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(background, CircleShape),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        DeviceGlyph(kind = kind, modifier = Modifier.size(26.dp), tint = tint)
    }
}
