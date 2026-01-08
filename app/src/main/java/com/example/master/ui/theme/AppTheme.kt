package com.example.master.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class AppThemePalette(
    val name: String,
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val preferDark: Boolean = false
)

object ThemeCatalog {
    val themes: List<AppThemePalette> = listOf(
        AppThemePalette(
            name = "Sunrise",
            lightScheme = lightColorScheme(
                primary = Color(0xFFFF8A3D),
                secondary = Color(0xFFFFC857),
                background = Color(0xFFFFF7ED),
                surface = Color(0xFFFFF1E6),
                onPrimary = Color(0xFFFFFFFF),
                onSecondary = Color(0xFF1F2937),
                onBackground = Color(0xFF1F2937),
                onSurface = Color(0xFF1F2937)
            ),
            darkScheme = darkColorScheme(
                primary = Color(0xFFFF8A3D),
                secondary = Color(0xFFFFC857),
                background = Color(0xFF12131A),
                surface = Color(0xFF1A1B25),
                onPrimary = Color(0xFF0B0B0F),
                onSecondary = Color(0xFF0B0B0F),
                onBackground = Color(0xFFE5E7EB),
                onSurface = Color(0xFFE5E7EB)
            )
        ),
        AppThemePalette(
            name = "Ocean",
            lightScheme = lightColorScheme(
                primary = Color(0xFF118AB2),
                secondary = Color(0xFF06D6A0),
                background = Color(0xFFF0FBFF),
                surface = Color(0xFFE6F6FF),
                onPrimary = Color(0xFFFFFFFF),
                onSecondary = Color(0xFF0F172A),
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            ),
            darkScheme = darkColorScheme(
                primary = Color(0xFF3FB9E0),
                secondary = Color(0xFF4CE9C2),
                background = Color(0xFF0B1720),
                surface = Color(0xFF11202A),
                onPrimary = Color(0xFF041016),
                onSecondary = Color(0xFF041016),
                onBackground = Color(0xFFE2E8F0),
                onSurface = Color(0xFFE2E8F0)
            )
        ),
        AppThemePalette(
            name = "Forest",
            lightScheme = lightColorScheme(
                primary = Color(0xFF2F855A),
                secondary = Color(0xFF68D391),
                background = Color(0xFFF1FBF5),
                surface = Color(0xFFE7F6ED),
                onPrimary = Color(0xFFFFFFFF),
                onSecondary = Color(0xFF0F172A),
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            ),
            darkScheme = darkColorScheme(
                primary = Color(0xFF45C38A),
                secondary = Color(0xFF9AE6B4),
                background = Color(0xFF0C1A12),
                surface = Color(0xFF13241A),
                onPrimary = Color(0xFF07120D),
                onSecondary = Color(0xFF07120D),
                onBackground = Color(0xFFE2E8F0),
                onSurface = Color(0xFFE2E8F0)
            )
        ),
        AppThemePalette(
            name = "Rose",
            lightScheme = lightColorScheme(
                primary = Color(0xFFE11D48),
                secondary = Color(0xFFF472B6),
                background = Color(0xFFFFF1F2),
                surface = Color(0xFFFFE4E6),
                onPrimary = Color(0xFFFFFFFF),
                onSecondary = Color(0xFF0F172A),
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            ),
            darkScheme = darkColorScheme(
                primary = Color(0xFFFB7185),
                secondary = Color(0xFFF9A8D4),
                background = Color(0xFF1B0D12),
                surface = Color(0xFF25121A),
                onPrimary = Color(0xFF12060B),
                onSecondary = Color(0xFF12060B),
                onBackground = Color(0xFFE5E7EB),
                onSurface = Color(0xFFE5E7EB)
            )
        ),
        AppThemePalette(
            name = "Midnight",
            lightScheme = lightColorScheme(
                primary = Color(0xFF4C6FFF),
                secondary = Color(0xFF7C3AED),
                background = Color(0xFFF4F6FF),
                surface = Color(0xFFEDE9FE),
                onPrimary = Color(0xFFFFFFFF),
                onSecondary = Color(0xFFFFFFFF),
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A)
            ),
            darkScheme = darkColorScheme(
                primary = Color(0xFF7C8CFF),
                secondary = Color(0xFFA78BFA),
                background = Color(0xFF0B0F1A),
                surface = Color(0xFF121827),
                onPrimary = Color(0xFF0B0F1A),
                onSecondary = Color(0xFF0B0F1A),
                onBackground = Color(0xFFE2E8F0),
                onSurface = Color(0xFFE2E8F0)
            ),
            preferDark = true
        )
    )

    fun findByName(name: String): AppThemePalette {
        return themes.firstOrNull { it.name == name } ?: themes.first()
    }
}
