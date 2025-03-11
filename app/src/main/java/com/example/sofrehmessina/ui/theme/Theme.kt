package com.example.sofrehmessina.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sofrehmessina.data.model.ThemeOption
import com.example.sofrehmessina.util.LocaleHelper
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.sofrehmessina.ui.screens.user.SettingsViewModel

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = TextPrimaryDark,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = TextPrimaryDark,
    secondary = Secondary,
    onSecondary = TextPrimaryDark,
    tertiary = Accent,
    onTertiary = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    error = Error,
    onError = TextPrimaryDark,
    surfaceTint = PrimaryLight.copy(alpha = 0.1f)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryDark,
    onPrimary = TextPrimaryDark,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = Secondary,
    onSecondary = TextPrimaryDark,
    tertiary = Accent,
    onTertiary = TextPrimaryDark,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    error = Error,
    onError = TextPrimaryDark,
    surfaceTint = PrimaryLight.copy(alpha = 0.1f)
)

@Composable
fun SofrehMessinaTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val settings by settingsViewModel.settings.collectAsState()
    val context = LocalContext.current
    
    // Read theme directly from preferences for immediate application
    // This ensures the theme is applied correctly even after app restart
    val savedTheme = remember {
        try {
            val prefs = context.getSharedPreferences("SofrehMessinaPrefs", Context.MODE_PRIVATE)
            val themeName = prefs.getString("app_theme", ThemeOption.SYSTEM.name)
            try {
                ThemeOption.valueOf(themeName ?: ThemeOption.SYSTEM.name)
            } catch (e: Exception) {
                Log.e("Theme", "Error parsing theme from prefs: $themeName", e)
                ThemeOption.SYSTEM
            }
        } catch (e: Exception) {
            Log.e("Theme", "Error reading theme preference", e)
            ThemeOption.SYSTEM
        }
    }
    
    // Observe theme changes
    val themeChangedEvent by remember { settingsViewModel.themeChangedEvent }.collectAsState(initial = null)
    
    // Log theme information for debugging
    Log.d("Theme", "Current theme setting: ${settings.theme}, saved theme: $savedTheme")
    if (themeChangedEvent != null) {
        Log.d("Theme", "Theme change detected: $themeChangedEvent")
        // Force recomposition when theme changes
        LaunchedEffect(themeChangedEvent) {
            Log.d("Theme", "Applying theme change: $themeChangedEvent")
        }
    }
    
    // Use the saved theme value for immediate updates
    val useDarkTheme = when (savedTheme) {
        ThemeOption.LIGHT -> false
        ThemeOption.DARK -> true
        ThemeOption.SYSTEM -> isSystemInDarkTheme()
    }
    
    // Theme changes will trigger SideEffect execution
    DisposableEffect(useDarkTheme) {
        Log.d("Theme", "DisposableEffect triggered with useDarkTheme: $useDarkTheme")
        onDispose {}
    }
    
    // Determine which typography to use based on language setting
    val currentLanguage = LocaleHelper.getSelectedLanguageCode(context)
    val typography = when (currentLanguage) {
        LocaleHelper.LANGUAGE_PERSIAN -> PersianTypography
        else -> Typography
    }
    
    // Determine layout direction based on language
    val layoutDirection = if (currentLanguage == LocaleHelper.LANGUAGE_PERSIAN) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val currentContext = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(currentContext) else dynamicLightColorScheme(currentContext)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Modern approach for system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Still need to set the color for visual consistency across versions
            // Starting with Android 12 this is ignored in favor of Material You, 
            // but we set it for older versions
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            
            // Configure the appearance of the system bars (dark/light)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    // Set layout direction based on language
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}