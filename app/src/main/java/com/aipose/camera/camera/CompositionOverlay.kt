package com.aipose.camera.camera

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun CompositionOverlay(analyzing:Boolean, subject:SubjectFraming.Box?, modifier:Modifier=Modifier) {
    val transition=rememberInfiniteTransition(label="composition")
    val phase by transition.animateFloat(0f,1f,infiniteRepeatable(tween(2200,easing=LinearEasing)),label="scan")
    val colors=listOf(Color(0xFF91E9E5),Color(0xFFCAB8EF),Color(0xFFFFD1B3),Color(0xFF91E9E5))
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            if(analyzing) {
                // Sparse, soft light points leave the photograph visible.
                for(row in 1..4) for(col in 1..3) {
                    val alpha=(.12f+.25f*(1f-kotlin.math.abs(phase-(row-1)/4f))).coerceIn(0f,1f)
                    drawCircle(Color.White.copy(alpha=alpha),2.dp.toPx(),Offset(size.width*col/4,size.height*row/5))
                }
                val center=Offset(size.width/2,size.height*.42f)
                drawArc(Brush.sweepGradient(colors,center),phase*360,260f,false,center-Offset(22.dp.toPx(),22.dp.toPx()),androidx.compose.ui.geometry.Size(44.dp.toPx(),44.dp.toPx()),style=Stroke(2.dp.toPx()))
            } else if(subject!=null) {
                val center=Offset(size.width/2,size.height/2)
                val actual=Offset(subject.cx*size.width,subject.cy*size.height)
                val aligned=kotlin.math.abs(subject.cx-.5f)<.09f && kotlin.math.abs(subject.cy-.5f)<.09f
                drawCircle(if(aligned) Color(0xFF91E9E5) else Color.White.copy(alpha=.6f),21.dp.toPx(),center,style=Stroke(2.dp.toPx()))
                drawLine(Color.White.copy(alpha=.25f),center,actual,1.dp.toPx())
                drawCircle(Brush.sweepGradient(colors,actual),12.dp.toPx(),actual,style=Stroke(3.dp.toPx()))
                drawLine(Color.White,center-Offset(7.dp.toPx(),0f),center+Offset(7.dp.toPx(),0f),2.dp.toPx())
                drawLine(Color.White,center-Offset(0f,7.dp.toPx()),center+Offset(0f,7.dp.toPx()),2.dp.toPx())
            }
        }
        if(analyzing || subject!=null) Text(
            if(analyzing) "正在识别主体 · 请稍稳住" else "缓慢转动手机，让彩环靠近中央准星",
            color=Color.White,style=MaterialTheme.typography.labelMedium,
            modifier=Modifier.align(Alignment.TopCenter).padding(16.dp).background(Color.Black.copy(alpha=.52f),RoundedCornerShape(24.dp)).padding(horizontal=12.dp,vertical=6.dp))
    }
}
