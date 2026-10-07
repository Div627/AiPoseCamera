package com.aipose.camera.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

@Composable
fun CameraScreen(mode: CameraMode, photos: RecentPhotoState, onModeChanged: (CameraMode) -> Unit, onOpenScanner: () -> Unit, onOpenAssistant: () -> Unit = {}, plan: com.aipose.camera.assistant.ShootingPlan? = null, onPhotoSaved: (String) -> Unit = {}, onClearPlan: () -> Unit = {}) {
    key(mode) { AiCameraScreen(mode, photos, onOpenScanner, onMode = onModeChanged, onOpenAssistant = onOpenAssistant, plan = plan, onPhotoSaved = onPhotoSaved, onClearPlan = onClearPlan) }
}
