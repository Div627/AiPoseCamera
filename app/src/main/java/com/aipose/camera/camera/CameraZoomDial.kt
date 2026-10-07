package com.aipose.camera.camera

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.*

private fun zoomLabel(v:Float)=if(abs(v-v.roundToInt())<.04f) v.roundToInt().toString() else String.format(Locale.US,"%.1f",v)

@Composable
fun CameraZoomDial(value:Float,min:Float,max:Float,enabled:Boolean,expanded:Boolean,onExpanded:(Boolean)->Unit,onZoom:(Float)->Unit) {
    val shortcuts=remember(min,max){ZoomScale.quickPresets(min,max)}
    val visibleShortcuts = if (shortcuts.any {abs(value-it)<.07f}) shortcuts
        else (shortcuts.take(2) + value).distinct().sorted()
    val currentValue by rememberUpdatedState(value)
    val currentZoom by rememberUpdatedState(onZoom)
    val currentExpand by rememberUpdatedState(onExpanded)
    var dragPosition by remember {mutableFloatStateOf(0f)}
    val gesture=Modifier.pointerInput(min,max,enabled) {
        if(enabled && max>min) detectDragGestures(
            onDragStart={dragPosition=ZoomScale.position(currentValue,min,max);currentExpand(true)},
            onDrag={change,amount->
                change.consume();dragPosition=(dragPosition+amount.x/size.width*.9f).coerceIn(0f,1f)
                currentZoom(ZoomScale.ratio(dragPosition,min,max))
            })
    }
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
        if(expanded) {
            Canvas(Modifier.fillMaxWidth().height(132.dp).then(gesture)
                .semantics {
                    contentDescription="变焦刻度盘"
                    progressBarRangeInfo=ProgressBarRangeInfo(value.coerceIn(min,max),min..max)
                    setProgress {v->if(enabled) {onZoom(v.coerceIn(min,max));true} else false}
                }) {
                val radius=size.width*.62f
                // Keep the top of the arc and its fixed indicator inside the visible canvas.
                val center=Offset(size.width/2,radius+12.dp.toPx())
                drawCircle(Color.Black.copy(alpha=.5f),radius,center)
                val position=ZoomScale.position(value,min,max)
                val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=android.graphics.Color.WHITE;textSize=13.dp.toPx();textAlign=Paint.Align.CENTER}
                for(i in -30..30) {
                    val tickPosition=position+i/70f
                    if(tickPosition !in 0f..1f) continue
                    val angle=(-90+i*3.1f)*PI.toFloat()/180
                    val major=i%5==0;val length=if(major) 19.dp.toPx() else 10.dp.toPx()
                    val direction=Offset(cos(angle),sin(angle))
                    drawLine(Color.White.copy(alpha=if(major) .9f else .5f),center+direction*radius,center+direction*(radius-length),1.4.dp.toPx())
                }
                for(preset in ZoomScale.presets(min,max)) {
                    val delta=(ZoomScale.position(preset,min,max)-position)*70
                    if(abs(delta)>29) continue
                    val angle=(-90+delta*3.1f)*PI.toFloat()/180
                    val labelPoint=center+Offset(cos(angle),sin(angle))*(radius-37.dp.toPx())
                    drawContext.canvas.nativeCanvas.drawText(zoomLabel(preset),labelPoint.x,labelPoint.y+4.dp.toPx(),paint)
                }
                drawLine(CameraAccent,Offset(center.x,center.y-radius-3.dp.toPx()),Offset(center.x,center.y-radius+23.dp.toPx()),3.dp.toPx(),cap=androidx.compose.ui.graphics.StrokeCap.Round)
            }
            TextButton(onClick={onExpanded(false)},modifier=Modifier.heightIn(min=48.dp)) {Text("${zoomLabel(value)}×  ·  收起",color=CameraAccent)}
        }
        Row(Modifier.fillMaxWidth().then(gesture),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
            visibleShortcuts.forEach {preset->
                val selected=abs(value-preset)<.07f
                TextButton(enabled=enabled,onClick={if(selected) onExpanded(!expanded) else onZoom(preset)},
                    modifier=Modifier.size(48.dp).background(if(selected) Color.White.copy(alpha=.14f) else Color.Transparent,CircleShape)
                        .semantics {contentDescription="变焦 ${zoomLabel(preset)} 倍";this.selected=selected}) {
                    Text(zoomLabel(preset),color=if(selected) CameraAccent else Color.White,style=MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
