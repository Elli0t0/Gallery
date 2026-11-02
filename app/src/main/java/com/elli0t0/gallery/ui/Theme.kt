package com.elli0t0.gallery.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GalleryScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFB0B0B0),
    surfaceContainerHigh = Color(0xFF2B2B2B)
)

@Composable
fun GalleryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GalleryScheme, content = content)
}

val PillColor = Color(0xFF2B2B2B)
val PillSelectedColor = Color(0xFF404040)
val CardColor = Color(0xFF242424)
