package com.aipose.camera.camera

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Only the narrow left edge opens chat, leaving pinch zoom and subject taps available. */
@Composable
fun AssistantEdgeGesture(modifier:Modifier,onOpen:()->Unit) {
    val open by rememberUpdatedState(onOpen)
    Box(modifier.pointerInput(Unit) {
        var distance=0f
        detectHorizontalDragGestures(
            onDragStart={distance=0f},
            onHorizontalDrag={change,amount ->change.consume();distance+=amount},
            onDragCancel={distance=0f},
            onDragEnd={if(distance>72.dp.toPx()) open();distance=0f},
        )
    })
}
