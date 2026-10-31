package com.greekscript.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Aegean blue, whitewashed walls, olive, and a little gold. */
object Pal {
    val Marble = Color(0xFFF6F8FB)
    val Ink = Color(0xFF14213D)
    val Muted = Color(0xFF5B6B88)
    val Aegean = Color(0xFF1456B8)
    val Deep = Color(0xFF0B2E6B)
    val Sky = Color(0xFFE6EFFB)
    val SkyLine = Color(0xFFC9D8EE)
    val Olive = Color(0xFF4F8A3C)
    val OliveDark = Color(0xFF356B26)
    val OliveTint = Color(0xFFE3F1DA)
    val Coral = Color(0xFFD64545)
    val CoralDark = Color(0xFFA83232)
    val CoralTint = Color(0xFFFBE3E0)
    val Gold = Color(0xFFF0B429)
    val GoldDeepText = Color(0xFFB7791F)
    val Lock = Color(0xFFD8DEE8)
    val LockEdge = Color(0xFFB9C3D3)
}

/** Serif system font: its Greek letters look classical and are on every phone. */
val GreekFont: FontFamily = FontFamily.Serif

@Composable
fun GreekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Pal.Aegean,
            onPrimary = Color.White,
            background = Pal.Marble,
            onBackground = Pal.Ink,
            surface = Color.White,
            onSurface = Pal.Ink,
        ),
        content = content,
    )
}
