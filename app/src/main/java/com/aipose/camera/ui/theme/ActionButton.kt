package com.aipose.camera.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class ActionPhase { READY, RUNNING, SUCCEEDED, FAILED }

/** Fixed footprint, real operation state. No timer manufactures success. */
@Composable
fun ActionButton(label: String, runningLabel: String, successLabel: String, phase: ActionPhase,
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    PrimaryButton(onClick, modifier, enabled = enabled && phase != ActionPhase.RUNNING) {
        Crossfade(phase, animationSpec = tween(160), label = "action state") {value ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when(value) {
                    ActionPhase.RUNNING -> {CircularProgressIndicator(Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp); Text(runningLabel)}
                    ActionPhase.SUCCEEDED -> {Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)); Text(successLabel)}
                    ActionPhase.FAILED -> Text("重试$label")
                    ActionPhase.READY -> Text(label)
                }
            }
        }
    }
}

/** Release/cancel interrupts progress. Accessibility has an explicit confirm action. */
@Composable
fun HoldConfirmButton(onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    val progress = remember {Animatable(0f)}
    val latest by rememberUpdatedState(onConfirm)
    val scope = rememberCoroutineScope()
    var completed by remember {mutableStateOf(false)}
    Box(modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(BgDark)
        .semantics {
            role = Role.Button
            contentDescription = "长按删除，松开取消"
            onClick(label = "确认删除") {if(!completed) {completed = true; latest()}; true}
        }
        .pointerInput(Unit) {
            detectTapGestures(onPress = {
                if(completed) return@detectTapGestures
                val job = scope.launch {
                    progress.snapTo(0f)
                    progress.animateTo(1f, tween(900))
                    completed = true
                    latest()
                }
                try {tryAwaitRelease()} finally {job.cancel(); if(!completed) scope.launch {progress.snapTo(0f)}}
            })
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize().background(TextPrimary.copy(alpha = .15f)))
        Canvas(Modifier.matchParentSize()) {drawRect(Success.copy(alpha = .25f),size = androidx.compose.ui.geometry.Size(size.width * progress.value, size.height))}
        Text("长按删除", Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = TextPrimary)
    }
}
