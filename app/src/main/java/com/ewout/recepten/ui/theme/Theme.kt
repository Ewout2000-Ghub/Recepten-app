package com.ewout.recepten.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ReceptenColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = BrandSurface,
    primaryContainer = BrandOrange,
    onPrimaryContainer = BrandSurface,
    secondary = BrandOrangeDark,
    onSecondary = BrandSurface,
    background = BrandCream,
    onBackground = TextPrimary,
    surface = BrandSurface,
    onSurface = TextPrimary,
    surfaceVariant = BrandSurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = DividerSoft,
    outlineVariant = DividerSoft
)

@Composable
fun ReceptenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ReceptenColorScheme,
        typography = ReceptenTypography,
        content = content
    )
}
