package com.aipose.camera.assistant

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.aipose.camera.camera.PhotoStyle
import com.aipose.camera.ui.theme.ActionButton
import com.aipose.camera.ui.theme.ActionPhase
import com.aipose.camera.ui.theme.PrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun VideoStudyScreen(model: VideoStudyViewModel, onBack: () -> Unit, onPhotoSaved: (String) -> Unit,
    onVlog: (VideoStudy, Boolean) -> Unit) {
    val state by model.state.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri -> uri?.let(model::import)}
    var selected by remember(state.study?.source) {mutableIntStateOf(0)}
    var chronological by remember {mutableStateOf(true)}
    var forVideo by remember {mutableStateOf(false)}
    var showSummary by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回对话")}
            Text("从视频选片", modifier=Modifier.weight(1f),style = MaterialTheme.typography.titleMedium)
            if(state.study!=null) TextButton(onClick={picker.launch(arrayOf("video/*"))},enabled=!state.analyzing && !state.saving){Text("换视频")}
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if(state.loading) CircularProgressIndicator()
            else if(state.study==null) PrimaryButton(onClick = {picker.launch(arrayOf("video/*"))}, enabled = !state.analyzing && !state.saving,
                modifier = Modifier.fillMaxWidth()) {Text(if(state.study == null) "选择一段视频" else "换一段视频")}
            if(state.analyzing) {
                Text("正在本机挑选画面 · ${(state.progress * 100).toInt()}%")
                LinearProgressIndicator(progress = {state.progress}, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = model::cancel) {Text("取消分析")}
            }
            state.error?.let {Text(it, color = MaterialTheme.colorScheme.error)}
            state.study?.let {study ->
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("${study.durationMs/1000} 秒 · ${study.candidates.size} 张候选画面",modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                    TextButton(onClick={showSummary=!showSummary}) {Text(if(showSummary) "收起摘要" else "视频摘要")}
                }
                if(showSummary) MarkdownText(VideoSummary.describe(study))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected=!forVideo,onClick={forVideo=false},label={Text("选照片")})
                    FilterChip(selected=forVideo,onClick={forVideo=true},label={Text("做 Vlog")})
                }
                if(!forVideo) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(study.candidates, key = {it.timeMs}) {frame ->
                        Surface(shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(
                            if(study.candidates.getOrNull(selected)?.timeMs == frame.timeMs) 2.dp else 1.dp,
                            if(study.candidates.getOrNull(selected)?.timeMs == frame.timeMs) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.clickable(enabled = !state.saving && !state.analyzing) {selected = study.candidates.indexOf(frame)}) {
                            Column(Modifier.padding(4.dp)) {
                                CandidateImage(frame.preview)
                                Text("${VideoSummary.time(frame.timeMs)} 秒", Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                study.candidates.getOrNull(selected)?.let {frame ->
                    var style by remember(study.source, frame.timeMs) {mutableStateOf(if(frame.faces > 0) PhotoStyle.PORTRAIT else PhotoStyle.SCENIC)}
                    var strength by remember(study.source, frame.timeMs) {mutableFloatStateOf(.6f)}
                    var ratio by remember(study.source, frame.timeMs) {mutableStateOf<Float?>(null)}
                    var x by remember(study.source, frame.timeMs) {mutableFloatStateOf(frame.centerX)}
                    var y by remember(study.source, frame.timeMs) {mutableFloatStateOf(frame.centerY)}
                    val edit = FrameEdit(style, strength, ratio, x, y)
                    LaunchedEffect(study.source,frame.timeMs,edit) {model.clearSaved()}
                    val preview by produceState<Bitmap?>(null, study.source, frame.timeMs, edit) {
                        delay(180)
                        value = runCatching {FramePhotoEditor.render(study, frame, edit, minOf(960, maxOf(study.width, study.height)))}.getOrNull()
                    }

                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                            preview?.let {Image(it.asImageBitmap(), "调色与裁剪预览", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)}
                                ?: CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                    Text(if(frame.faces > 0) "检出 ${frame.faces} 张人脸${if(frame.eyesOpen != null && frame.eyesOpen < .35f) "，可能闭眼，请确认" else ""}" else "参考画面细节、曝光和重复程度推荐", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(PhotoStyle.ORIGINAL to "原色", (if(frame.faces > 0) PhotoStyle.PORTRAIT else PhotoStyle.SCENIC) to "自然", PhotoStyle.ICELAND to "冷调", PhotoStyle.K_GOLD to "暖调", PhotoStyle.A_APX to "黑白").forEach {(value, text) ->
                            FilterChip(selected = style == value, enabled = !state.saving, onClick = {style = value}, label = {Text(text)})
                        }
                    }
                    if(style != PhotoStyle.ORIGINAL) {
                        Text("调色强度", style = MaterialTheme.typography.labelMedium)
                        Slider(value = strength, onValueChange = {strength = it}, enabled = !state.saving)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(null to "原比例", 1f to "1:1", 4f / 3 to "4:3", 9f / 16 to "9:16").forEach {(value, label) ->
                            FilterChip(selected = ratio == value, onClick = {ratio = value}, enabled = !state.saving, label = {Text(label)})
                        }
                    }
                    if(ratio != null) {
                        Text("调整裁剪位置", style = MaterialTheme.typography.labelMedium)
                        if(ratio!! < study.width.toFloat()/study.height) Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("左右",Modifier.width(48.dp),style=MaterialTheme.typography.bodySmall)
                            Slider(value = x, onValueChange = {x = it}, enabled = !state.saving,modifier=Modifier.weight(1f))
                        } else if(ratio!! > study.width.toFloat()/study.height) Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("上下",Modifier.width(48.dp),style=MaterialTheme.typography.bodySmall)
                            Slider(value = y, onValueChange = {y = it}, enabled = !state.saving,modifier=Modifier.weight(1f))
                        }
                    }
                    ActionButton("保存照片", "正在保存…", "已保存 · 再保存",
                        when {state.saving -> ActionPhase.RUNNING; state.saved != null -> ActionPhase.SUCCEEDED;state.error != null -> ActionPhase.FAILED; else -> ActionPhase.READY},
                        {model.save(frame, edit, onPhotoSaved)}, Modifier.fillMaxWidth(), !state.analyzing)
                    state.saved?.let {Text(it, style = MaterialTheme.typography.bodySmall)}
                }
                } else {
                Text("把片刻连成 Vlog", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = chronological, onClick = {chronological = true}, label = {Text("时间顺序")})
                    FilterChip(selected = !chronological, onClick = {chronological = false}, label = {Text("由静到动")})
                }
                Text("选出时间分散的片段，每段最多四秒，保留原声。“由静到动”参考人像与亮度排序，可在镜头卡中调整素材。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val segments=remember(study) {FrameSelection.segments(study.frames,study.durationMs)}
                segments.forEachIndexed {index, frame ->
                    val range=FrameSelection.range(frame,study.frames,study.durationMs);val start=range.first
                    Text("${index+1} · ${VideoSummary.time(start)}–${VideoSummary.time(range.last+1)} 秒 · ${frame.labels.firstOrNull()?.let(VideoSummary::label) ?: "片刻"}",style=MaterialTheme.typography.bodyMedium)
                }
                if(segments.size<2) Text("素材太短，暂时不能选出两段独立片段。",style=MaterialTheme.typography.bodySmall)
                PrimaryButton(onClick = {onVlog(study, chronological)}, modifier = Modifier.fillMaxWidth(), enabled = segments.size>=2 && !state.analyzing && !state.saving) {Text("生成 Vlog 镜头卡")}
                }
            }
            if(state.study == null) Text("视频保存在本机。选择候选画面后，可调色、裁剪并保存照片，也能选片生成 Vlog。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CandidateImage(path: String) {
    val bitmap by produceState<Bitmap?>(null, path) {value = withContext(Dispatchers.IO) {
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {inSampleSize = 2})
    }}

    Box(Modifier.size(112.dp, 84.dp), contentAlignment = Alignment.Center) {
        bitmap?.let {Image(it.asImageBitmap(), "候选画面", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)}
    }
}
