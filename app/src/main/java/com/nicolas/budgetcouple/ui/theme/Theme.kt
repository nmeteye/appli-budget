package com.nicolas.budgetcouple.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF1C1B18)
val Cream = Color(0xFFF5F2EA)
val Paper = Color(0xFFFFFDF7)
val Sage = Color(0xFF5C7A6B)
val SageLite = Color(0xFFE3EBE6)
val Ocre = Color(0xFFC08A3E)
val Brique = Color(0xFFA8443A)
val LineCol = Color(0xFFD8D2C4)
val Muted = Color(0xFF8A8579)

private val LightColors = lightColorScheme(
    primary = Sage,
    secondary = Ocre,
    tertiary = Brique,
    background = Cream,
    surface = Paper,
    onPrimary = Cream,
    onBackground = Ink,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Sage,
    secondary = Ocre,
    tertiary = Brique,
    background = Color(0xFF14130F),
    surface = Color(0xFF1F1D18),
    onBackground = Cream,
    onSurface = Cream,
)

@Composable
fun BudgetCoupleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
