package com.aipose.camera.assistant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VideoCaptureScreen(projectId:String,shot:Shot,onBack:()->Unit,onSaved:(File)->Unit) {
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current
    val main=remember {ContextCompat.getMainExecutor(context)}
    val providerFuture=remember {ProcessCameraProvider.getInstance(context)}
    var provider by remember {mutableStateOf<ProcessCameraProvider?>(null)}
    var capture by remember {mutableStateOf<VideoCapture<Recorder>?>(null)}
    var recording by remember {mutableStateOf<Recording?>(null)}
    var finishing by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    var seconds by remember {mutableIntStateOf(0)}
    var permission by remember {mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
    var preview by remember {mutableStateOf<PreviewView?>(null)}
    var disposed by remember {mutableStateOf(false)}
    fun start(withAudio:Boolean) {
        val source=capture ?: return
        if(recording!=null || finishing) return
        val file=File(ProjectMedia.directory(context,projectId),"${newId()}.mp4")
        error=null;seconds=0
        try {
            var pending=source.output.prepareRecording(context,FileOutputOptions.Builder(file).build())
            if(withAudio && ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) pending=pending.withAudioEnabled()
            recording=pending.start(main) {event ->
                if(event is VideoRecordEvent.Finalize) {
                    recording=null;finishing=false
                    if(!event.hasError() && file.length()>0) onSaved(file)
                    else {file.delete();error="录制未完成，请重试；也可以从相册导入。"}
                }
            }
        } catch(_:Exception) {file.delete();error="无法开始录制，请检查权限和存储空间。"}
    }
    val cameraPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {permission=it}
    val audioPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {start(it)}
    LaunchedEffect(Unit) {if(!permission) cameraPermission.launch(Manifest.permission.CAMERA)}
    LaunchedEffect(permission,preview) {
        val view=preview ?: return@LaunchedEffect
        if(!permission) return@LaunchedEffect
        providerFuture.addListener({
            if(disposed) return@addListener
            try {
                val cameraProvider=providerFuture.get();provider=cameraProvider
                val recorder=Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD,FallbackStrategy.lowerQualityOrHigherThan(Quality.HD))).build()
                val video=VideoCapture.withOutput(recorder)
                val cameraPreview=Preview.Builder().build().also {it.setSurfaceProvider(view.surfaceProvider)}
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycle,CameraSelector.DEFAULT_BACK_CAMERA,cameraPreview,video)
                capture=video
            } catch(_:Exception) {error="视频相机暂时不可用，可返回后导入素材。"}
        },main)
    }
    LaunchedEffect(recording) {
        if(recording!=null) while(seconds<10) {delay(1000);seconds++;if(seconds==10){finishing=true;recording?.stop()}}
    }
    DisposableEffect(Unit) {onDispose {disposed=true;recording?.stop();provider?.unbindAll()}}
    BackHandler(recording!=null || finishing) {finishing=true;recording?.stop()}
    Column(Modifier.fillMaxSize().background(Color(0xff101114)).systemBarsPadding()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            TextButton(enabled=recording==null && !finishing,onClick=onBack){Text("返回分镜")}
            Text(shot.title,style=MaterialTheme.typography.titleMedium)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(factory={PreviewView(it).apply {implementationMode=PreviewView.ImplementationMode.COMPATIBLE;scaleType=PreviewView.ScaleType.FILL_CENTER;preview=this}},modifier=Modifier.fillMaxSize())
            Text(shot.direction,Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xaa101114)).padding(20.dp),color=Color(0xfff5f5f7))
        }
        error?.let {Text(it,Modifier.padding(16.dp),color=MaterialTheme.colorScheme.error)}
        Text(if(finishing) "正在保存…" else if(recording!=null) "$seconds 秒 / 10 秒" else "录 6–10 秒，合成使用前 5 秒",Modifier.align(Alignment.CenterHorizontally).padding(12.dp))
        Button(enabled=capture!=null && !finishing,onClick={
            if(recording!=null) {finishing=true;recording?.stop()}
            else if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) start(true)
            else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        },modifier=Modifier.align(Alignment.CenterHorizontally).padding(bottom=20.dp).height(56.dp)) {Text(if(recording!=null) "结束这一段" else "录制这一段")}
    }
}
