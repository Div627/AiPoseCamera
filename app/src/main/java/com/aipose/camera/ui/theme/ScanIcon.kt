package com.aipose.camera.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** Lightweight scan frame, shared by the camera entry and scanner. */
@Composable
fun ScanIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier.size(24.dp)) {
        val unit=size.width/24f
        fun line(x:Float,y:Float,a:Float,b:Float)=drawLine(color,Offset(x*unit,y*unit),Offset(a*unit,b*unit),1.65f*unit,StrokeCap.Round)
        line(3f,8f,3f,4f);line(3f,4f,8f,4f)
        line(16f,4f,21f,4f);line(21f,4f,21f,8f)
        line(3f,16f,3f,20f);line(3f,20f,8f,20f)
        line(16f,20f,21f,20f);line(21f,20f,21f,16f)
        line(7f,12f,17f,12f)
    }
}
