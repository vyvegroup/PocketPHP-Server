package com.phpserver.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PhpPrimary,
    onPrimary = PhpOnPrimary,
    primaryContainer = PhpPrimaryContainer,
    onPrimaryContainer = PhpOnPrimaryContainer,
    secondary = PhpSecondary,
    onSecondary = PhpOnSecondary,
    secondaryContainer = PhpSecondaryContainer,
    onSecondaryContainer = PhpOnSecondaryContainer,
    tertiary = PhpTertiary,
    background = PhpBackground,
    onBackground = PhpOnBackground,
    surface = PhpSurface,
    onSurface = PhpOnSurface,
    surfaceVariant = PhpSurfaceVariant,
    onSurfaceVariant = PhpOnSurfaceVariant,
    error = PhpError,
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFC8C5D0),
    inverseSurface = Color(0xFF303034),
    inverseOnSurface = Color(0xFFF3F0FA),
    inversePrimary = Color(0xFFB3C5FF),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB3C5FF),
    onPrimary = Color(0xFF3B3163),
    primaryContainer = Color(0xFF53468B),
    onPrimaryContainer = Color(0xFFE0E0F6),
    secondary = Color(0xFFC8C5D0),
    onSecondary = Color(0xFF303034),
    secondaryContainer = Color(0xFF46464F),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFFC8C5D0),
    background = Color(0xFF1B1B1F),
    onBackground = Color(0xFFE5E1E5),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE5E1E5),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC8C5D0),
    error = Color(0xFFFFB4AB),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF46464F),
    inverseSurface = Color(0xFFE5E1E5),
    inverseOnSurface = Color(0xFF303034),
    inversePrimary = Color(0xFF6650a4),
)

@Composable
fun PHPServerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
