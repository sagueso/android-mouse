package ve.com.sagues.rafael.android_mouse.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

enum class DeviceKind(val label: String) {
    LAPTOP("Laptop"),
    DESKTOP("Desktop"),
    TV("TV"),
    PHONE("Phone"),
    TABLET("Tablet");

    companion object {
        fun fromStorage(value: String?): DeviceKind? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }

        /** Heuristic guess based on the host's Bluetooth name. */
        fun guess(name: String): DeviceKind {
            val lower = name.lowercase()
            return when {
                lower.contains("tv") || lower.contains("bravia") || lower.contains("roku") ||
                    lower.contains("fire") || lower.contains("shield") -> TV
                lower.contains("macbook") || lower.contains("laptop") || lower.contains("notebook") -> LAPTOP
                lower.contains("imac") || lower.contains("desktop") || lower.contains("pc") ||
                    lower.contains("windows") || lower.contains("tower") -> DESKTOP
                lower.contains("tab") || lower.contains("ipad") -> TABLET
                lower.contains("phone") || lower.contains("pixel") || lower.contains("galaxy") -> PHONE
                else -> LAPTOP
            }
        }
    }
}

@Composable
fun DeviceGlyph(
    kind: DeviceKind,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    strokeWidth: Dp = 1.6.dp
) {
    val color = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val stroke = strokeWidth.toPx()
        val strokeStyle = Stroke(width = stroke, cap = StrokeCap.Round)
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)

        when (kind) {
            DeviceKind.LAPTOP -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(3.5f, 5f),
                    size = Size(17f * unit, 10.5f * unit),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.6f * unit),
                    style = strokeStyle
                )
                drawLine(color, p(2f, 18f), p(22f, 18f), stroke, StrokeCap.Round)
                drawLine(color, p(5f, 18f), p(19f, 18f), stroke * 1.6f, StrokeCap.Round)
            }
            DeviceKind.DESKTOP -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(2.5f, 4f),
                    size = Size(19f * unit, 12.5f * unit),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.6f * unit),
                    style = strokeStyle
                )
                drawLine(color, p(12f, 16.5f), p(12f, 19.5f), stroke, StrokeCap.Round)
                drawLine(color, p(8f, 20.5f), p(16f, 20.5f), stroke, StrokeCap.Round)
            }
            DeviceKind.TV -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(1.5f, 4.5f),
                    size = Size(21f * unit, 12.5f * unit),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8f * unit),
                    style = strokeStyle
                )
                drawLine(color, p(8f, 17f), p(5.5f, 21f), stroke, StrokeCap.Round)
                drawLine(color, p(16f, 17f), p(18.5f, 21f), stroke, StrokeCap.Round)
            }
            DeviceKind.PHONE -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(7f, 2.5f),
                    size = Size(10f * unit, 19f * unit),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f * unit),
                    style = strokeStyle
                )
                drawLine(color, p(10.5f, 18.7f), p(13.5f, 18.7f), stroke, StrokeCap.Round)
            }
            DeviceKind.TABLET -> {
                drawRoundRect(
                    color = color,
                    topLeft = p(4.5f, 2.5f),
                    size = Size(15f * unit, 19f * unit),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8f * unit),
                    style = strokeStyle
                )
                drawCircle(color, radius = stroke * 1.2f, center = p(12f, 18.7f))
            }
        }
    }
}

@Composable
fun KeyboardGlyph(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val color = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val stroke = 1.5f * unit
        val topLeft = Offset(1.5f * unit, 5.5f * unit)
        val panelSize = Size(21f * unit, 13f * unit)
        drawRoundRect(
            color = color,
            topLeft = topLeft,
            size = panelSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f * unit),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        val dotRadius = 0.85f * unit
        listOf(7.5f, 10.5f, 13.5f, 16.5f).forEach { x ->
            drawCircle(color, dotRadius, Offset(x * unit, 9.5f * unit))
        }
        listOf(8.5f, 11.5f, 14.5f).forEach { x ->
            drawCircle(color, dotRadius, Offset(x * unit, 12.2f * unit))
        }
        drawLine(
            color,
            Offset(8.5f * unit, 15.3f * unit),
            Offset(15.5f * unit, 15.3f * unit),
            stroke,
            StrokeCap.Round
        )
    }
}

