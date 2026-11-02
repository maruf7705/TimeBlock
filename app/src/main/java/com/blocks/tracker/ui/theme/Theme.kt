package com.blocks.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AccentIndigo,
    onPrimary = ColorWhite,
    background = PaperSurface,
    surface = PaperSurface,
    onSurface = InkPrimary,
    onSurfaceVariant = InkSecondary,
    outlineVariant = PaperDivider
)

@Composable
fun BlocksTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
