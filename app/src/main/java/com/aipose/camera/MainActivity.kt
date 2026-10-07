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
import com.aipose.camera.ui.theme.LightUiTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.aipose.camera.update.ScannerScreen
import com.aipose.camera.assistant.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi

@UnstableApi
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

@UnstableApi
@Composable
private fun AppNav() {
    val nav = rememberNavController()
    var cameraMode by remember {mutableStateOf(CameraMode.LANDSCAPE)}
    val context = LocalContext.current
    val photos = remember { RecentPhotoState(context.applicationContext) }
    val assistant: AssistantViewModel = viewModel()
    var activePlan by remember { mutableStateOf<ShootingPlan?>(null) }
    var activeProject by remember { mutableStateOf<String?>(null) }
    var recordingProject by remember { mutableStateOf<String?>(null) }
    var recordingShot by remember { mutableStateOf<Shot?>(null) }
    var playingFile by remember { mutableStateOf<String?>(null) }

    NavHost(navController = nav, startDestination = "camera") {
        composable("camera") {
            CameraScreen(cameraMode, photos, onModeChanged={cameraMode=it;activePlan=null},onOpenScanner = { nav.navigate("scanner") },
                onOpenAssistant={nav.navigate("assistant") {launchSingleTop=true}},plan=activePlan,
                onPhotoSaved={uri -> activeProject?.let {assistant.photoSaved(it,uri)}},onClearPlan={activePlan=null})
        }
        composable("assistant",
            enterTransition={slideInHorizontally(tween(240),initialOffsetX={-it})+fadeIn(tween(160))},
            exitTransition={fadeOut(tween(160))},
            popExitTransition={slideOutHorizontally(tween(240),targetOffsetX={-it})+fadeOut(tween(160))}) {
            LightUiTheme {
            AssistantScreen(assistant,onBack={nav.popBackStack()},onPhoto={plan ->
                activePlan=plan;activeProject=assistant.state.value.project.id;cameraMode=plan.mode;nav.popBackStack()
            },onRecord={shot ->recordingProject=assistant.state.value.project.id;recordingShot=shot;nav.navigate("record")},onPlay={file ->playingFile=file;nav.navigate("video")})
            }
        }
        composable("record") {
            recordingShot?.let {shot -> VideoCaptureScreen(recordingProject ?: assistant.state.value.project.id,shot,onBack={nav.popBackStack()},onSaved={file ->
                assistant.recorded(recordingProject ?: assistant.state.value.project.id,shot.id,file)
                if(nav.currentDestination?.route=="record") nav.popBackStack()
            })}
        }
        composable("video") {playingFile?.let {VideoPlayerScreen(it){nav.popBackStack()}}}
        composable("scanner") {
            ScannerScreen(onBack = { nav.popBackStack() })
        }
    }
}
