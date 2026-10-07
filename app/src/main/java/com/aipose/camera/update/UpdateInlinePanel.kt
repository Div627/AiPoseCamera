package com.aipose.camera.update

import com.aipose.camera.ui.theme.PrimaryButton as Button
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aipose.camera.ui.theme.*
import java.util.Locale

private fun sizeLabel(bytes: Long) = String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576f)

@Composable
fun UpdateInlinePanel(state: UpdateState, onCancel: () -> Unit, onRetry: () -> Unit, onInstall: () -> Unit, onScan: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(24.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (state.problem == UpdateProblem.CURRENT_VERSION) "无需更新" else when (state.phase) {
            UpdatePhase.CONNECTING -> "正在连接"
            UpdatePhase.DOWNLOADING -> "正在下载更新"
            UpdatePhase.VERIFYING -> "正在校验"
            UpdatePhase.READY -> "更新已就绪"
            UpdatePhase.PERMISSION -> "允许安装更新"
            UpdatePhase.INSTALLING -> "等待安装确认"
            UpdatePhase.CANCELLED -> "下载已取消"
            else -> "暂时无法更新"
        }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (state.busy) {
            val percent = state.progress.percent
            if (state.phase == UpdatePhase.DOWNLOADING && percent != null)
                LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth(), color = Accent, trackColor = SurfaceElevated)
            else LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent, trackColor = SurfaceElevated)
            if (state.phase == UpdatePhase.DOWNLOADING) Text(
                sizeLabel(state.progress.bytes) + if (state.progress.total > 0) " / ${sizeLabel(state.progress.total)} · $percent%" else " · 已下载",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (state.version.isNotBlank()) Text("映刻相机 ${state.version}", style = MaterialTheme.typography.bodyMedium)
        Text(state.message, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        if (state.busy) TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("取消下载") }
        else {
            val needsNewLink = state.problem in setOf(UpdateProblem.NOT_FOUND, UpdateProblem.NOT_APK,
                UpdateProblem.WRONG_APP, UpdateProblem.SIGNATURE, UpdateProblem.CURRENT_VERSION)
            Button(onClick = if (state.file != null) onInstall else if (needsNewLink) onScan else onRetry,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) {
                Text(if (state.file != null) if (state.phase == UpdatePhase.READY) "安装更新" else "继续安装" else if (needsNewLink) "重新扫码" else "重新下载")
            }
            if (!needsNewLink) TextButton(onClick = onScan, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("重新扫码") }
        }
    }
}
