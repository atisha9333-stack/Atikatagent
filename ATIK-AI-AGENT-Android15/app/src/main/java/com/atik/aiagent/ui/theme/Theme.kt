package com.atik.aiagent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AtikColors {
    val ObsidianBg = Color(0xFF070913)
    val SurfaceDark = Color(0xFF0E1324)
    val SurfaceElevated = Color(0xFF151C34)
    val HairlineBorder = Color(0xFF253052)
    val AccentPurple = Color(0xFF8B5CF6)
    val AccentVioletGlow = Color(0xFFA855F7)
    val AccentBlue = Color(0xFF3B82F6)
    val AccentCyan = Color(0xFF38BDF8)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val SuccessEmerald = Color(0xFF10B981)
    val WarningAmber = Color(0xFFF59E0B)
    val DangerRose = Color(0xFFF43F5E)
}

private val AtikDarkColorScheme = darkColorScheme(
    primary = AtikColors.AccentPurple,
    onPrimary = Color.White,
    secondary = AtikColors.AccentBlue,
    onSecondary = Color.White,
    tertiary = AtikColors.AccentCyan,
    background = AtikColors.ObsidianBg,
    onBackground = AtikColors.TextPrimary,
    surface = AtikColors.SurfaceDark,
    onSurface = AtikColors.TextPrimary,
    surfaceVariant = AtikColors.SurfaceElevated,
    onSurfaceVariant = AtikColors.TextSecondary,
    outline = AtikColors.HairlineBorder,
    error = AtikColors.DangerRose
)

@Composable
fun AtikAiAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AtikDarkColorScheme,
        content = content
    )
}
