package com.aipose.camera.update

import com.aipose.camera.diagnostics.Diagnostics
import com.aipose.camera.diagnostics.DiagnosticStore.Event
import com.aipose.camera.diagnostics.DiagnosticStore.Field
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.MoreVert

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.aipose.camera.ui.theme.CameraDesign
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.aipose.camera.ui.theme.ScanIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun ScannerScreen(onResult: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope=rememberCoroutineScope()
    var menu by remember {mutableStateOf(false)}
    LaunchedEffect(Unit) {Diagnostics.event(Event.SCANNER_OPEN)}
    val owner = LocalLifecycleOwner.current
    val currentResult by rememberUpdatedState(onResult)
    val main = remember { ContextCompat.getMainExecutor(context) }
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    LaunchedEffect(Unit) { if (!permission) request.launch(Manifest.permission.CAMERA) }
    var status by remember { mutableStateOf("") }
    var cameraFailed by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(false) }
    val allowScan by rememberUpdatedState(!manual)
    var text by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf(false) }
    val view = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val session = remember(retry, permission) { ScanSession() }

    DisposableEffect(permission, retry, owner) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
        val future = ProcessCameraProvider.getInstance(context)
        val preview = Preview.Builder().build()
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
                ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build())
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        var provider: ProcessCameraProvider? = null
        var lastLog=0L
        if (permission) future.addListener({
            if (session.isActive()) view.doOnLayout {
                if (!session.isActive()) return@doOnLayout
                try {
                    provider = future.get()
                    preview.targetRotation = view.display.rotation
                    analysis.targetRotation = view.display.rotation
                    preview.setSurfaceProvider(view.surfaceProvider)
                    analysis.setAnalyzer(executor) { frame ->
                        if (!session.begin()) { frame.close(); return@setAnalyzer }
                        val image = frame.image
                        if (image == null) { frame.close(); session.finish(); return@setAnalyzer }
                        try {
                            // Keep ImageProxy alive until ML Kit completes; rotation comes from this exact frame.
                            scanner.process(InputImage.fromMediaImage(image, frame.imageInfo.rotationDegrees))
                                .addOnSuccessListener(main) { codes ->
                                    val now=android.os.SystemClock.elapsedRealtime()
                                    if(now-lastLog>2000) {lastLog=now;Diagnostics.event(Event.SCANNER_FRAME,Field.WIDTH to frame.width,Field.HEIGHT to frame.height,Field.COUNT to codes.size)}
                                    if (session.isActive() && allowScan) {
                                        val valid = codes.firstNotNullOfOrNull { UpdateLink.parse(it.rawValue) }
                                        val url = session.deliver(valid, owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                                        if (url != null) {Diagnostics.event(Event.SCAN_RESULT,Field.ACCEPTED to 1);currentResult(url)}
                                        else if (codes.isNotEmpty() && valid == null) status = "请扫描映刻相机的更新二维码"
                                    }
                                }
                                .addOnFailureListener(main) {error->
                                    val now=android.os.SystemClock.elapsedRealtime()
                                    if(now-lastLog>2000) {lastLog=now;Diagnostics.error(Event.SCANNER_ERROR,error)}
                                    if (session.isActive()) status = "暂未识别，请靠近一点"
                                }
                                .addOnCompleteListener(main) { frame.close(); session.finish() }
                        } catch (_: Exception) {
                            frame.close(); session.finish()
                            main.execute { if (session.isActive()) {cameraFailed=true;status = "相机暂不可用"} }
                        }
                    }
                    provider!!.unbindAll()
                    camera = provider!!.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    cameraFailed=false;status = ""
                    view.setOnTouchListener { _, event ->
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            val point = view.meteringPointFactory.createPoint(event.x, event.y)
                            camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point)
                                .setAutoCancelDuration(3, TimeUnit.SECONDS).build())
                            view.performClick()
                        }
                        true
                    }
                } catch (error: Exception) { Diagnostics.error(Event.SCANNER_ERROR,error);cameraFailed=true;status = "相机暂不可用" }
            }
        }, main)
        onDispose {
            session.close()
            analysis.clearAnalyzer()
            // Only release this screen's use cases: outgoing navigation must not unbind the next screen.
            provider?.unbind(preview, analysis)
            view.setOnTouchListener(null)
            scanner.close()
            executor.shutdown()
            camera = null; torch = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if(permission) AndroidView(factory={view},modifier=Modifier.fillMaxSize())
        if(permission && !cameraFailed) Canvas(Modifier.fillMaxSize()) {
            val side=minOf(size.width*.7f,size.height*.36f)
            val left=(size.width-side)/2;val top=size.height*.43f-side/2
            val right=left+side;val bottom=top+side
            val shade=Path().apply {
                fillType=PathFillType.EvenOdd
                addRect(Rect(0f,0f,size.width,size.height))
                addRoundRect(RoundRect(Rect(left,top,right,bottom),CornerRadius(20.dp.toPx())))
            }
            drawPath(shade,Color.Black.copy(alpha=.48f))
            val length=24.dp.toPx();val stroke=2.5.dp.toPx()
            fun line(x:Float,y:Float,a:Float,b:Float)=drawLine(Color.White,Offset(x,y),Offset(a,b),stroke,StrokeCap.Round)
            line(left,top+length,left,top);line(left,top,left+length,top)
            line(right-length,top,right,top);line(right,top,right,top+length)
            line(left,bottom-length,left,bottom);line(left,bottom,left+length,bottom)
            line(right-length,bottom,right,bottom);line(right,bottom,right,bottom-length)
        }
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal=20.dp)) {
            Box(Modifier.fillMaxWidth().height(56.dp),contentAlignment=Alignment.Center) {
                Box(Modifier.align(Alignment.CenterEnd)) {
                    IconButton(onClick={menu=true}) {Icon(Icons.Default.MoreVert,"更多",tint=Color.White)}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                        DropdownMenuItem(text={Text("导出诊断日志")},onClick={menu=false;scope.launch {Diagnostics.export(context)}})
                    }
                }
                IconButton(onClick=onBack,modifier=Modifier.align(Alignment.CenterStart)) {Icon(Icons.Default.Close,"返回相机",tint=Color.White)}
                Text("扫一扫",color=Color.White,style=MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().padding(bottom=24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                when {
                    !permission -> {
                        ScanIcon(Modifier.size(36.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("开启相机，即可扫码更新",color=Color.White)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick={request.launch(Manifest.permission.CAMERA)},shape=CircleShape) {Text("开启相机")}
                    }
                    cameraFailed -> {
                        Text(status,color=Color.White)
                        TextButton(onClick={cameraFailed=false;status="";retry++}) {Text("重新打开相机",color=Color.White)}
                    }
                    else -> {
                        Text(status.ifBlank {"对准更新二维码"},color=Color.White,style=MaterialTheme.typography.bodyMedium,textAlign=TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text("识别后自动下载更新",color=Color.White.copy(alpha=.55f),style=MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(32.dp))
                if(permission && !cameraFailed) {
                    IconButton(enabled=camera?.cameraInfo?.hasFlashUnit()==true,onClick={
                        val next=!torch
                        camera?.cameraControl?.enableTorch(next)
                        torch=next
                    },modifier=Modifier.size(52.dp).background(if(torch) Color.White else Color.White.copy(alpha=.12f),CircleShape)) {
                        Icon(if(torch) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,if(torch) "关闭补光" else "打开补光",tint=if(torch) Color.Black else Color.White)
                    }
                    Spacer(Modifier.height(20.dp))
                }
                TextButton(onClick={manual=true}) {Text("使用更新链接",color=Color.White.copy(alpha=.7f),style=MaterialTheme.typography.labelMedium)}
            }
        }
    }
    if(manual) ModalBottomSheet(onDismissRequest={manual=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).navigationBarsPadding().imePadding(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("使用更新链接",style=MaterialTheme.typography.titleLarge)
            OutlinedTextField(value=text,onValueChange={text=it;manualError=false},isError=manualError,
                modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("粘贴下载链接")},
                supportingText=if(manualError) {{Text("请输入有效的 HTTPS 安装包链接")}} else null)
            Button(onClick={
                val url=session.deliver(text,owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                if(url==null) manualError=true else {manual=false;currentResult(url)}
            },modifier=Modifier.fillMaxWidth().height(50.dp),shape=CircleShape) {Text("下载更新")}
            Spacer(Modifier.height(12.dp))
        }
    }
}