@Composable
fun BluetoothGlyph(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val color = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val path = Path().apply {
            moveTo(12f * unit, 2.5f * unit)
            lineTo(12f * unit, 21.5f * unit)
            lineTo(18f * unit, 16.5f * unit)
            lineTo(6f * unit, 7.5f * unit)
            moveTo(12f * unit, 2.5f * unit)
            lineTo(18f * unit, 7.5f * unit)
            lineTo(6f * unit, 16.5f * unit)
        }
        drawPath(path, color = color, style = Stroke(width = 1.8f * unit, cap = StrokeCap.Round))
    }
}

/**
 * Expanding Bluetooth rings used while the phone is discoverable.
 */
@Composable
fun BluetoothRadar(
    active: Boolean,
    modifier: Modifier = Modifier,
    color: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar-wave"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            for (i in 0..2) {
                val progress = if (active) (wave + i / 3f) % 1f else 0f
                val ringRadius = radius * (0.28f + progress * 0.72f)
                val alpha = if (active) ((1f - progress) * 0.55f) else 0f
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
            drawCircle(
                color = color.copy(alpha = if (active) 0.16f else 0.08f),
                radius = radius * 0.34f,
                center = center
            )
        }
        BluetoothGlyph(
            modifier = Modifier.size(40.dp),
            tint = if (active) color else color.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun EmptyStateIllustration(
    modifier: Modifier = Modifier,
    tint: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "empty")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty-pulse"
    )
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 160f
        val stroke = 2.4f * unit
        // Laptop
        drawRoundRect(
            color = tint.copy(alpha = 0.85f),
            topLeft = Offset(18f * unit, 48f * unit),
            size = Size(88f * unit, 56f * unit),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f * unit),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = tint.copy(alpha = 0.85f),
            start = Offset(8f * unit, 112f * unit),
            end = Offset(116f * unit, 112f * unit),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Phone
        drawRoundRect(
            color = tint,
            topLeft = Offset(96f * unit, 62f * unit),
            size = Size(42f * unit, 78f * unit),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(9f * unit),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = tint,
            start = Offset(110f * unit, 131f * unit),
            end = Offset(124f * unit, 131f * unit),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Link waves
        listOf(6f, 12f, 18f).forEachIndexed { index, spread ->
            drawArc(
                color = tint.copy(alpha = pulse * (1f - index * 0.22f)),
                startAngle = -55f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(
                    (42f - spread) * unit,
                    (62f - spread) * unit
                ),
                size = Size((52f + spread * 2) * unit, (52f + spread * 2) * unit),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        drawCircle(color = tint, radius = 4.5f * unit, center = Offset(134f, 44f) * unit)
    }
}

/** Draws a round "connected" check mark that scales in when [visible] becomes true. */@Composable
fun SuccessBadge(
    visible: Boolean,
    modifier: Modifier = Modifier,
    color: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "success-scale"
    )
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f * scale
        if (radius <= 0f) return@Canvas
        drawCircle(color.copy(alpha = 0.16f), radius = radius)
        drawCircle(color, radius = radius, style = Stroke(width = 3f))
        val check = Path().apply {
            moveTo(center.x - radius * 0.42f, center.y + radius * 0.02f)
            lineTo(center.x - radius * 0.1f, center.y + radius * 0.34f)
            lineTo(center.x + radius * 0.46f, center.y - radius * 0.3f)
        }
        drawPath(check, color, style = Stroke(width = radius * 0.14f, cap = StrokeCap.Round))
    }
}
