package com.aipose.camera.camera

import com.aipose.camera.diagnostics.Diagnostics
import com.aipose.camera.diagnostics.DiagnosticStore.Event
import com.aipose.camera.diagnostics.DiagnosticStore.Field
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import android.util.Size
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import android.os.Build
import android.os.SystemClock
import android.util.Rational
import androidx.camera.core.Camera
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.core.view.doOnLayout
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.LinearProgressIndicator
import com.aipose.camera.pose.AutoFraming
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.CheckCircle
import com.aipose.camera.ui.theme.BgDark
import com.aipose.camera.ui.theme.TextPrimary
import com.aipose.camera.ui.theme.SurfaceElevated
import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import com.aipose.camera.ui.theme.ScanIcon
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.aipose.camera.ui.theme.CameraSheet
import com.aipose.camera.ui.theme.CameraDesign
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aipose.camera.llm.LlmClient
import com.aipose.camera.pose.Landmarks
import com.aipose.camera.pose.PoseAligner
import com.aipose.camera.pose.HumanPoseGuide
import com.aipose.camera.pose.GroupPoses
import com.aipose.camera.llm.PhotographyGuide
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import com.aipose.camera.pose.PoseSilhouetteIcon
import com.aipose.camera.pose.PoseTemplate
import com.aipose.camera.pose.PoseCategory
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.semantics.selected
import com.aipose.camera.settings.SettingsRepository
import com.aipose.camera.ui.theme.Accent
import com.aipose.camera.ui.theme.Success
import com.aipose.camera.ui.theme.TextSecondary
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 相机主界面：
 * - CameraX 共用视口 + MediaPipe 实时姿态检测（端上推理）
 * - 明确人像/风景意图；人体细轮廓与圈选调色分别提供
 * - 对齐达标自动拍照；DeepSeek/千问 提供异步进阶点评
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AiCameraScreen(mode:CameraMode,photos: RecentPhotoState,onOpenScanner: () -> Unit, onMode: (CameraMode) -> Unit, onOpenAssistant: () -> Unit = {}, plan: com.aipose.camera.assistant.ShootingPlan? = null, onPhotoSaved: (String) -> Unit = {}, onClearPlan: () -> Unit = {}) {
    val context = LocalContext.current
    var selectedPhoto by remember { mutableStateOf<RecentPhoto?>(null) }
    var reviewingSelection by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val requiredPermissions = remember { if (Build.VERSION.SDK_INT < 29)
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        else arrayOf(Manifest.permission.CAMERA) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasPermission = requiredPermissions.all { permission -> ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED } }

    LaunchedEffect(Unit) {
        hasPermission = requiredPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!hasPermission) permissionLauncher.launch(requiredPermissions)
    }

    val settings = remember { SettingsRepository(context) }
    val timer=rememberCaptureTimer()
    var timerSeconds by remember {mutableIntStateOf(0)}
    var gridEnabled by remember {mutableStateOf(false)}
    var levelEnabled by remember {mutableStateOf(true)}
    var guidanceEnabled by remember {mutableStateOf(true)}
    var referenceEnabled by remember {mutableStateOf(true)}
    var maintenanceOpen by remember {mutableStateOf(false)}
    var advancedCapture by remember {mutableStateOf(false)}
    val level=rememberLevel(levelEnabled)
    var intent by remember {mutableStateOf(TravelIntent.AUTO)}
    var referenceStyle by remember {mutableStateOf(ReferenceStyle.ALL)}
    var grade by remember {mutableStateOf(ColorGrade())}
    var autoExposure by remember {mutableStateOf(true)}
    var selectingSubject by remember {mutableStateOf(false)} // True while awaiting a tap segmentation.
    var subjectEdges by remember {mutableStateOf<List<TapSubjectMask.Edge>>(emptyList())}
    val selectionGeneration=remember {AtomicInteger(0)}
    val pendingTap=remember {AtomicReference<TapSelectionRequest?>(null)}
    val segmentationExecutor=remember {Executors.newSingleThreadExecutor()}
    val segmenter=remember {TapSubjectSegmenter(context.applicationContext)}
    var subjectBox by remember {mutableStateOf<SubjectFraming.Box?>(null)}
    val subjectRegion=remember {AtomicReference<SubjectColor.Region?>(null)}
    var subjectSession by remember {mutableStateOf(SubjectColor.Session())}
    var panel by remember {mutableStateOf(CameraPanel.NONE)}
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedPhoto = RecentPhoto(0, uri)
            reviewingSelection = true
            panel = CameraPanel.REVIEW
        }
    }
    val filterPanel=panel==CameraPanel.FILTERS
    var compareOriginal by remember {mutableStateOf(false)}
    var filterThumbnail by remember {mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)}
    val filterPreviewEnabled=remember {AtomicBoolean(false)}
    filterPreviewEnabled.set(filterPanel)
    val analysisPaused=remember {AtomicBoolean(false)}
    analysisPaused.set(panel==CameraPanel.REVIEW)
    var subjectOriginalZoom by remember {mutableStateOf<Float?>(null)}
    var subjectAutoArmed by remember {mutableStateOf(false)}
    var subjectMessage by remember {mutableStateOf("")}
    var compositionActive by remember {mutableStateOf(false)}
    var compositionAnalyzing by remember {mutableStateOf(false)}
    var compositionStarted by remember {mutableLongStateOf(0L)}
    var centeredFrames by remember {mutableIntStateOf(0)}
    var trackedFrames by remember {mutableIntStateOf(0)}
    var preserveScene by remember {mutableStateOf(true)}
    var compositionTarget by remember {mutableStateOf(SubjectColor.Point(.5f,.5f))}
    val subjectTracker=remember {CompositionTracking()}
    fun clearComposition() {
        selectionGeneration.incrementAndGet();pendingTap.set(null);subjectEdges=emptyList()
        compositionActive=false;compositionAnalyzing=false;subjectTracker.reset();centeredFrames=0;trackedFrames=0
        subjectBox=null;subjectRegion.set(null);selectingSubject=false;subjectMessage=""
    }

    var templateIndex by remember { mutableIntStateOf(0) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var score by remember { mutableIntStateOf(0) }
    var quality by remember { mutableStateOf(CaptureQuality.UNKNOWN) }
    val landscapeGuidance=remember {LandscapeGuidance()}
    var landscapeTip by remember {mutableStateOf<String?>(null)}
    var portraitHint by remember { mutableStateOf(PortraitGuidance.Hint(PortraitGuidance.Kind.UNKNOWN, "对准人物，轻点脸部确认对焦")) }
    var faces by remember { mutableStateOf<List<FaceRegion>>(emptyList()) }
    var detected by remember { mutableStateOf<Landmarks?>(null) }
    var autoCapture by remember { mutableStateOf(false) }
    var aiReview by remember { mutableStateOf("") }
    var aiLoading by remember { mutableStateOf(false) }
    val optionsOpen=panel==CameraPanel.TOOLS
    var photoSaved by remember {mutableStateOf(false)}
    var savedAt by remember {mutableLongStateOf(0L)}
    val capturing = photos.capturing
    val framing = remember { AutoFraming() }
    val presence = remember { ScenePresence() }
    val sceneGate = remember { SceneCaptureGate() }
    val faceGate = remember { FaceCaptureGate() }
    val groupZoomSession=remember {GroupZoomSession()}
    var peopleCount by remember { mutableStateOf<Int?>(null) }
    var peopleSettled by remember { mutableStateOf(false) }
    val posePanel=panel==CameraPanel.POSES
    var lastGroupZoom by remember { mutableLongStateOf(0L) }
    var poseCategory by remember { mutableStateOf<PoseCategory?>(null) }
    val poseOptions = remember(peopleCount,poseCategory,referenceStyle) { if(mode==CameraMode.PORTRAIT) GroupPoses.options((peopleCount ?: 1).coerceAtLeast(1),poseCategory,referenceStyle) else emptyList() }
    val selectedPose = poseOptions.getOrNull(templateIndex.coerceAtMost((poseOptions.size-1).coerceAtLeast(0)))
    val guideResource = remember { context.assets.open(PhotographyGuide.ASSET).bufferedReader().use {it.readText()} }
    var lastRemoteRequest by remember { mutableLongStateOf(0L) }
    var framingTip by remember { mutableStateOf("正在识别画面…") }
    var progress by remember { mutableFloatStateOf(0f) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoom by remember {mutableFloatStateOf(1f)}
    var maxZoom by remember {mutableFloatStateOf(1f)}
    var zoomDialExpanded by remember {mutableStateOf(false)}
    var savedResolution by remember {mutableStateOf("")}
    var touchDown by remember {mutableStateOf(Offset.Zero)}
    var touchStarted by remember {mutableLongStateOf(0L)}
    var touchMoved by remember {mutableStateOf(false)}
    var touchPinched by remember {mutableStateOf(false)}
    DisposableEffect(camera,lifecycleOwner) {
        val state=camera?.cameraInfo?.zoomState
        val observer=androidx.lifecycle.Observer<androidx.camera.core.ZoomState> {z->
            minZoom=z.minZoomRatio;maxZoom=minOf(z.maxZoomRatio,10f);zoomRatio=z.zoomRatio
        }
        state?.observe(lifecycleOwner,observer)
        onDispose {state?.removeObserver(observer)}
    }
    var zoomPending by remember { mutableStateOf(false) }
    var zoomSettledAt by remember { mutableLongStateOf(0L) }
    var lastResultAt by remember { mutableLongStateOf(0L) }
    var armed by remember { mutableStateOf(true) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val generation = remember { AtomicInteger(0) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    var showGuide by remember { mutableStateOf(false) } // 照片参考优先；轮廓按需开启

    var previewSize by remember { mutableStateOf(1 to 1) }
    val template = remember(selectedPose, previewSize) {
        PoseTemplate.ALL[selectedPose?.slots?.firstOrNull()?.pose ?: 0].forViewport(previewSize.first.toFloat(), previewSize.second.toFloat())
    }
    var highResolution by remember {mutableStateOf(true)}
    val imageCapture = remember(highResolution) { CameraQuality.capture(highResolution) }
    var style by remember { mutableStateOf(PhotoStyle.ORIGINAL) }
    var autoStyle by remember { mutableStateOf(true) }
    var exposureEv by remember { mutableFloatStateOf(0f) }
    var appliedPlan by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(plan?.id) {
        plan?.takeIf { it.id != appliedPlan }?.let {
            autoStyle=false; style=it.style; grade=ColorGrade(strength=.6f); autoExposure=true
            appliedPlan=it.id
        }
    }
    var lastMeterAt by remember { mutableLongStateOf(0L) }
    var lastFocusAt by remember { mutableLongStateOf(0L) }
    var lastExposureAt by remember { mutableLongStateOf(0L) }
    var focusPending by remember { mutableStateOf(false) }
    var focusReady by remember { mutableStateOf(true) }
    val sceneOptimizer = remember { SceneOptimizer() }
    val previewRef = remember { AtomicReference<PreviewView?>(null) }
    BackHandler(enabled=panel!=CameraPanel.REVIEW && (timer.active || selectingSubject || filterPanel || optionsOpen || posePanel || compositionActive || zoomDialExpanded)) { if(zoomDialExpanded) zoomDialExpanded=false else if(timer.active) timer.cancel() else if(selectingSubject) {clearComposition()} else if(filterPanel) {panel=CameraPanel.NONE;compareOriginal=false} else if(optionsOpen) panel=CameraPanel.NONE else if(posePanel) panel=CameraPanel.NONE else if(compositionActive) {clearComposition();autoCapture=false;subjectAutoArmed=false;framing.reset();sceneGate.reset();faceGate.reset()} }
    DisposableEffect(lifecycleOwner) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_RESUME) hasPermission=requiredPermissions.all {
                ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED
            }
            if(event==androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                clearComposition();timer.cancel();subjectAutoArmed=false;selectingSubject=false;subjectBox=null;subjectRegion.set(null);sceneGate.reset();progress=0f
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {lifecycleOwner.lifecycle.removeObserver(observer)}
    }
    fun capture() {
        if(camera==null || timer.active || !photos.canCapture) return
        clearComposition();framing.lock();sceneGate.reset();faceGate.reset();armed=false;progress=0f;subjectAutoArmed=false;photoSaved=false
        timer.start(timerSeconds) {
            if(camera!=null) {
                val ticket = photos.reserve() ?: return@start
                PhotoCapture.take(context,imageCapture,style,grade,onOriginalSaved={ uri ->
                    photos.original(ticket, uri)
                    onPhotoSaved(uri.toString())
                    photoSaved=true;savedAt=SystemClock.elapsedRealtime()
                }) { result ->
                    photos.finish(ticket, result)
                    if(result.original!=null && photos.latest?.id==ticket) {
                        savedResolution="${result.width}×${result.height} · ${"%.1f".format(result.width.toLong()*result.height/1_000_000f)} MP"
                    }
                    result.error?.let {Toast.makeText(context,it,Toast.LENGTH_LONG).show()}
                }
            }
        }
    }

    fun setZoom(value: Float) {
        val active = camera ?: return
        val state = active.cameraInfo.zoomState.value ?: return
        selectionGeneration.incrementAndGet();pendingTap.set(null);selectingSubject=false;subjectEdges=emptyList()
        subjectTracker.reset();subjectBox=null;subjectRegion.set(null)
        zoomPending = true
        framing.reset(); sceneGate.reset()
        cameraError = null
        val future = active.cameraControl.setZoomRatio(value.coerceIn(state.minZoomRatio, minOf(state.maxZoomRatio, 10f)))
        future.addListener({
            if (camera === active) {
                zoomPending = false
                zoomSettledAt = SystemClock.elapsedRealtime() + 600
                runCatching { future.get() }.onFailure { cameraError = "变焦失败，请手动调整距离" }
                zoomRatio = active.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                if(compositionActive && mode==CameraMode.LANDSCAPE && cameraError==null) subjectMessage="构图已调整 · 检查边缘后按快门拍摄"
            }
        }, mainExecutor)
    }

    fun manualZoom(value:Float) {
        timer.cancel();subjectAutoArmed=false;autoCapture=false;settings.autoCapture=false
        setZoom(value)
        if(compositionActive && mode==CameraMode.LANDSCAPE) {compositionAnalyzing=false;subjectMessage="点击物体选择构图主体"}
    }
    val currentPinchRatio by rememberUpdatedState(zoomRatio)
    val pinchZoom by rememberUpdatedState<(Float)->Unit> {manualZoom(it)}
    val scaleDetector=remember {android.view.ScaleGestureDetector(context,object:android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var desiredRatio=1f
        override fun onScaleBegin(detector:android.view.ScaleGestureDetector):Boolean {
            desiredRatio=currentPinchRatio;touchPinched=true;return !capturing && !timer.active
        }
        override fun onScale(detector:android.view.ScaleGestureDetector):Boolean {
            touchPinched=true
            desiredRatio=(desiredRatio*detector.scaleFactor).coerceIn(minZoom,maxZoom)
            if(!capturing && !timer.active) pinchZoom(desiredRatio)
            return true
        }
    })}

    val sceneConsumer by rememberUpdatedState<(SceneOptimizer.Reading, Boolean) -> Unit> { reading, portrait ->
        val now = SystemClock.elapsedRealtime()
        if (!capturing && now-lastMeterAt > 650) {
            lastMeterAt=now
            val choice=sceneOptimizer.update(reading, portrait, exposureEv)
            if(choice!=null) {
                if(compositionActive && subjectRegion.get()==null) compositionAnalyzing=false
                val next=if(autoStyle && subjectRegion.get()==null && !selectingSubject && !filterPanel) choice.style else style
                if(autoStyle && subjectRegion.get()==null && !filterPanel) grade=grade.copy(strength=.6f,shadows=choice.shadows,highlights=choice.highlights)
                if(style!=next) {style=next;framing.reset();zoomSettledAt=now+700}
                val active=camera
                val exposure=active?.cameraInfo?.exposureState
                if(autoExposure && exposure!=null && exposure.isExposureCompensationSupported && now-lastExposureAt>2500) {
                    val step=exposure.exposureCompensationStep.toFloat()
                    val index=if(step>0f) (choice.exposureEv/step).roundToInt().coerceIn(exposure.exposureCompensationRange.lower,exposure.exposureCompensationRange.upper) else 0
                    if(index!=exposure.exposureCompensationIndex) {
                        lastExposureAt=now;framing.reset();zoomSettledAt=now+1200
                        val future=active.cameraControl.setExposureCompensationIndex(index)
                        future.addListener({ if(camera===active) {
                            runCatching {future.get()}.onSuccess {exposureEv=index*step}
                            zoomSettledAt=SystemClock.elapsedRealtime()+700
                        }},mainExecutor)
                    }
                }
            }
        }
    }

    // Confidence-filtered observations and UI state are consumed on the main thread.
    val resultConsumer by rememberUpdatedState<(SceneObservation) -> Unit> { observation ->
        val now=SystemClock.elapsedRealtime()
        lastResultAt=now
        quality=CaptureQuality.measure(observation.signature)
        if(mode==CameraMode.LANDSCAPE) landscapeTip=landscapeGuidance.update(observation.signature,now)
        faces=observation.faces
        if(compositionActive && mode==CameraMode.LANDSCAPE && !selectingSubject && !zoomPending && !capturing && !timer.active && panel==CameraPanel.NONE) {
            subjectBox?.let {box->
                val tracked=subjectTracker.update(observation.signature,box)
                if(tracked==null) {
                    if(now-compositionStarted>900) {
                        subjectBox=null;subjectRegion.set(null);subjectEdges=emptyList();subjectMessage="主体跟踪不确定，请重新点击物体";centeredFrames=0
                    }
                } else {
                    trackedFrames++
                    if(trackedFrames>=3) compositionAnalyzing=false
                    if(kotlin.math.abs(tracked.cx-box.cx)+kotlin.math.abs(tracked.cy-box.cy)>.02f) {
                        // The original color mask must not be sampled over a moved background.
                        subjectRegion.set(null)
                    }
                    val dx=tracked.cx-box.cx;val dy=tracked.cy-box.cy
                    if(dx!=0f || dy!=0f) subjectEdges=subjectEdges.map {edge->TapSubjectMask.Edge(
                        SubjectColor.Point((edge.from.x+dx).coerceIn(0f,1f),(edge.from.y+dy).coerceIn(0f,1f)),
                        SubjectColor.Point((edge.to.x+dx).coerceIn(0f,1f),(edge.to.y+dy).coerceIn(0f,1f)))}
                    subjectBox=tracked
                    centeredFrames=if(kotlin.math.abs(tracked.cx-compositionTarget.x)<.07f && kotlin.math.abs(tracked.cy-compositionTarget.y)<.09f) centeredFrames+1 else 0
                    if(centeredFrames>=5 && now-compositionStarted>1200) {
                        if(preserveScene) subjectMessage="主体位置合适，检查画面四边后按快门"
                        else camera?.cameraInfo?.zoomState?.value?.let {z->
                            val choice=SubjectFraming.choose(tracked,z.zoomRatio,z.minZoomRatio,z.maxZoomRatio)
                            subjectMessage=if(choice.zoom!=null) "正在调整构图，完成后按快门拍摄" else choice.message
                            subjectBox=null;subjectTracker.reset();subjectRegion.set(null);subjectEdges=emptyList()
                            choice.zoom?.let {subjectOriginalZoom=z.zoomRatio;setZoom(it)}
                        }
                    } else subjectMessage=if(preserveScene && compositionTarget.x!=.5f) "缓慢转动手机，让主体靠近三分线参考点" else "缓慢转动手机，让主体靠近中央参考点"
                }
            }
        }
        val previous=peopleCount
        peopleCount=presence.update(mode.effectiveCount(observation.count),now)
        peopleSettled=presence.settled && mode.effectiveCount(observation.count)==peopleCount
        if(previous!=peopleCount) {
            templateIndex=0;poseCategory=null;groupZoomSession.reset();framing.reset();sceneGate.reset();faceGate.reset();progress=0f
            lastFocusAt=0L;focusReady=true;sceneOptimizer.reset();if(autoStyle) style=PhotoStyle.ORIGINAL
        }
        if(mode==CameraMode.PORTRAIT && peopleCount!=0) {subjectAutoArmed=false;subjectRegion.set(null);subjectBox=null;subjectEdges=emptyList();selectingSubject=false}
        val people=observation.people
        val points=people.firstOrNull()?.let {PoseAligner.orient(template,it)}
        detected=points
        portraitHint=PortraitGuidance.next(points,faces,peopleCount,intent,selectedPose?.tip ?: "身体稍侧转，手臂自然放松")
        if(!peopleSettled || peopleCount==null) {
            score=0;progress=0f;framing.reset();sceneGate.reset();faceGate.reset()
            framingTip="对准人物，轻点脸部确认对焦"
        } else {
            val count=peopleCount!!
            val alignment=if(count==1 && points!=null && portraitHint.kind==PortraitGuidance.Kind.FULL) PoseAligner.evaluate(template,points) else null
            score=alignment?.score ?: 0
            if(showGuide && alignment!=null && score<78 && portraitHint.ready) portraitHint=portraitHint.copy(message=alignment.tips.firstOrNull() ?: portraitHint.message)
            val active=camera;val view=previewRef.get()
            if(active!=null && view!=null && !capturing && !focusPending && now-lastFocusAt>6500) {
                val face=if(count==0) (.5f to .5f) else faces.getOrNull(faces.size/2)?.let {it.cx to it.cy} ?: people.getOrNull(people.size/2)?.get(0)
                if(face!=null && face.first in 0f..1f && face.second in 0f..1f) {
                    lastFocusAt=now
                    val action=FocusMeteringAction.Builder(view.meteringPointFactory.createPoint(face.first*view.width,face.second*view.height)).build()
                    if(active.cameraInfo.isFocusMeteringSupported(action)) {
                        focusPending=true;focusReady=false;framing.reset();sceneGate.reset()
                        val future=active.cameraControl.startFocusAndMetering(action)
                        future.addListener({if(camera===active) {
                            focusPending=false;lastFocusAt=SystemClock.elapsedRealtime();zoomSettledAt=lastFocusAt+500
                            focusReady=runCatching {future.get().isFocusSuccessful}.getOrDefault(false)
                        }},mainExecutor)
                    }
                }
            }
            val enabled=(autoCapture || subjectAutoArmed || (compositionActive && mode==CameraMode.PORTRAIT)) && armed && camera!=null && !capturing && cameraError==null && panel==CameraPanel.NONE && !zoomDialExpanded && !timer.active && !selectingSubject && mode.permitsAutoCapture(peopleCount)
            val ready=!zoomPending && !focusPending && focusReady && now>=zoomSettledAt && quality.permitsAutomatic && photos.canCapture
            val zoom=camera?.cameraInfo?.zoomState?.value
            if(!enabled) {
                framing.reset();sceneGate.reset();faceGate.reset();progress=0f
                framingTip=if(!armed && autoCapture) "已拍好，点「再拍一张」继续" else portraitHint.message
            } else if(mode==CameraMode.PORTRAIT && (portraitHint.kind!=PortraitGuidance.Kind.FULL || people.any {p->listOf(0,11,12,23,24,27,28).any {p[it]==null}})) {
                framing.reset();sceneGate.reset()
                val decision=faceGate.update(now,faces,count,ready && portraitHint.ready)
                framingTip=if(faces.size!=count) "轻点脸部确认对焦，也可直接按快门" else if(portraitHint.ready) decision.message else portraitHint.message
                progress=decision.progress
                if(decision.shoot && autoCapture) capture()
                else if(decision.shoot) framingTip="构图就绪，按快门记录此刻"
            } else if(count==1 && points!=null) {
                val decision=framing.evaluate(points,template,if(showGuide) score else 100,now,zoom?.zoomRatio ?: 1f,zoom?.minZoomRatio ?: 1f,
                    minOf(zoom?.maxZoomRatio ?: 1f,3f),true,ready)
                framingTip=if(decision.tip.startsWith("按轮廓") && alignment!=null) alignment.tips.firstOrNull() ?: decision.tip else decision.tip;progress=decision.progress
                decision.zoom?.let {setZoom(it)}
                if(decision.shoot && (autoCapture || subjectAutoArmed)) capture() else if(decision.shoot) {framingTip="构图就绪，按快门记录此刻";framing.reset(rearm=true)}
            } else {
                val zoomDecision=if(count in 2..4 && ready && now-lastGroupZoom>700)
                    groupZoomSession.evaluate(people,zoom?.zoomRatio ?: 1f,zoom?.minZoomRatio ?: 1f,minOf(zoom?.maxZoomRatio ?: 1f,3f),selectedPose?.let{GroupPoses.targets(it,previewSize.first.toFloat(),previewSize.second.toFloat())}) else GroupZoomSession.Decision()
                val nextZoom=zoomDecision.zoom
                if(zoomDecision.blocked) {framingTip="自动变焦已暂停，请换机位或手动拍摄";sceneGate.reset();progress=0f}
                else if(nextZoom!=null) {lastGroupZoom=now;setZoom(nextZoom);framingTip="正在调整合影范围…";progress=0f}
                else {
                    val decision=sceneGate.update(now,count,peopleSettled,observation.signature,people,ready)
                    framingTip=if(count>4) "已达到检测上限，五人及以上请手动拍摄" else decision.message
                    progress=decision.progress
                    if(decision.shoot && (autoCapture || subjectAutoArmed)) capture()
                }
            }
            if(enabled && !quality.permitsAutomatic) framingTip=quality.hint
            else if(enabled && !focusReady) framingTip="轻点人物确认对焦，也可直接按快门"
        }
    }
    val previewRenderer=remember {PreviewColorRenderer()}
    val previewLut=remember(style,grade,compareOriginal) {StyleLut.create(if(compareOriginal) PhotoStyle.ORIGINAL else style,if(compareOriginal) ColorGrade() else grade)}
    val faceDetector=remember {AtomicReference<FaceDetector?>(null)}
    var poseModelFailed by remember {mutableStateOf(false)}
    var faceModelFailed by remember {mutableStateOf(false)}
    val modelFailed = poseModelFailed || faceModelFailed
    var modelAttempt by remember {mutableIntStateOf(0)}
    val landmarker = remember { AtomicReference<PoseLandmarker?>(null) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    LaunchedEffect(modelAttempt) {
        if(mode.usesPoseModel) analyzerExecutor.execute {
            if(landmarker.get()==null) runCatching {
                PoseLandmarker.createFromOptions(context,
                    PoseLandmarker.PoseLandmarkerOptions.builder()
                        .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build())
                        .setRunningMode(RunningMode.VIDEO).setNumPoses(5)
                        .setMinPoseDetectionConfidence(.5f).setMinPosePresenceConfidence(.5f).setMinTrackingConfidence(.5f).build())
            }.onSuccess {landmarker.set(it);Diagnostics.event(Event.MODEL_READY,Field.MODEL to 1)}
                .onFailure {Diagnostics.error(Event.MODEL_ERROR,it,Field.MODEL to 1)}
            if(faceDetector.get()==null) runCatching {
                FaceDetector.createFromOptions(context,FaceDetector.FaceDetectorOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_detector_short_range.tflite").build())
                    .setRunningMode(RunningMode.VIDEO).setMinDetectionConfidence(.5f).build())
            }.onSuccess {faceDetector.set(it);Diagnostics.event(Event.MODEL_READY,Field.MODEL to 2)}
                .onFailure {Diagnostics.error(Event.MODEL_ERROR,it,Field.MODEL to 2)}
            mainExecutor.execute {poseModelFailed=landmarker.get()==null;faceModelFailed=faceDetector.get()==null}
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            generation.incrementAndGet();selectionGeneration.incrementAndGet();pendingTap.set(null)
            segmentationExecutor.execute {segmenter.close()}
            segmentationExecutor.shutdown()
            analyzerExecutor.execute { landmarker.getAndSet(null)?.close();faceDetector.getAndSet(null)?.close() }
            analyzerExecutor.shutdown()
        }
    }
    LaunchedEffect(lensFacing, autoCapture) {
        framing.reset(rearm = true); armed = true; progress = 0f
        score = 0; detected = null;sceneGate.reset()
        zoomSettledAt = SystemClock.elapsedRealtime() + 800
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            if(photoSaved && SystemClock.elapsedRealtime()-savedAt>3500) photoSaved=false
            if(compositionAnalyzing && SystemClock.elapsedRealtime()-compositionStarted>6500) {
                selectionGeneration.incrementAndGet();pendingTap.set(null);selectingSubject=false
                compositionAnalyzing=false;subjectMessage="识别暂未就绪，请重新点击物体或直接拍摄"
            }
            if (SystemClock.elapsedRealtime() - lastResultAt > 1100) {
                if(subjectBox!=null) subjectMessage="跟踪已中断，请重新点击物体"
                subjectRegion.set(null);subjectEdges=emptyList()
                framing.reset(); sceneGate.reset();presence.reset();peopleCount=null;peopleSettled=false
                progress = 0f; score = 0; detected = null;subjectTracker.reset();subjectBox=null
                framingTip = if (landmarker.get() == null) "识别尚未就绪，可使用手动拍摄" else "识别中断或画面不清晰，等待确认场景…"
            }
        }
    }

    fun doManualCapture() { capture() }

    fun requestAiReview() {
        if (aiLoading) return
        val config = settings.currentConfig()
        if (config.apiKey.isBlank()) {
            aiReview = "本地优化正常运行；在线摄影建议暂不可用"
            return
        }
        val requestTime=SystemClock.elapsedRealtime()
        if(lastRemoteRequest>0 && requestTime-lastRemoteRequest<15000) {aiReview="在线点评请间隔15秒；本地优化持续运行";return}
        lastRemoteRequest=requestTime
        aiLoading = true
        aiReview = ""
        scope.launch {
            aiReview = try {
                LlmClient().chat(
                    config = config,
                    systemPrompt = PhotographyGuide.systemPrompt(guideResource),
                    userPrompt = PhotographyGuide.sceneContext(peopleCount,peopleSettled,style.label,camera?.cameraInfo?.exposureState?.let{it.exposureCompensationIndex*it.exposureCompensationStep.toFloat()} ?: exposureEv,selectedPose?.name)+"用户手选意图：${intent.label}；本地提示：${intent.advice}；没有景物语义识别。",
                )
            } catch (e: Exception) {
                "AI 点评失败：${e.message?.take(120) ?: "网络错误"}"
            }
            aiLoading = false
        }
    }

    if (!hasPermission) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(Modifier.padding(CameraDesign.Page),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("允许使用相机",style=MaterialTheme.typography.headlineSmall)
                Text("开启相机权限后即可取景拍摄。",style=MaterialTheme.typography.bodyMedium,color=TextSecondary)
                TextButton(onClick=onOpenAssistant) {Text("先规划拍摄") }
                TextButton(onClick={permissionLauncher.launch(requiredPermissions)}) {Text("开启权限")}
                TextButton(onClick={context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${context.packageName}")))}) {Text("打开系统设置")}
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(BgDark).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal=12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            IconButton(enabled=!capturing && camera?.cameraInfo?.hasFlashUnit()==true,onClick={flashMode=when(flashMode){ImageCapture.FLASH_MODE_OFF->ImageCapture.FLASH_MODE_AUTO;ImageCapture.FLASH_MODE_AUTO->ImageCapture.FLASH_MODE_ON;else->ImageCapture.FLASH_MODE_OFF}}) {
                Icon(when(flashMode){ImageCapture.FLASH_MODE_ON->Icons.Filled.FlashOn;ImageCapture.FLASH_MODE_AUTO->Icons.Filled.FlashAuto;else->Icons.Filled.FlashOff},"闪光灯：${when(flashMode){ImageCapture.FLASH_MODE_ON->"开";ImageCapture.FLASH_MODE_AUTO->"自动";else->"关"}}",tint=Color.White)
            }
            TextButton(enabled=!capturing,onClick={timer.cancel();onOpenAssistant()}) {Text("拍摄助手",color=TextPrimary,style=MaterialTheme.typography.titleMedium)}
            IconButton(enabled=!capturing,onClick={timer.cancel();panel=CameraPanel.TOOLS}) {Icon(Icons.Outlined.MoreHoriz,"更多拍摄设置",tint=TextPrimary)}
        }
      Box(Modifier.fillMaxWidth().weight(1f).onSizeChanged { previewSize = it.width to it.height }) {
        // ---- 相机预览 + 绑定（镜头切换时重建）----
        val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
        var boundLens by remember { mutableIntStateOf(Int.MIN_VALUE) }
        var boundSize by remember { mutableStateOf(0 to 0) }
        var boundHighResolution by remember {mutableStateOf<Boolean?>(null)}

        val ownedUseCases = remember { mutableListOf<androidx.camera.core.UseCase>() }
        DisposableEffect(lifecycleOwner) {
            onDispose {
                generation.incrementAndGet()
                if (providerFuture.isDone) runCatching { providerFuture.get().unbind(*ownedUseCases.toTypedArray()) }
            }
        }
        AndroidView(
            factory = { ctx -> PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode=PreviewView.ImplementationMode.COMPATIBLE
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                previewRef.set(this)
            } },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setOnTouchListener { _,event ->
                    scaleDetector.onTouchEvent(event)
                    val x=(event.x/view.width.coerceAtLeast(1)).coerceIn(0f,1f)
                    val y=(event.y/view.height.coerceAtLeast(1)).coerceIn(0f,1f)
                    when(event.actionMasked) {
                        MotionEvent.ACTION_DOWN->{touchDown=Offset(event.x,event.y);touchStarted=event.eventTime;touchMoved=false;touchPinched=false}
                        MotionEvent.ACTION_POINTER_DOWN->touchPinched=true
                        MotionEvent.ACTION_MOVE->if((Offset(event.x,event.y)-touchDown).getDistance()>android.view.ViewConfiguration.get(context).scaledTouchSlop) touchMoved=true
                        MotionEvent.ACTION_UP->if(!touchMoved && !touchPinched && event.eventTime-touchStarted<500 && !capturing && !timer.active && panel==CameraPanel.NONE) {
                            val point=view.meteringPointFactory.createPoint(event.x,event.y)
                            camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                            lastFocusAt=SystemClock.elapsedRealtime()
                            if(compositionActive && mode==CameraMode.LANDSCAPE) {
                                val token=selectionGeneration.incrementAndGet()
                                subjectTracker.reset();subjectBox=null;subjectRegion.set(null);subjectEdges=emptyList()
                                centeredFrames=0;trackedFrames=0;selectingSubject=true;compositionAnalyzing=true;compositionStarted=SystemClock.elapsedRealtime()
                                subjectMessage="正在识别点击的物体…"
                                Diagnostics.event(Event.TAP_START)
                                pendingTap.set(TapSelectionRequest(x,y,token,generation.get(),compositionStarted))
                            }
                            view.performClick()
                        }
                        MotionEvent.ACTION_CANCEL->{touchMoved=true}
                    }
                    true
                }
                imageCapture.flashMode = flashMode
                previewRenderer.apply(view,previewLut)
                if (boundLens != lensFacing || boundSize != previewSize || boundHighResolution != highResolution) {
                    subjectRegion.set(null)
                    boundHighResolution=highResolution
                    boundSize = previewSize
                    boundLens = lensFacing
                    val lens = lensFacing
                    val session = generation.incrementAndGet()
                    camera = null; cameraError = null; zoomPending = false; zoomRatio = 1f
                    exposureEv=0f;focusPending=false;focusReady=true;lastFocusAt=0L
                    framing.reset();sceneGate.reset();presence.reset();peopleCount=null;peopleSettled=false;progress = 0f
                    zoomSettledAt = SystemClock.elapsedRealtime() + 800
                    providerFuture.addListener({
                        view.doOnLayout {
                            if (session != generation.get()) return@doOnLayout
                            try {
                                val provider = providerFuture.get()
                                val rotation = view.display.rotation
                                val preview = Preview.Builder().setTargetRotation(rotation).build().also { it.setSurfaceProvider(view.surfaceProvider) }
                                val analysis = ImageAnalysis.Builder()
                                    .setTargetRotation(rotation)
                                    .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(640,480),ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build())
                                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                                imageCapture.targetRotation = rotation
                                var lastAnalysis=0L
                                var lastThumbnail=0L
                                var lastDiagnostic=0L
                                var lastFaceAt=0L
                                var cachedFaces=emptyList<FaceRegion>()
                                val samplePixels=IntArray(SubjectColor.GRID*SubjectColor.GRID)
                                analysis.setAnalyzer(analyzerExecutor) { frame ->
                                    frame.use { img ->
                                        if (session != generation.get()) return@use
                                        if (analysisPaused.get()) return@use
                                        val started = SystemClock.elapsedRealtime()
                                        if(started-lastAnalysis<120) return@use
                                        lastAnalysis=started
                                        val lm = landmarker.get()
                                        val bmp = img.toRotatedBitmap() ?: return@use
                                        try {
                                            pendingTap.getAndSet(null)?.let {request->
                                                val snapshot=if(lens==CameraSelector.LENS_FACING_FRONT) Bitmap.createBitmap(bmp,0,0,bmp.width,bmp.height,Matrix().apply {setScale(-1f,1f)},true)
                                                    else bmp.copy(Bitmap.Config.ARGB_8888,false)
                                                try {segmentationExecutor.execute {
                                                    val selection=try {if(request.token==selectionGeneration.get()) segmenter.select(snapshot,request.x,request.y) else null} catch(e:Exception) {Diagnostics.error(Event.TAP_ERROR,e);null} finally {snapshot.recycle()}
                                                    Diagnostics.event(Event.TAP_RESULT,Field.ACCEPTED to if(selection!=null) 1 else 0,Field.DURATION_MS to (SystemClock.elapsedRealtime()-request.started))
                                                    mainExecutor.execute {
                                                        if(request.token==selectionGeneration.get() && request.session==generation.get() && compositionActive && lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                                                            selectingSubject=false;compositionAnalyzing=false
                                                            if(selection==null || SystemClock.elapsedRealtime()-request.started>6500) subjectMessage="没能识别主体，请点击物体内部重试"
                                                            else {
                                                                subjectBox=selection.box;subjectRegion.set(selection.region);subjectEdges=selection.edges
                                                                compositionTarget=SubjectFraming.target(selection.box,preserveScene)
                                                                subjectSession=SubjectColor.Session();subjectTracker.reset();centeredFrames=0;trackedFrames=0
                                                                compositionStarted=SystemClock.elapsedRealtime();subjectMessage="已选中主体 · 缓慢调整手机构图"
                                                            }
                                                        }
                                                    }
                                                }} catch(_:java.util.concurrent.RejectedExecutionException) {snapshot.recycle()}
                                            }
                                            val pixels=samplePixels
                                            for(index in pixels.indices) pixels[index]=bmp.getPixel(SubjectColor.sampleX(index%32,bmp.width,lens==CameraSelector.LENS_FACING_FRONT),((index/32+.5f)*bmp.height/32).toInt().coerceAtMost(bmp.height-1))
                                            val thumbnail=if(filterPreviewEnabled.get() && started-lastThumbnail>1000) {
                                                lastThumbnail=started
                                                Bitmap.createScaledBitmap(bmp,120,(120f*bmp.height/bmp.width).roundToInt().coerceAtLeast(1),true).let {small->
                                                    if(lens==CameraSelector.LENS_FACING_FRONT) Bitmap.createBitmap(small,0,0,small.width,small.height,Matrix().apply{setScale(-1f,1f)},true).also{if(it!==small) small.recycle()} else small
                                                }
                                            } else null
                                            val roi=subjectRegion.get()
                                            val roiReading=roi?.let{SubjectColor.measure(pixels,it)}
                                            val frameWidth = bmp.width
                                            val frameHeight = bmp.height
                                            val input = BitmapImageBuilder(bmp).build()
                                            var faceCount:Int?=null
                                            val result = try {
                                                val timestamp=SystemClock.uptimeMillis()
                                                val pose=try {lm?.detectForVideo(input,timestamp)} catch(e:Exception) {if(started-lastDiagnostic>2000) Diagnostics.error(Event.FRAME_ERROR,e);null}
                                                if(mode==CameraMode.PORTRAIT && started-lastFaceAt>240) {
                                                    lastFaceAt=started
                                                    cachedFaces=faceDetector.get()?.detectForVideo(input,timestamp)?.detections().orEmpty().map {detection->
                                                        val box=detection.boundingBox()
                                                        val left=(box.left/bmp.width).coerceIn(0f,1f)
                                                        val right=(box.right/bmp.width).coerceIn(0f,1f)
                                                        FaceRegion(if(lens==CameraSelector.LENS_FACING_FRONT) 1f-right else left,
                                                            (box.top/bmp.height).coerceIn(0f,1f),
                                                            if(lens==CameraSelector.LENS_FACING_FRONT) 1f-left else right,
                                                            (box.bottom/bmp.height).coerceIn(0f,1f))
                                                    }.filter{it.valid}.sortedBy{it.cx}
                                                }
                                                if(mode==CameraMode.PORTRAIT) faceCount=if(faceDetector.get()!=null) cachedFaces.size else null
                                                pose
                                            } finally { input.close() }
                                            val rawPeople=if(mode==CameraMode.LANDSCAPE) emptyList<Landmarks>() else result?.landmarks()?.map { person ->
                                                person.mapIndexedNotNull { idx, point ->
                                                    if(PortraitDetection.visible(point.x(),point.y(),point.visibility().orElse(1f),point.presence().orElse(1f)))
                                                        idx to ((if(lens==CameraSelector.LENS_FACING_FRONT) 1f-point.x() else point.x()) to point.y()) else null
                                                }.toMap()
                                            }
                                            val people=rawPeople.orEmpty().filter {PortraitDetection.guidable(it)}.sortedBy {(it.getValue(11).first+it.getValue(12).first)/2}
                                            val observedFaces=cachedFaces.toList()
                                            val reading=SceneOptimizer.measure(pixels,observedFaces)
                                            val count=if(mode==CameraMode.LANDSCAPE) 0 else PortraitDetection.count(rawPeople?.size,faceCount)
                                            if(started-lastDiagnostic>2000) {
                                                lastDiagnostic=started
                                                Diagnostics.event(Event.FRAME,Field.MODE to mode.ordinal,Field.WIDTH to frameWidth,Field.HEIGHT to frameHeight,
                                                    Field.ROTATION to img.imageInfo.rotationDegrees,Field.DURATION_MS to (SystemClock.elapsedRealtime()-started),
                                                    Field.RAW_PEOPLE to (rawPeople?.size ?: -1),Field.RELIABLE_PEOPLE to people.size,Field.FACES to (faceCount ?: -1),Field.COUNT to (count ?: -1))
                                            }
                                            val signature=FloatArray(pixels.size){i-> val px=pixels[i];(.213f*(px ushr 16 and 255)+.715f*(px ushr 8 and 255)+.072f*(px and 255))/255f}
                                            mainExecutor.execute {
                                                if(session==generation.get() && lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) && SystemClock.elapsedRealtime()-started<900) {
                                                    thumbnail?.let{filterThumbnail=it.asImageBitmap()}
                                                    resultConsumer(SceneObservation(people,count,signature,observedFaces))
                                                    if(roi!=null && roi===subjectRegion.get() && roiReading!=null) {
                                                        val choice=subjectSession.update(roiReading,SystemClock.elapsedRealtime(),autoStyle)
                                                        if(subjectSession.invalidated) {subjectRegion.set(null);if(subjectBox==null) subjectMessage="画面已变化，请重新点击物体"}
                                                        else if(choice!=null && !capturing && !timer.active) {
                                                            compositionAnalyzing=false;style=choice.style;grade=grade.copy(strength=choice.strength)
                                                            subjectMessage="主体引导全局调色 · ${style.label}"
                                                        }
                                                    }
                                                    if(peopleSettled) sceneConsumer(reading,peopleCount!=null && peopleCount!!>0)
                                                }
                                            }
                                        } catch (e: Exception) {
                                Diagnostics.error(Event.CAMERA_ERROR,e)
                                            if(started-lastDiagnostic>2000) {lastDiagnostic=started;Diagnostics.error(Event.FRAME_ERROR,e)}
                                            mainExecutor.execute { if (session == generation.get()) resultConsumer(SceneObservation(emptyList(),null,floatArrayOf())) }
                                        } finally { bmp.recycle() }
                                    }
                                }
                                // Shared crop makes analysis, preview and saved image use the same framing.
                                val group = UseCaseGroup.Builder()
                                    .setViewPort(ViewPort.Builder(Rational(view.width, view.height), rotation).setScaleType(ViewPort.FILL_CENTER).build())
                                    .addUseCase(preview).addUseCase(analysis).addUseCase(imageCapture).build()
                                provider.unbindAll()
                                ownedUseCases.clear()
                                ownedUseCases.addAll(listOf(preview, analysis, imageCapture))
                                camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.Builder().requireLensFacing(lens).build(), group)
                                Diagnostics.event(Event.CAMERA_BIND,Field.MODE to mode.ordinal,Field.ROTATION to rotation)
                            } catch (e: Exception) {
                                Diagnostics.error(Event.CAMERA_ERROR,e)
                                if(highResolution) {highResolution=false;cameraError=null}
                                else cameraError = "相机启动失败，请切换镜头或重新打开"
                            }
                        }
                    }, mainExecutor)
                }
            },
        )

        if(mode==CameraMode.PORTRAIT && portraitHint.kind==PortraitGuidance.Kind.FULL && peopleCount in 1..4 && selectedPose!=null && showGuide) {
            HumanPoseGuide(selectedPose,Modifier.fillMaxSize(),matched=peopleCount==1 && score>=78 && detected!=null && template.points.keys.all{detected?.containsKey(it)==true})
        }
        FramingGuides(gridEnabled || (mode==CameraMode.LANDSCAPE && (compositionActive || guidanceEnabled && landscapeTip!=null)),level,Modifier.fillMaxSize())
        if(compositionActive && panel==CameraPanel.NONE && !selectingSubject && (compositionAnalyzing || subjectBox!=null)) CompositionOverlay(compositionAnalyzing,subjectBox,compositionTarget,Modifier.fillMaxSize())
        if(mode==CameraMode.PORTRAIT && panel==CameraPanel.NONE && !capturing) {
            if(referenceEnabled && selectedPose!=null && portraitHint.kind==PortraitGuidance.Kind.FULL) {
                PoseReferenceCard(selectedPose,{timer.cancel();panel=CameraPanel.POSES},Modifier.align(Alignment.TopStart).padding(12.dp))
            } else TextButton(onClick={timer.cancel();panel=CameraPanel.POSES},modifier=Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Text("姿势灵感",color=TextPrimary)
            }
        }
        if(subjectEdges.isNotEmpty()) Canvas(Modifier.fillMaxSize()) {
            subjectEdges.forEach {edge->drawLine(CameraYellow.copy(alpha=.85f),Offset(edge.from.x*size.width,edge.from.y*size.height),Offset(edge.to.x*size.width,edge.to.y*size.height),1.3.dp.toPx())}
        }
        if(timer.active) TextButton(onClick={timer.cancel()},modifier=Modifier.align(Alignment.Center)) {Text("${timer.seconds} · 取消",style=MaterialTheme.typography.headlineLarge,color=CameraYellow)}

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(CameraDesign.BottomScrim).padding(horizontal=8.dp,vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            val message=cameraError ?: when {
                capturing -> "正在拍摄…"
                timer.active -> "延时拍摄中"
                !photos.canCapture -> "正在生成成片，稍候即可继续拍摄"
                photoSaved -> if(photos.latest?.processing==true) "已保存原片 · 可以继续拍摄" else "已保存 · 轻点左下角查看"
                selectingSubject -> "正在识别点击的物体…"
                subjectBox!=null && compositionActive -> subjectMessage
                compositionActive && mode==CameraMode.LANDSCAPE -> subjectMessage.ifBlank {"点击物体选择构图主体"}
                mode==CameraMode.PORTRAIT && poseModelFailed && faceModelFailed -> "人物识别未启动 · 可按快门拍摄，轻点重试"
                mode==CameraMode.PORTRAIT && poseModelFailed -> "姿态引导未启动 · 人脸检测仍可用，轻点重试"
                mode==CameraMode.PORTRAIT && faceModelFailed -> "人脸检测未启动 · 姿态引导仍可用，轻点重试"
                (autoCapture || compositionActive && mode==CameraMode.PORTRAIT) -> framingTip
                !guidanceEnabled -> ""
                level.roll?.let{kotlin.math.abs(it)>3f}==true -> "调整手机左右倾斜，让水平线变平"
                quality.available && !quality.permitsAutomatic -> quality.hint
                mode==CameraMode.PORTRAIT -> portraitHint.message
                landscapeTip!=null && intent==TravelIntent.AUTO -> landscapeTip!!
                intent!=TravelIntent.AUTO -> TravelGuidance.next(intent,0,null,level.roll,"").substringAfter("：")
                quality.available && quality.highlights>.20f -> quality.hint
                else -> "稳住手机，检查画面四边后按快门"
            }
            if(message.isNotBlank()) Row(Modifier.padding(horizontal=16.dp,vertical=6.dp)
                .clickable(enabled=mode==CameraMode.PORTRAIT && modelFailed) {poseModelFailed=false;faceModelFailed=false;modelAttempt++},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                if(photoSaved) Icon(Icons.Outlined.CheckCircle,null,tint=Success,modifier=Modifier.size(16.dp))
                Text(message,color=TextPrimary,style=MaterialTheme.typography.bodySmall,maxLines=3)
            }
            if(!armed && autoCapture && !capturing) TextButton(onClick={
                timer.cancel();armed=true;photoSaved=false;framing.reset(rearm=true);sceneGate.reset();faceGate.reset();progress=0f
            }) {Text("再拍一张",color=CameraYellow)}
            if(compositionActive && mode==CameraMode.LANDSCAPE && subjectBox!=null) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(true to "保留景物",false to "突出主体").forEach {(preserve,label)->
                    FilterChip(selected=preserveScene==preserve,onClick={
                        preserveScene=preserve;subjectBox?.let {compositionTarget=SubjectFraming.target(it,preserve)};centeredFrames=0
                    },label={Text(label)})
                }
            }
            if(subjectOriginalZoom!=null) TextButton(enabled=!capturing,onClick={
                timer.cancel();subjectAutoArmed=false;autoCapture=false;subjectOriginalZoom?.let{setZoom(it)};subjectOriginalZoom=null
            },modifier=Modifier.heightIn(min=48.dp)) {Text("撤销自动放大",color=CameraYellow)}
            if(progress>0f && armed && (autoCapture || subjectAutoArmed) && !timer.active && !selectingSubject) LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().padding(top=4.dp),color=CameraYellow)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                if(!zoomDialExpanded) TextButton(enabled=!capturing,onClick={timer.cancel();panel=CameraPanel.FILTERS;compareOriginal=false;sceneGate.reset();framing.reset()},modifier=Modifier.width(72.dp).heightIn(min=48.dp)) {
                    Text("风格",color=TextPrimary,style=MaterialTheme.typography.labelLarge)
                }
                Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {
                    CameraZoomDial(zoomRatio,minZoom,maxZoom,!capturing && camera!=null,zoomDialExpanded,{timer.cancel();zoomDialExpanded=it},::manualZoom)
                }
                if(!zoomDialExpanded) TextButton(enabled=!capturing && camera!=null,onClick={
                    timer.cancel();autoCapture=false;subjectAutoArmed=false;framing.reset(rearm=true);sceneGate.reset();armed=true;photoSaved=false
                    if(compositionActive) clearComposition() else {
                        compositionActive=true;sceneOptimizer.reset();compositionStarted=SystemClock.elapsedRealtime();compositionAnalyzing=mode==CameraMode.PORTRAIT
                        subjectMessage=if(mode==CameraMode.LANDSCAPE) "点击物体选择构图主体" else "正在分析人像构图…"
                    }
                },modifier=Modifier.width(72.dp).heightIn(min=48.dp).semantics {contentDescription=if(compositionActive) "退出 AI 构图" else "AI 构图";selected=compositionActive}) {
                    Text("构图",color=if(compositionActive) CameraYellow else TextPrimary,style=MaterialTheme.typography.labelLarge)
                }
            }
        }
      }
      if(appliedPlan!=null && plan!=null) Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
          Text("${if(autoStyle) "自动" else style.label} · ${plan.title}",Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,color=TextSecondary,maxLines=1)
          TextButton(onClick={autoStyle=true;appliedPlan=null;grade=ColorGrade(strength=.6f);onClearPlan()}) {Text("恢复自动")}
      }
      CameraModes(mode,!capturing){timer.cancel();onMode(it)}
      Row(Modifier.fillMaxWidth().padding(horizontal=28.dp,vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
          RecentPhotoButton(photos.latest,!capturing) {
              timer.cancel();autoCapture=false;faceGate.reset();sceneGate.reset();framing.reset()
              if (photos.latest != null) {reviewingSelection=false;panel=CameraPanel.REVIEW}
              else galleryLauncher.launch("image/*")
          }
          CameraShutter(photos.canCapture && camera!=null,capturing) {if(timer.active) timer.cancel() else doManualCapture()}
          IconButton(enabled=!capturing,onClick={
              timer.cancel();clearComposition();subjectAutoArmed=false;subjectOriginalZoom=null;zoomDialExpanded=false
              lensFacing=if(lensFacing==CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
          },modifier=Modifier.size(56.dp)) {Icon(Icons.Filled.Cameraswitch,"切换前后镜头",tint=Color.White,modifier=Modifier.size(30.dp))}
      }
    }
    if(panel==CameraPanel.REVIEW) (if(reviewingSelection) selectedPhoto else photos.latest)?.let {photo->
        PhotoReview(photo,onBrowse={galleryLauncher.launch("image/*")}) {panel=CameraPanel.NONE;selectedPhoto=null;reviewingSelection=false}
    }
    if(posePanel) CameraSheet("姿势灵感",onClose={panel=CameraPanel.NONE}) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            (listOf<PoseCategory?>(null)+PoseCategory.entries).forEach {category->
                FilterChip(selected=poseCategory==category,onClick={poseCategory=category;templateIndex=0;groupZoomSession.reset();framing.reset();sceneGate.reset();progress=0f},label={Text(category?.label ?: "全部")})
            }
        }
        LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            itemsIndexed(poseOptions,key={_,option->option.name}) {index,option->
                Column(Modifier.clip(RoundedCornerShape(CameraDesign.CardRadius)).background(MaterialTheme.colorScheme.surfaceVariant)
                    .semantics{selected=index==templateIndex}.border(if(index==templateIndex) 2.dp else 1.dp,if(index==templateIndex) Accent else CameraDesign.Border,RoundedCornerShape(CameraDesign.CardRadius))
                    .clickable{templateIndex=index;groupZoomSession.reset();framing.reset();sceneGate.reset();progress=0f;showGuide=false;panel=CameraPanel.NONE}.padding(12.dp)) {
                    HumanPoseGuide(option,Modifier.fillMaxWidth().height(172.dp),1f,thumbnail=true)
                    Text(option.name,style=MaterialTheme.typography.labelLarge)
                    Text(option.tip,style=MaterialTheme.typography.bodySmall,color=TextSecondary,maxLines=3)
                    if(option.support.label.isNotBlank()) Text(option.support.label,color=TextSecondary,style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if(filterPanel) CameraSheet("风格",onClose={panel=CameraPanel.NONE;compareOriginal=false}) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            QuickStyleControls(autoStyle,style,grade,mode,filterThumbnail,onNatural={
                autoStyle=true;compareOriginal=false;grade=ColorGrade(strength=.6f)
                style=if(mode==CameraMode.PORTRAIT) PhotoStyle.PORTRAIT else PhotoStyle.SCENIC
                subjectSession=SubjectColor.Session();sceneOptimizer.reset()
            },onPreset={preset,tuning->
                autoStyle=false;compareOriginal=false;style=preset;grade=tuning;framing.reset();sceneGate.reset();faceGate.reset()
            }) {
                GradeControls(style,{autoStyle=false;compareOriginal=false;style=it;Diagnostics.event(Event.STYLE,Field.STYLE to it.ordinal);framing.reset();sceneGate.reset()},grade,{autoStyle=false;compareOriginal=false;grade=it;framing.reset();sceneGate.reset()},filterThumbnail)
            }
            TextButton(onClick={compareOriginal=!compareOriginal}) {Text(if(compareOriginal) "返回风格预览" else "对比原色")}
        }
    }
    if(optionsOpen) CameraSheet("拍摄设置",onClose={panel=CameraPanel.NONE}) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            CameraToggle("拍摄指引",guidanceEnabled){guidanceEnabled=it}
            CameraToggle("稳定后自动拍摄",autoCapture){autoCapture=it;settings.autoCapture=it;groupZoomSession.reset();framing.reset(rearm=true);faceGate.reset();sceneGate.reset();armed=true}
            if(mode==CameraMode.PORTRAIT) CameraToggle("姿势参考图",referenceEnabled){referenceEnabled=it}
            Text("延时拍摄",Modifier.padding(top=12.dp,bottom=8.dp),style=MaterialTheme.typography.titleSmall)
            TimerChoices(timerSeconds){timerSeconds=it}
            CameraToggle("九宫格",gridEnabled){gridEnabled=it}
            if(level.available) CameraToggle("水平辅助",levelEnabled){levelEnabled=it}
            if(modelFailed) TextButton(onClick={poseModelFailed=false;faceModelFailed=false;modelAttempt++}) {Text("重试人物识别")}
            TextButton(onClick={advancedCapture=!advancedCapture}) {Text(if(advancedCapture) "收起精细拍摄选项" else "精细拍摄选项")}
            if(advancedCapture) {
                Text("场景提示",style=MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    mode.intents.forEach {value->FilterChip(selected=intent==value,onClick={
                        intent=value;clearComposition();subjectAutoArmed=false;framing.reset();sceneGate.reset();faceGate.reset();progress=0f
                        if(value==TravelIntent.AURORA){autoCapture=false;autoStyle=false;style=PhotoStyle.ORIGINAL;grade=ColorGrade();flashMode=ImageCapture.FLASH_MODE_OFF;setZoom(1f)}
                    },label={Text(value.label)})}
                }
                Text(intent.advice.substringAfter("："),style=MaterialTheme.typography.bodySmall,color=TextSecondary)
                if(mode==CameraMode.PORTRAIT) {
                    CameraToggle("叠加姿势轮廓",showGuide){showGuide=it}
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {ReferenceStyle.entries.forEach {r->FilterChip(selected=referenceStyle==r,onClick={referenceStyle=r;templateIndex=0;groupZoomSession.reset();framing.reset();sceneGate.reset();progress=0f},label={Text(r.label)})}}
                }
                ExposureControl(camera,onManual={autoExposure=false;zoomSettledAt=SystemClock.elapsedRealtime()+2000})
                TextButton(onClick={autoExposure=true;sceneOptimizer.reset()}) {Text(if(autoExposure) "自动曝光已开启" else "恢复自动曝光")}
                TextButton(onClick={timer.cancel();subjectAutoArmed=false;autoCapture=false;openNativeCamera(context)}) {Text("打开原生相机")}
            }
            HorizontalDivider(Modifier.padding(vertical=12.dp),color=CameraDesign.Border)
            TextButton(onClick={maintenanceOpen=!maintenanceOpen}) {Text(if(maintenanceOpen) "收起更新与诊断" else "更新与诊断")}
            if(maintenanceOpen) {
                Text("映刻 ${com.aipose.camera.BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.bodySmall,color=TextSecondary)
                TextButton(onClick={timer.cancel();panel=CameraPanel.NONE;onOpenScanner()}) {Text("扫一扫更新")}
                TextButton(onClick={scope.launch {Diagnostics.export(context)}}) {Text("导出诊断日志")}
                if(savedResolution.isNotBlank()) Text("最近成片：$savedResolution",style=MaterialTheme.typography.bodySmall,color=TextSecondary)
                if(settings.currentConfig().apiKey.isNotBlank()) {
                    TextButton(enabled=!aiLoading,onClick={requestAiReview()}) {Text(if(aiLoading) "正在获取建议…" else "获取摄影建议")}
                    if(aiReview.isNotBlank()) Text(aiReview)
                }
            }
        }
    }
}

/** RGBA_8888 ImageProxy -> 按传感器方向旋转后的 Bitmap */
private fun ImageProxy.toRotatedBitmap(): Bitmap? = try {
    val raw = toBitmap()
    val rect = cropRect
    val rotated = Bitmap.createBitmap(raw, rect.left, rect.top, rect.width(), rect.height(),
        Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }, true)
    if (rotated !== raw) raw.recycle()
    rotated
} catch (_: Exception) { null }

private data class TapSelectionRequest(val x:Float,val y:Float,val token:Int,val session:Int,val started:Long)
