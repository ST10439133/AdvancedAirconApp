package com.prog7314.arcticflow.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class ThemeState(
    val isDarkMode: Boolean = false,
    val dynamicColor: Boolean = true
)

class ThemeManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeState(): ThemeState {
        return ThemeState(
            isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false),
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        )
    }

    fun saveThemeState(state: ThemeState) {
        prefs.edit().apply {
            putBoolean(KEY_DARK_MODE, state.isDarkMode)
            putBoolean(KEY_DYNAMIC_COLOR, state.dynamicColor)
            apply()
        }
    }
}

@Composable
fun ArcticFlowTheme(
    themeState: ThemeState,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        themeState.dynamicColor -> {
            if (themeState.isDarkMode) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        else -> {
            if (themeState.isDarkMode) darkColorScheme()
            else lightColorScheme()
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as androidx.activity.ComponentActivity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !themeState.isDarkMode
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}