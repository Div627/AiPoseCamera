package com.aipose.camera.assistant

import com.aipose.camera.ui.theme.PrimaryButton as Button
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.aipose.camera.camera.PhotoStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(model: AssistantViewModel,onBack:()->Unit,onPhoto:(ShootingPlan)->Unit,onRecord:(Shot)->Unit,onPlay:(String)->Unit) {
    val state by model.state.collectAsState()
    val project=state.project
    var input by remember {mutableStateOf("")}
    var projectsOpen by remember {mutableStateOf(false)}
    var placesOpen by remember {mutableStateOf(false)}
    var delete by remember {mutableStateOf<ProjectSummary?>(null)}
    var importing by remember {mutableStateOf<String?>(null)}
    var visible by remember(project.id) {mutableIntStateOf(40)}
    val clipboard=LocalClipboardManager.current
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri ->
        val shot=importing;importing=null
        if(uri!=null && shot!=null) model.importClip(shot,uri)
    }
    Scaffold(topBar={TopAppBar(title={Column {Text("拍摄助手",style=MaterialTheme.typography.titleMedium);Text(if(model.onlineAvailable) "AI 规划" else "本地规划",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}},
        navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"返回相机")}},
        actions={IconButton(enabled=!state.busy,onClick={projectsOpen=true}){Icon(Icons.Outlined.Folder,"本地项目")};IconButton(enabled=!state.busy,onClick={model.newProject()}){Icon(Icons.Outlined.Add,"新对话")}})},
        bottomBar={Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal=16.dp,vertical=8.dp)) {
            if(state.exporting) Row(verticalAlignment=Alignment.CenterVertically) {Text("正在本机合成，原素材保留",Modifier.weight(1f));TextButton(onClick=model::cancelExport){Text("取消")}}
            else if(state.busy) Text(if(state.replying) "正在回复…" else "正在处理…",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(bottom=8.dp))
            AssistantComposer(input,{input=it},input.isNotBlank() && !state.busy && state.ready,state.replying,
                onSend={model.send(input);input=""},onStop=model::stopReply)

        }}) {padding ->
        val listState=androidx.compose.foundation.lazy.rememberLazyListState()
        val messages=project.messages.takeLast(visible)
        var previousCount by androidx.compose.runtime.saveable.rememberSaveable(project.id) {mutableIntStateOf(project.messages.size)}
        LaunchedEffect(project.messages.size) {
            val changed=project.messages.size>previousCount
            val follow=changed && (project.messages.lastOrNull()?.role=="user" || !listState.canScrollForward)
            previousCount=project.messages.size
            kotlinx.coroutines.delay(100)
            withFrameNanos {}
            if(follow && listState.layoutInfo.totalItemsCount>0) listState.animateScrollToItem((messages.lastIndex+(if(project.messages.size>visible) 1 else 0)).coerceAtLeast(0))
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding),state=listState,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            if(!state.ready && state.error==null) item {CircularProgressIndicator()}
            if(!state.ready && state.error!=null) item {TextButton(onClick=model::reload){Text("重新读取本地项目")}}
            if(messages.isEmpty() && state.ready) item {
                Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(40.dp))
                    Text("想拍点什么？",style=MaterialTheme.typography.headlineSmall)
                    Text("说说地点和想法，我来安排拍摄。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf("我到冰岛了，想拍自然的风景照","拍一段从孤独到有生命力的个人 Vlog","想拍自然、有环境感的人像").forEach {example ->
                        TextButton(enabled=!state.busy,onClick={model.send(example)},contentPadding=PaddingValues(horizontal=0.dp,vertical=8.dp)){Text(example,style=MaterialTheme.typography.bodyMedium)}
                    }
                    Text(if(model.onlineAvailable) "项目保存在本机" else "本地规划，尚未连接云端 AI",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if(project.messages.size>visible) item {TextButton(onClick={visible+=40}){Text("加载更早的对话")}}
            items(messages,key={it.id},contentType={it.role}) {message ->
                if(message.role=="user") Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.widthIn(max=300.dp)){Text(message.text,Modifier.padding(14.dp))}
                } else Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    MarkdownText(message.text)
                    IconButton(onClick={clipboard.setText(AnnotatedString(message.text))},modifier=Modifier.size(40.dp)){Icon(Icons.Outlined.ContentCopy,"复制回复",modifier=Modifier.size(17.dp))}
                }
            }
            state.error?.let {error -> item {Column {
                Text(error,color=MaterialTheme.colorScheme.error)
                if(state.retryReply && !state.busy && !state.exporting) project.messages.lastOrNull {it.role=="user"}?.let {last -> TextButton(onClick={model.send(last.text,retry=true)}){Text("重试回复")}}
            }}}
            state.notice?.let {item {Text(it,color=MaterialTheme.colorScheme.primary)}}
            project.plan?.let {plan ->
                item(key="plan-${plan.id}") {
                    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text(plan.title,style=MaterialTheme.typography.titleMedium)
                        Text(plan.description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(plan.kind=="photo") {
                            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                listOf(PhotoStyle.SCENIC to "自然",PhotoStyle.ICELAND to "冷调",PhotoStyle.K_GOLD to "暖调",PhotoStyle.A_APX to "黑白").forEach {(style,label) ->
                                    FilterChip(selected=plan.style==style,enabled=!state.busy,onClick={model.selectStyle(style)},label={Text(label)})
                                }
                            }
                            Button(enabled=!state.busy,onClick={onPhoto(plan)},modifier=Modifier.fillMaxWidth()){Text("开始拍摄")}
                        }
                        TextButton(onClick={placesOpen=true},enabled=!state.busy){Text("去哪里拍")}
                    }
                }
                if(plan.kind=="video") {
                    items(plan.shots,key={"shot-${it.id}"},contentType={"shot"}) {shot ->
                        val clip=project.clips.firstOrNull {it.shotId==shot.id}
                        val valid=clip!=null && File(clip.file).exists()
                        Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface) {
                            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                    if(valid) ClipThumbnail(clip!!.file)
                                    Column(Modifier.weight(1f)){Text(shot.title,style=MaterialTheme.typography.titleMedium);Text(if(valid) "已填充 · ${clip!!.durationMs/1000} 秒" else if(clip!=null) "素材缺失，请重新选择" else "待拍摄，也可跳过",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                                }
                                Text(shot.direction,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                    TextButton(enabled=!state.busy,onClick={onRecord(shot)}){Text(if(valid) "重拍" else "拍这一段")}
                                    TextButton(enabled=!state.busy,onClick={importing=shot.id;importer.launch(arrayOf("video/*"))}){Text(if(valid) "换素材" else "从相册选")}
                                    if(valid) TextButton(enabled=!state.busy,onClick={onPlay(clip!!.file)}){Text("预览")}
                                }
                                if(valid) TextButton(enabled=!state.busy,onClick={model.removeClip(shot.id)}){Text("移除此段")}
                            }
                        }
                    }
                    item {
                        val filled=plan.shots.count {shot -> project.clips.any {it.shotId==shot.id && File(it.file).exists()} }
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text("$filled / ${plan.shots.size} 段已准备 · 至少两段可成片",style=MaterialTheme.typography.bodySmall)
                            Button(enabled=filled>=2 && !state.busy,onClick=model::generateVideo,modifier=Modifier.fillMaxWidth()){Text(if(state.exporting) "合成中…" else "生成视频")}
                            Text("按镜头顺序拼接，每段保留最多 5 秒，统一竖屏并保留原声。当前是本地粗剪，不做 AI 画面评分或自动配乐。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(project.exports.asReversed(),key={it},contentType={"export"}) {file ->
                Column {Text("本地成片",style=MaterialTheme.typography.titleMedium);Row {TextButton(onClick={onPlay(file)}){Text("播放")};TextButton(enabled=!state.busy,onClick={model.saveVideo(file)}){Text("保存到相册")}}}
            }
            if(project.photos.isNotEmpty()) item {
                val context=LocalContext.current
                Text("这段对话拍下的照片",style=MaterialTheme.typography.titleMedium)
                project.photos.takeLast(12).asReversed().forEachIndexed {index,uri -> TextButton(onClick={runCatching {context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri),"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}}){Text("查看照片 ${project.photos.size-index}")}}
            }
        }
    }
    if(projectsOpen) ModalBottomSheet(onDismissRequest={projectsOpen=false}) {
        Text("本地项目",Modifier.padding(horizontal=24.dp),style=MaterialTheme.typography.titleLarge)
        LazyColumn(Modifier.heightIn(max=480.dp),contentPadding=PaddingValues(16.dp)) {
            items(state.projects,key={it.id}) {summary -> Row(verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick={model.openProject(summary.id);projectsOpen=false},modifier=Modifier.weight(1f)){Text(summary.title)}
                TextButton(onClick={delete=summary}){Text("删除")}
            }}
        }
    }
    delete?.let {summary -> AlertDialog(onDismissRequest={delete=null},title={Text("删除这个本地项目？")},text={Text("删除对话和项目内的视频副本。相册原素材与已保存到相册的作品保留。")},confirmButton={TextButton(onClick={model.deleteProject(summary.id);delete=null}){Text("删除")}},dismissButton={TextButton(onClick={delete=null}){Text("保留")}})}
    if(placesOpen) PlacesSheet {placesOpen=false}
}

@Composable
private fun ClipThumbnail(path: String) {
    val image by produceState<Bitmap?>(null,path) {
        value=withContext(Dispatchers.IO) {
            val reader=MediaMetadataRetriever()
            try {
                reader.setDataSource(path)
                if(Build.VERSION.SDK_INT>=27) reader.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,128,128)
                else reader.getFrameAtTime(0)?.let {bitmap -> Bitmap.createScaledBitmap(bitmap,128,128,true).also {if(it!==bitmap) bitmap.recycle()} }
            } catch(_:Exception){null} finally {reader.release()}
        }
    }
    image?.let {Image(it.asImageBitmap(),null,Modifier.size(64.dp),contentScale=androidx.compose.ui.layout.ContentScale.Crop)}
}
