package com.nicolas.familybudget.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Palette "sage green" / terreuse, calme et lisible pour un app finance.
private val Sage = Color(0xFF5E7C6B)
private val SageDark = Color(0xFF3E5749)
private val SageLight = Color(0xFFD7E3DA)
private val Clay = Color(0xFFB07D56)
private val Sand = Color(0xFFF4F1EA)
private val Ink = Color(0xFF1E2421)

private val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = SageLight,
    onPrimaryContainer = SageDark,
    secondary = Clay,
    onSecondary = Color.White,
    background = Sand,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = SageLight,
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DC0AB),
    onPrimary = Color(0xFF14251B),
    primaryContainer = SageDark,
    onPrimaryContainer = SageLight,
    secondary = Color(0xFFD6A983),
    background = Color(0xFF141816),
    onBackground = Color(0xFFE3E6E2),
    surface = Color(0xFF1B201D),
    onSurface = Color(0xFFE3E6E2),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
)

@Composable
fun FamilyBudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
