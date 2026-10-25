package ve.com.sagues.rafael.android_mouse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF003824),
    primaryContainer = MintDeep,
    onPrimaryContainer = Color(0xFFC9F7E2),
    secondary = AccentBlue,
    onSecondary = Color(0xFF0B2545),
    secondaryContainer = Color(0xFF17293F),
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = AccentAmber,
    onTertiary = Color(0xFF3D2A00),
    tertiaryContainer = Color(0xFF3B2C0C),
    onTertiaryContainer = Color(0xFFFFE7B8),
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Surface,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = Color(0xFF25252B),
    outline = OutlineSoft,
    outlineVariant = Color(0xFF232329),
    error = Danger,
    onError = Color(0xFF3A0909),
    scrim = Color(0xFF000000)
)

// Kept for completeness if the app is ever forced into light mode.
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0E7A55),
    secondary = Color(0xFF31557F),
    tertiary = Color(0xFF7A5A00)
)

@Composable
fun AndroidmouseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
