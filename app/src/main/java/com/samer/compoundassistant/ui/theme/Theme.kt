package com.samer.compoundassistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Vibrant brand (teal + purple) + warm secondary
private val BrandPrimary = Color(0xFF2BB3C0)   // teal
private val BrandSecondary = Color(0xFF8A4FFF) // purple
private val BrandTertiary = Color(0xFFFFB020)  // amber

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimary.copy(alpha = .15f),
    onPrimaryContainer = BrandPrimary,

    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondary.copy(alpha = .15f),
    onSecondaryContainer = BrandSecondary,

    tertiary = BrandTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = BrandTertiary.copy(alpha = .18f),
    onTertiaryContainer = BrandTertiary,

    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF111418),
    surface = Color.White,
    onSurface = Color(0xFF1D2025),
    surfaceVariant = Color(0xFFE9ECF3),
    outline = Color(0xFFB8C0CC)
)

private val DarkColors = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.Black,
    primaryContainer = BrandPrimary.copy(alpha = .25f),
    onPrimaryContainer = Color(0xFF001F21),

    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondary.copy(alpha = .28f),

    tertiary = BrandTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = BrandTertiary.copy(alpha = .25f),

    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE9EDF3),
    surface = Color(0xFF171920),
    onSurface = Color(0xFFE9EDF3),
    surfaceVariant = Color(0xFF252A33),
    outline = Color(0xFF3E4653)
)

private val Shapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    small      = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium     = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large      = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Composable
fun CompoundTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // (Optional) swap to dynamicColor if you want Android 12+ wallpaper colors:
    // val colors = if (Build.VERSION.SDK_INT >= 31) dynamicLightColorScheme(LocalContext.current) else LightColors
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes,
        typography = Typography().run {
            copy(
                headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                titleMedium = titleMedium.copy(fontWeight = FontWeight.Medium),
                labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        content = content
    )
}
