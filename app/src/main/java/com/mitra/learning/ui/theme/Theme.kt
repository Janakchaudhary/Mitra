package com.mitra.learning.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MitraColors = lightColorScheme(
    primary = Color(0xFF5B4BDB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E2FF),
    onPrimaryContainer = Color(0xFF1D1456),
    secondary = Color(0xFF16796F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBEFE8),
    onSecondaryContainer = Color(0xFF063C36),
    tertiary = Color(0xFFE26A3F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCCF),
    onTertiaryContainer = Color(0xFF4D1607),
    background = Color(0xFFFFFAF2),
    onBackground = Color(0xFF211A18),
    surface = Color(0xFFFFFCF8),
    onSurface = Color(0xFF211A18),
    surfaceVariant = Color(0xFFF1EAF5),
    outlineVariant = Color(0xFFD7CFE0),
)

private val MitraShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun MitraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MitraColors,
        shapes = MitraShapes,
        content = content,
    )
}
