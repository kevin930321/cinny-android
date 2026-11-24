package `in`.cinny.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import `in`.cinny.android.data.CinnyThemeMode
import kotlin.math.abs

@Immutable
data class CinnyExtendedColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceHover: Color,
    val surfaceActive: Color,
    val containerLine: Color,
    val surfaceLine: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val critical: Color,
    val criticalContainer: Color,
    val mxidPalette: List<Color>
)

private val LightMxidPalette = listOf(
    Color(0xFF007AE6),
    Color(0xFF990094),
    Color(0xFF00996E),
    Color(0xFFE60041),
    Color(0xFFE65C00),
    Color(0xFF009699),
    Color(0xFF0600E6),
    Color(0xFF4EB300)
)

private val DarkMxidPalette = listOf(
    Color(0xFF80C4FF),
    Color(0xFFFF99FB),
    Color(0xFF66FFD4),
    Color(0xFFFF80A4),
    Color(0xFFFFA266),
    Color(0xFF33FAFF),
    Color(0xFF9D99FF),
    Color(0xFFBBFF99)
)

// Upstream Cinny WebUI Color Tokens (src/colors.css.ts)
private val LightExtended = CinnyExtendedColors(
    isDark = false,
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAEAEA),
    surfaceHover = Color(0xFFF0F0F0),
    surfaceActive = Color(0xFFE2E2E2),
    containerLine = Color(0xFFBBBBBB),
    surfaceLine = Color(0xFFE0E0E0),
    onSurface = Color(0xFF0E0E0E),
    onSurfaceMuted = Color(0xFF525252),
    primary = Color(0xFF1245A8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC4D0E9),
    onPrimaryContainer = Color(0xFF0D3076),
    success = Color(0xFF017343),
    successContainer = Color(0xFFBFE2D3),
    warning = Color(0xFF864300),
    warningContainer = Color(0xFFEED3B9),
    critical = Color(0xFF9D0F0F),
    criticalContainer = Color(0xFFFAD7D7),
    mxidPalette = LightMxidPalette
)

private val SilverExtended = CinnyExtendedColors(
    isDark = false,
    background = Color(0xFFDEDEDE),
    surface = Color(0xFFEAEAEA),
    surfaceVariant = Color(0xFFD2D2D2),
    surfaceHover = Color(0xFFE0E0E0),
    surfaceActive = Color(0xFFC9C9C9),
    containerLine = Color(0xFFA6A6A6),
    surfaceLine = Color(0xFFC7C7C7),
    onSurface = Color(0xFF0C0C0C),
    onSurfaceMuted = Color(0xFF474747),
    primary = Color(0xFF1245A8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC4D0E9),
    onPrimaryContainer = Color(0xFF0D3076),
    success = Color(0xFF017343),
    successContainer = Color(0xFFBFE2D3),
    warning = Color(0xFF864300),
    warningContainer = Color(0xFFEED3B9),
    critical = Color(0xFF9D0F0F),
    criticalContainer = Color(0xFFFAD7D7),
    mxidPalette = LightMxidPalette
)

private val DarkExtended = CinnyExtendedColors(
    isDark = true,
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF262626),
    surfaceVariant = Color(0xFF333333),
    surfaceHover = Color(0xFF2E2E2E),
    surfaceActive = Color(0xFF3A3A3A),
    containerLine = Color(0xFF4D4D4D),
    surfaceLine = Color(0xFF3A3A3A),
    onSurface = Color(0xFFF2F2F2),
    onSurfaceMuted = Color(0xFFA6A6A6),
    primary = Color(0xFFBDB6EC),
    onPrimary = Color(0xFF2C2843),
    primaryContainer = Color(0xFF413C65),
    onPrimaryContainer = Color(0xFFE3E1F7),
    success = Color(0xFF85E0BA),
    successContainer = Color(0xFF14422F),
    warning = Color(0xFFE3BA91),
    warningContainer = Color(0xFF4A2E14),
    critical = Color(0xFFE69D9D),
    criticalContainer = Color(0xFF521E1E),
    mxidPalette = DarkMxidPalette
)

