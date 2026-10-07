package com.aipose.camera.camera

import android.content.Context
import android.content.Intent
import android.hardware.*
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*

class CaptureTimer(private val scope:CoroutineScope,private val owner:LifecycleOwner) {
    var seconds by mutableStateOf(0);private set
    var active by mutableStateOf(false);private set
    private var job:Job?=null
    fun cancel(){job?.cancel();job=null;active=false;seconds=0}
    fun start(delaySeconds:Int,action:()->Unit) {
        cancel()
        if(!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if(delaySeconds==0){action();return}
        active=true
        job=scope.launch {
            val gate=CountdownGate();gate.start(delaySeconds,SystemClock.elapsedRealtime())
            while(isActive) {
                val now=SystemClock.elapsedRealtime();seconds=gate.remaining(now)
                if(gate.consume(now)) {active=false;seconds=0;if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) action();break}
                delay(100)
            }
        }
    }
}
@Composable fun rememberCaptureTimer():CaptureTimer {
    val owner=LocalLifecycleOwner.current;val scope=rememberCoroutineScope();val timer=remember(owner){CaptureTimer(scope,owner)}
    DisposableEffect(owner,timer) {
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP) timer.cancel()}
        owner.lifecycle.addObserver(observer)
        onDispose {timer.cancel();owner.lifecycle.removeObserver(observer)}
    }
    return timer
}

data class LevelReading(val available:Boolean,val roll:Float?)
@Composable fun rememberLevel(enabled:Boolean):LevelReading {
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    val manager=remember{context.getSystemService(Context.SENSOR_SERVICE) as SensorManager}
    val sensor=remember{manager.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)}
    var roll by remember{mutableStateOf<Float?>(null)}
    DisposableEffect(enabled,owner) {
        var x=0f;var y=0f;var registered=false
        val listener=object:SensorEventListener {
            override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
            override fun onSensorChanged(event:SensorEvent) {
                x=.8f*x+.2f*event.values[0];y=.8f*y+.2f*event.values[1];roll=TravelGuidance.roll(x,y)
            }
        }
        fun register(){if(enabled && sensor!=null && !registered){registered=manager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_UI)}}
        val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_RESUME->register();Lifecycle.Event.ON_PAUSE->{manager.unregisterListener(listener);registered=false;roll=null};else->{}}}
        owner.lifecycle.addObserver(observer);if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) register()
        onDispose {manager.unregisterListener(listener);owner.lifecycle.removeObserver(observer);roll=null}
    }
    return LevelReading(sensor!=null,if(enabled) roll else null)
}
@Composable fun FramingGuides(grid:Boolean,level:LevelReading,modifier:Modifier=Modifier) {
    Canvas(modifier) {
        if(grid) for(i in 1..2) {
            drawLine(Color.White.copy(alpha=.22f),Offset(size.width*i/3,0f),Offset(size.width*i/3,size.height),1f)
            drawLine(Color.White.copy(alpha=.22f),Offset(0f,size.height*i/3),Offset(size.width,size.height*i/3),1f)
        }
        level.roll?.let {roll->
            val center=Offset(size.width/2,size.height*.48f);val half=48.dp.toPx();val angle=(-roll).coerceIn(-45f,45f)*Math.PI/180
            val delta=Offset((half*kotlin.math.cos(angle)).toFloat(),(half*kotlin.math.sin(angle)).toFloat())
            drawLine(Color.White.copy(alpha=.5f),center-Offset(half,0f),center+Offset(half,0f),2.dp.toPx())
            drawLine(if(kotlin.math.abs(roll)<2f) CameraAccent else Color.White,center-delta,center+delta,3.dp.toPx())
        }
    }
}
fun openNativeCamera(context:Context) {
    try {context.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))}
    catch(e:Exception){Toast.makeText(context,"无法打开原生相机，请从桌面打开",Toast.LENGTH_LONG).show()}
}
@Composable fun TimerChoices(seconds:Int,onChange:(Int)->Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {listOf(0,2,5,10).forEach {n->FilterChip(selected=seconds==n,onClick={onChange(n)},label={Text(if(n==0) "无延时" else "${n}秒")})}}
}
@Composable fun ExposureControl(camera:Camera?,onManual:()->Unit={}) {
    val context=LocalContext.current;var value by remember(camera){mutableStateOf(camera?.cameraInfo?.exposureState?.exposureCompensationIndex ?: 0)}
    var pending by remember(camera){mutableStateOf(false)};var error by remember(camera){mutableStateOf("")}
    val state=camera?.cameraInfo?.exposureState
    Text("相机曝光补偿",style=MaterialTheme.typography.titleSmall)
    if(state?.isExposureCompensationSupported!=true || state.exposureCompensationRange.lower>=state.exposureCompensationRange.upper) Text("当前镜头不支持曝光补偿",style=MaterialTheme.typography.bodySmall)
    else {
        Text("${"%.1f".format(value*state.exposureCompensationStep.toFloat())} EV")
        Slider(value=value.toFloat(),onValueChange={value=it.toInt()},valueRange=state.exposureCompensationRange.lower.toFloat()..state.exposureCompensationRange.upper.toFloat(),enabled=!pending,
            modifier=Modifier.semantics {contentDescription="相机曝光补偿"},onValueChangeFinished={
                onManual();pending=true;val future=camera!!.cameraControl.setExposureCompensationIndex(value)
                future.addListener({pending=false;runCatching{future.get()}.onFailure{value=state.exposureCompensationIndex;error="曝光调整失败，请重试"}},androidx.core.content.ContextCompat.getMainExecutor(context))
            })
    }
    if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
}
