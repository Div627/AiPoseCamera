package com.aipose.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aipose.camera.camera.CameraScreen
import com.aipose.camera.camera.CameraMode
import com.aipose.camera.update.UpdateScreen
import com.aipose.camera.ui.theme.AiPoseTheme
import com.aipose.camera.update.ScannerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.aipose.camera.diagnostics.Diagnostics.install(applicationContext)
        setContent {
            AiPoseTheme {
                AppNav()
            }
        }
    }
}

@Composable
private fun AppNav() {
    val nav = rememberNavController()
    var cameraMode by remember {mutableStateOf(CameraMode.LANDSCAPE)}
    // 扫码结果进入独立更新页，不再经过设置页。
    var pendingScanUrl by rememberSaveable { mutableStateOf<String?>(null) }

    NavHost(navController = nav, startDestination = "camera") {
        composable("camera") {
            CameraScreen(cameraMode,onModeChanged={cameraMode=it},onOpenScanner = { nav.navigate("scanner") })
        }
        composable("update") {
            UpdateScreen(
                onBack = { nav.popBackStack() },
                pendingScanUrl = pendingScanUrl,
                onScanConsumed = { pendingScanUrl = null },
            )
        }
        composable("scanner") {
            ScannerScreen(
                onResult = { url ->
                    pendingScanUrl = url
                    nav.navigate("update") { popUpTo("camera") }
                },
                onBack = { nav.popBackStack() },
            )
        }
    }
}
