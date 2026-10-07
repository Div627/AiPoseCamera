package com.aipose.camera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = BgDark,
    secondary = AccentSoft,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = TextPrimary,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = TextPrimary,
    surfaceTint = Color.Transparent,
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

private val LightScheme = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF242428), onPrimary = Color(0xFFFCFCFD),
    primaryContainer = Color(0xFFECECEF), onPrimaryContainer = Color(0xFF242428),
    secondary = Color(0xFF68686F), onSecondary = Color(0xFFFCFCFD),
    secondaryContainer = Color(0xFFECECEF), onSecondaryContainer = Color(0xFF242428),
    background = Color(0xFFF7F7F9), onBackground = Color(0xFF242428),
    surface = Color(0xFFFCFCFD), onSurface = Color(0xFF242428),
    surfaceVariant = Color(0xFFEDEDF0), onSurfaceVariant = Color(0xFF74747C),
    outline = Color(0xFFC8C8CE), outlineVariant = Color(0xFFE0E0E5),
    surfaceTint = Color.Transparent, error = Color(0xFFB3261E),
)

@Composable
fun LightUiTheme(content: @Composable () -> Unit) {
    val view = androidx.compose.ui.platform.LocalView.current
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(view, activity) {
        val window = activity?.window
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        val statusColor = window?.statusBarColor
        val navigationColor = window?.navigationBarColor
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        window?.statusBarColor = android.graphics.Color.rgb(247,247,249)
        window?.navigationBarColor = android.graphics.Color.rgb(247,247,249)
        onDispose {
            previousStatus?.let { controller?.isAppearanceLightStatusBars = it }
            previousNavigation?.let { controller?.isAppearanceLightNavigationBars = it }
            statusColor?.let { window?.statusBarColor = it }
            navigationColor?.let { window?.navigationBarColor = it }
        }
    }
    MaterialTheme(colorScheme = LightScheme, typography = AppTypography, shapes = MaterialTheme.shapes, content = content)
}
