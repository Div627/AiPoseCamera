package com.aipose.camera.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aipose.camera.BuildConfig
import com.aipose.camera.ui.theme.CameraDesign
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** A dedicated update flow without model settings or credential editing. */
@Composable
fun UpdateScreen(onBack: () -> Unit, pendingScanUrl: String?, onScanConsumed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf(pendingScanUrl) }
    var downloadedPath by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf("准备下载更新…") }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }

    fun install(apk: File) {
        message = try {
            if (Updater.installApk(context, apk)) "已打开安装器，请确认安装完成更新"
            else "请允许安装未知应用，返回后点击“继续安装”"
        } catch (_: Exception) {
            "无法打开安装器，请重试"
        }
    }
    fun download() {
        if (downloading) return
        val link = UpdateLink.parse(url)
        if (link == null) { message = "需要有效的 HTTPS APK 下载链接"; return }
        downloading = true
        progress = 0
        message = "正在下载更新…"
        scope.launch {
            try {
                val apk = Updater.downloadApk(context, link) { value ->
                    scope.launch { progress = value }
                }
                downloadedPath = apk.absolutePath
                install(apk)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                message = "更新失败，请检查网络和下载链接后重试"
            } finally {
                downloading = false
            }
        }
    }
    LaunchedEffect(Unit) {
        onScanConsumed()
        if (downloadedPath == null) download()
    }
    BackHandler(enabled = downloading) { message = "正在下载更新，请稍候…" }
    Column(Modifier.fillMaxSize().background(Color(0xFF101112)).systemBarsPadding().padding(horizontal=24.dp)) {
        Box(Modifier.fillMaxWidth().height(56.dp),contentAlignment=Alignment.Center) {
            IconButton(enabled=!downloading,onClick=onBack,modifier=Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.Close,"返回相机",tint=Color.White.copy(alpha=if(downloading) .3f else 1f))
            }
            Text("更新",style=MaterialTheme.typography.titleMedium,color=Color.White)
        }
        Column(Modifier.weight(1f).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Box(Modifier.size(88.dp),contentAlignment=Alignment.Center) {
                if(downloading) {
                    if(progress>0) CircularProgressIndicator(progress={progress/100f},modifier=Modifier.fillMaxSize(),color=Color.White,strokeWidth=2.dp,trackColor=Color.White.copy(alpha=.12f))
                    else CircularProgressIndicator(modifier=Modifier.fillMaxSize(),color=Color.White,strokeWidth=2.dp)
                    Text(if(progress>0) "$progress%" else "…",style=MaterialTheme.typography.titleLarge,color=Color.White)
                } else {
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha=.06f),CircleShape),contentAlignment=Alignment.Center) {
                        Icon(if(downloadedPath!=null) Icons.Default.Check else Icons.Default.Refresh,null,modifier=Modifier.size(32.dp),tint=Color.White)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(when {downloading->"正在下载更新";downloadedPath!=null->"准备安装";message=="准备下载更新…"->"准备更新";else->"暂时无法更新"},
                style=MaterialTheme.typography.headlineSmall,color=Color.White)
            Spacer(Modifier.height(12.dp))
            Text(if(downloading) "请保持网络连接" else message,color=Color.White.copy(alpha=.55f),
                style=MaterialTheme.typography.bodyMedium,textAlign=TextAlign.Center,modifier=Modifier.widthIn(max=280.dp))
            if(!downloading && message!="准备下载更新…") {
                Spacer(Modifier.height(28.dp))
                Button(onClick={
                    val apk=downloadedPath?.let(::File)?.takeIf {it.isFile}
                    if(apk!=null) install(apk) else {downloadedPath=null;download()}
                },modifier=Modifier.fillMaxWidth().height(50.dp),shape=CircleShape,
                    colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color.Black)) {
                    Text(if(downloadedPath!=null) "继续安装" else "重试")
                }
            }
        }
        Text("映刻相机 · ${BuildConfig.VERSION_NAME}",color=Color.White.copy(alpha=.35f),style=MaterialTheme.typography.labelSmall,
            textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(bottom=24.dp))
    }
}
