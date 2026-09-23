package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AquaPrimaryLight,
    onPrimary = AquaDarkBackground,
    primaryContainer = AquaPrimaryDark,
    onPrimaryContainer = AquaLightBackground,
    secondary = AquaSecondaryLight,
    onSecondary = AquaDarkBackground,
    secondaryContainer = AquaSecondaryDark,
    onSecondaryContainer = AquaLightBackground,
    tertiary = AquaTertiaryLight,
    onTertiary = AquaDarkBackground,
    tertiaryContainer = AquaTertiaryDark,
    onTertiaryContainer = AquaLightBackground,
    background = AquaDarkBackground,
    onBackground = AquaDarkOnSurface,
    surface = AquaDarkSurface,
    onSurface = AquaDarkOnSurface,
    surfaceVariant = AquaDarkSurfaceVariant,
    onSurfaceVariant = AquaDarkOnSurfaceVariant,
    error = AquaAlarm
)

private val LightColorScheme = lightColorScheme(
    primary = AquaPrimary,
    onPrimary = AquaLightSurface,
    primaryContainer = AquaLightSurfaceVariant,
    onPrimaryContainer = AquaPrimaryDark,
    secondary = AquaSecondary,
    onSecondary = AquaLightSurface,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = AquaSecondaryDark,
    tertiary = AquaTertiaryDark,
    onTertiary = AquaLightSurface,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = AquaTertiaryDark,
    background = AquaLightBackground,
    onBackground = AquaLightOnSurface,
    surface = AquaLightSurface,
    onSurface = AquaLightOnSurface,
    surfaceVariant = AquaLightSurfaceVariant,
    onSurfaceVariant = AquaLightOnSurfaceVariant,
    error = AquaAlarm
)

@Composable
fun AquaAlertaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our signature crystal-aqua palette for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
