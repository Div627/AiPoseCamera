package com.aipose.camera.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import com.aipose.camera.ui.theme.CameraDesign
import com.aipose.camera.ui.theme.ShutterWhite
import com.aipose.camera.ui.theme.TextPrimary
import com.aipose.camera.ui.theme.BgDark
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

val CameraAccent = com.aipose.camera.ui.theme.Accent

@Composable
fun CameraModes(current:CameraMode, enabled:Boolean, onChange:(CameraMode)->Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.Center) {
        CameraMode.entries.forEach { mode ->
            val tint by animateColorAsState(if(mode==current) CameraAccent else com.aipose.camera.ui.theme.TextSecondary.copy(alpha=.7f),tween(160),label="mode")
            TextButton(onClick={if(mode!=current) onChange(mode)},enabled=enabled,
                modifier=Modifier.widthIn(min=96.dp).height(48.dp).semantics {selected=mode==current}) {
                Text(mode.label,color=tint,style=MaterialTheme.typography.labelLarge, fontWeight=if(mode==current) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal)
            }
        }
    }
}

@Composable
fun CameraShutter(enabled:Boolean,busy:Boolean,onClick:()->Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed && enabled) .94f else 1f,tween(100),label="shutter press")
    Box(Modifier.size(CameraDesign.Shutter).graphicsLayer {scaleX=scale;scaleY=scale}.semantics {contentDescription=if(busy) "正在拍摄" else if(!enabled) "拍照暂不可用" else "拍照";role=Role.Button}
        .clip(CircleShape).clickable(interactionSource=interaction,indication=null,enabled=enabled,onClick=onClick).border(3.dp,ShutterWhite,CircleShape).padding(7.dp)
        .background(ShutterWhite.copy(alpha=if(enabled) 1f else .4f),CircleShape),contentAlignment=Alignment.Center) {
        if(busy) CircularProgressIndicator(Modifier.size(26.dp),color=BgDark,strokeWidth=2.dp)
    }
}

@Composable
fun CameraZoom(value:Float,enabled:Boolean,onMinus:()->Unit,onPlus:()->Unit) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
        TextButton(enabled=enabled,onClick=onMinus,modifier=Modifier.size(48.dp).semantics {contentDescription="缩小"}) {Text("−",color=Color.White)}
        Text("${"%.1f".format(value)}×",color=CameraAccent,style=MaterialTheme.typography.labelLarge)
        TextButton(enabled=enabled,onClick=onPlus,modifier=Modifier.size(48.dp).semantics {contentDescription="放大"}) {Text("+",color=Color.White)}
    }
}

/** A simple viewfinder frame and a small sparkle, drawn at the same weight as camera controls. */
@Composable
fun CompositionIcon(active: Boolean, modifier: Modifier = Modifier.size(26.dp)) {
    androidx.compose.foundation.Canvas(modifier) {
        val color=if(active) CameraAccent else Color.White
        val s=size.width/26f;val stroke=1.7f*s
        fun line(x:Float,y:Float,a:Float,b:Float)=drawLine(color,androidx.compose.ui.geometry.Offset(x*s,y*s),androidx.compose.ui.geometry.Offset(a*s,b*s),stroke,cap=androidx.compose.ui.graphics.StrokeCap.Round)
        line(3f,9f,3f,4f);line(3f,4f,8f,4f);line(18f,4f,23f,4f);line(23f,4f,23f,9f)
        line(3f,17f,3f,22f);line(3f,22f,8f,22f);line(18f,22f,23f,22f);line(23f,22f,23f,17f)
        line(13f,8f,14.5f,11.5f);line(14.5f,11.5f,18f,13f)
        line(18f,13f,14.5f,14.5f);line(14.5f,14.5f,13f,18f)
        line(13f,18f,11.5f,14.5f);line(11.5f,14.5f,8f,13f)
        line(8f,13f,11.5f,11.5f);line(11.5f,11.5f,13f,8f)
        drawCircle(color,1.1f*s,androidx.compose.ui.geometry.Offset(20f*s,10f*s))
    }
}
