package com.aipose.camera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    secondary = AccentSoft,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = Color(0xFFFF8A80),
)

@Composable
fun AiPoseTheme(content: @Composable () -> Unit) {
    // 相机类应用固定深色，观感更专业
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = AppTypography,
        shapes = androidx.compose.material3.Shapes(
            small=androidx.compose.foundation.shape.RoundedCornerShape(CameraDesign.OptionRadius),
            medium=androidx.compose.foundation.shape.RoundedCornerShape(CameraDesign.CardRadius),
            large=androidx.compose.foundation.shape.RoundedCornerShape(CameraDesign.SheetRadius)),
        content = content,
    )
}
