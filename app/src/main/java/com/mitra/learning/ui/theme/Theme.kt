package com.mitra.learning.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MitraColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF2676B8),
    secondaryContainer = Color(0xFFD5E9FF),
    tertiary = Color(0xFFE26D3F),
    tertiaryContainer = Color(0xFFFFDBCF),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF2ECF4),
)

@Composable
fun MitraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MitraColors,
        content = content,
    )
}
