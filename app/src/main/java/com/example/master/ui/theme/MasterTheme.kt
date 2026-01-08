package com.example.master.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration

object ThemeManager {
    private val _current = mutableStateOf(ThemeCatalog.themes.first())
    val current: State<AppThemePalette> = _current
    private val _darkModeOverride = mutableStateOf<Boolean?>(null)
    val darkModeOverride: State<Boolean?> = _darkModeOverride

    fun applyTheme(name: String) {
        _current.value = ThemeCatalog.findByName(name)
    }

    fun setDarkMode(enabled: Boolean) {
        _darkModeOverride.value = enabled
    }

    fun getDarkModeOverride(): Boolean? = _darkModeOverride.value
}

@Composable
fun MasterTheme(
    palette: AppThemePalette = ThemeManager.current.value,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSystemDark =
        (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val forced = ThemeManager.darkModeOverride.value
    val useDark = forced ?: (if (palette.preferDark) true else isSystemDark)
    val colors = if (useDark) palette.darkScheme else palette.lightScheme

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
