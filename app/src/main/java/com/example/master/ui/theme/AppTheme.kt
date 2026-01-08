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
                background = Color(0xFF0F1116),
                surface = Color(0xFF171A22),
                onPrimary = Color(0xFF0B0C10),
                onSecondary = Color(0xFF0B0C10),
                onBackground = Color(0xFFF9FAFB),
                onSurface = Color(0xFFF9FAFB)
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
                background = Color(0xFF0F1116),
                surface = Color(0xFF171A22),
                onPrimary = Color(0xFF0B0C10),
                onSecondary = Color(0xFF0B0C10),
                onBackground = Color(0xFFF9FAFB),
                onSurface = Color(0xFFF9FAFB)
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
                background = Color(0xFF0F1116),
                surface = Color(0xFF171A22),
                onPrimary = Color(0xFF0B0C10),
                onSecondary = Color(0xFF0B0C10),
                onBackground = Color(0xFFF9FAFB),
                onSurface = Color(0xFFF9FAFB)
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
                background = Color(0xFF0F1116),
                surface = Color(0xFF171A22),
                onPrimary = Color(0xFF0B0C10),
                onSecondary = Color(0xFF0B0C10),
                onBackground = Color(0xFFF9FAFB),
                onSurface = Color(0xFFF9FAFB)
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
                background = Color(0xFF0F1116),
                surface = Color(0xFF171A22),
                onPrimary = Color(0xFF0B0C10),
                onSecondary = Color(0xFF0B0C10),
                onBackground = Color(0xFFF9FAFB),
                onSurface = Color(0xFFF9FAFB)
            ),
            preferDark = true
        )
    )

    fun findByName(name: String): AppThemePalette {
        return themes.firstOrNull { it.name == name } ?: themes.first()
    }
}
