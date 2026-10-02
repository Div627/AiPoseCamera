package com.aipose.camera.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

@Composable
fun CameraScreen(mode: CameraMode, photos: RecentPhotoState, onModeChanged: (CameraMode) -> Unit, onOpenScanner: () -> Unit) {
    key(mode) { AiCameraScreen(mode, photos, onOpenScanner, onMode = onModeChanged) }
}
