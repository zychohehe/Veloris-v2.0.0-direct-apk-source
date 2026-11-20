package com.veloris.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF0B0B12)
val Panel = Color(0xFF16162A)
val Accent = Color(0xFF8B7CFF)
val Accent2 = Color(0xFF3DE0C5)
val TextHi = Color(0xFFF2F2F8)
val TextLo = Color(0xFF9A9AB0)

@Composable
fun VelorisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, secondary = Accent2, background = Ink, surface = Panel,
            onBackground = TextHi, onSurface = TextHi, onPrimary = Ink,
        ),
        content = content,
    )
}
