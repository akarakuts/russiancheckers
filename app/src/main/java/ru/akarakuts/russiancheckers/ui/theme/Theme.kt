package ru.akarakuts.russiancheckers.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    surface = BoardLight,
    onSurface = Color(0xFF1C1B1F),
)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    surface = Color(0xFF1E1B16),
    onSurface = Color(0xFFE8E0D8),
)

/** Board palette that follows the system light/dark theme. */
@Composable
fun boardPalette(): BoardPalette =
    if (isSystemInDarkTheme()) NightBoardPalette else LightBoardPalette

/** Material 3 shell: dynamic colour on API 31+, else static schemes tuned for the board. */
@Composable
fun RussianCheckersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
