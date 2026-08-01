package com.videoChatting.echat.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dynamic Theme Configuration object
object ThemeConfig {
    var themeSelection = mutableStateOf("dark") // default is dark as requested
}

// Rich Premium Background Gradient for beautiful Glassmorphism blending (Dark Mode)
val PremiumBackgroundGradientDark = Brush.verticalGradient(
    colors = listOf(CyberMidnight, ObsidianBlack, DeepIndigo)
)

// Clean Soft Slate Gradient for Light Mode
val PremiumBackgroundGradientLight = Brush.verticalGradient(
    colors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE2E8F0))
)

@Composable
fun getThemeBackgroundGradient(): Brush {
    val selection = ThemeConfig.themeSelection.value
    val isDark = when (selection) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    return if (isDark) PremiumBackgroundGradientDark else PremiumBackgroundGradientLight
}

@Composable
fun getThemeGlassBackground(): Color {
    val selection = ThemeConfig.themeSelection.value
    val isDark = when (selection) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    return if (isDark) GlassBackground else Color.White.copy(alpha = 0.2f)
}

@Composable
fun getThemeGlassBorder(): Color {
    val selection = ThemeConfig.themeSelection.value
    val isDark = when (selection) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    return if (isDark) GlassBorder else Color(0xFFCBD5E1) // clean light slate border
}

@Composable
fun getThemeGlassBorderSelected(): Color {
    val selection = ThemeConfig.themeSelection.value
    val isDark = when (selection) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    return if (isDark) GlassBorderSelected else ElectricIndigo
}

@Composable
fun getThemeTextColor(): Color {
    val selection = ThemeConfig.themeSelection.value
    val isDark = when (selection) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    return if (isDark) Color.White else Color(0xFF0F172A)
}

@Composable
fun getThemeSubTextColor(): Color {
    val selection = ThemeConfig.themeSelection.value
    val isDark = when (selection) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    return if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
}

private val DarkColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    secondary = CyberCyan,
    tertiary = NeonRose,
    background = CyberMidnight,
    surface = DeepIndigo,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f)
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricIndigo,
    secondary = CyberCyan,
    tertiary = NeonRose,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun EChatTheme(
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val selection = ThemeConfig.themeSelection.value
    val darkTheme = when (selection) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = if (darkTheme) ObsidianBlack.toArgb() else Color.White.toArgb()
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EChatTypography,
        content = content
    )
}