private val ButterExtended = CinnyExtendedColors(
    isDark = true,
    background = Color(0xFF1A1916),
    surface = Color(0xFF262621),
    surfaceVariant = Color(0xFF33322C),
    surfaceHover = Color(0xFF2E2D27),
    surfaceActive = Color(0xFF3A3931),
    containerLine = Color(0xFF4D4B43),
    surfaceLine = Color(0xFF3C3B34),
    onSurface = Color(0xFFFFFBDE),
    onSurfaceMuted = Color(0xFFACA78D),
    primary = Color(0xFFBDB6EC),
    onPrimary = Color(0xFF2C2843),
    primaryContainer = Color(0xFF413C65),
    onPrimaryContainer = Color(0xFFE3E1F7),
    success = Color(0xFF85E0BA),
    successContainer = Color(0xFF14422F),
    warning = Color(0xFFE3BA91),
    warningContainer = Color(0xFF4A2E14),
    critical = Color(0xFFE69D9D),
    criticalContainer = Color(0xFF521E1E),
    mxidPalette = DarkMxidPalette
)

val LocalCinnyColors = staticCompositionLocalOf { DarkExtended }

object CinnyThemeColors {
    val current: CinnyExtendedColors
        @Composable
        get() = LocalCinnyColors.current
}

fun mxidColorIndex(id: String): Int {
    var hash = 0
    for (ch in id) {
        hash = ((hash shl 5) - hash) + ch.code
    }
    return (abs(hash.toLong()) % 8L).toInt()
}

fun colorMXID(id: String, isDark: Boolean): Color {
    val palette = if (isDark) DarkMxidPalette else LightMxidPalette
    return palette[mxidColorIndex(id)]
}

private fun toMaterialColorScheme(ext: CinnyExtendedColors): ColorScheme {
    return if (ext.isDark) {
        darkColorScheme(
            primary = ext.primary,
            onPrimary = ext.onPrimary,
            primaryContainer = ext.primaryContainer,
            onPrimaryContainer = ext.onPrimaryContainer,
            background = ext.background,
            onBackground = ext.onSurface,
            surface = ext.surface,
            onSurface = ext.onSurface,
            surfaceVariant = ext.surfaceVariant,
            onSurfaceVariant = ext.onSurfaceMuted,
            outline = ext.containerLine,
            outlineVariant = ext.surfaceLine,
            error = ext.critical,
            errorContainer = ext.criticalContainer,
            onError = ext.background,
            onErrorContainer = ext.critical
        )
    } else {
        lightColorScheme(
            primary = ext.primary,
            onPrimary = ext.onPrimary,
            primaryContainer = ext.primaryContainer,
            onPrimaryContainer = ext.onPrimaryContainer,
            background = ext.background,
            onBackground = ext.onSurface,
            surface = ext.surface,
            onSurface = ext.onSurface,
            surfaceVariant = ext.surfaceVariant,
            onSurfaceVariant = ext.onSurfaceMuted,
            outline = ext.containerLine,
            outlineVariant = ext.surfaceLine,
            error = ext.critical,
            errorContainer = ext.criticalContainer,
            onError = Color.White,
            onErrorContainer = ext.critical
        )
    }
}

@Composable
fun CinnyTheme(
    themeMode: CinnyThemeMode = CinnyThemeMode.System,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val resolvedMode = when (themeMode) {
        CinnyThemeMode.System -> if (systemDark) CinnyThemeMode.Dark else CinnyThemeMode.Light
        else -> themeMode
    }
    val extended = when (resolvedMode) {
        CinnyThemeMode.Light -> LightExtended
        CinnyThemeMode.Silver -> SilverExtended
        CinnyThemeMode.Dark -> DarkExtended
        CinnyThemeMode.Butter -> ButterExtended
        CinnyThemeMode.System -> if (systemDark) DarkExtended else LightExtended
    }

    CompositionLocalProvider(LocalCinnyColors provides extended) {
        MaterialTheme(
            colorScheme = toMaterialColorScheme(extended),
            content = content
        )
    }
}
