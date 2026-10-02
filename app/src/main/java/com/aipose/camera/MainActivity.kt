package com.aipose.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aipose.camera.camera.CameraScreen
import com.aipose.camera.camera.CameraMode
import com.aipose.camera.camera.RecentPhotoState
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val photos = remember { RecentPhotoState(context.applicationContext) }

    NavHost(navController = nav, startDestination = "camera") {
        composable("camera") {
            CameraScreen(cameraMode, photos, onModeChanged={cameraMode=it},onOpenScanner = { nav.navigate("scanner") })
        }
        composable("scanner") {
            ScannerScreen(onBack = { nav.popBackStack() })
        }
    }
}
