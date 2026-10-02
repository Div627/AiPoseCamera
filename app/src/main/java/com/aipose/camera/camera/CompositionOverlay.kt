package com.aipose.camera.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aipose.camera.ui.theme.Accent
import com.aipose.camera.ui.theme.Success
import com.aipose.camera.ui.theme.TextPrimary

/** Only real loading and geometry are shown; instruction text has a single owner below. */
@Composable
fun CompositionOverlay(analyzing: Boolean, subject: SubjectFraming.Box?, target: SubjectColor.Point,
                       modifier: Modifier = Modifier) {
    Box(modifier) {
        if (analyzing) CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp), color=TextPrimary, strokeWidth=2.dp)
        else if (subject != null) Canvas(Modifier.fillMaxSize()) {
            val center=Offset(size.width*target.x,size.height*target.y)
            val actual=Offset(subject.cx*size.width,subject.cy*size.height)
            val aligned=kotlin.math.abs(subject.cx-target.x)<.07f && kotlin.math.abs(subject.cy-target.y)<.09f
            val color=if(aligned) Success else Accent
            drawCircle(TextPrimary.copy(alpha=.7f),18.dp.toPx(),center,style=Stroke(1.5.dp.toPx()))
            drawLine(TextPrimary.copy(alpha=.3f),center,actual,1.dp.toPx())
            drawCircle(color,9.dp.toPx(),actual,style=Stroke(2.dp.toPx()))
            drawLine(TextPrimary,center-Offset(6.dp.toPx(),0f),center+Offset(6.dp.toPx(),0f),1.5.dp.toPx(),cap=StrokeCap.Round)
            drawLine(TextPrimary,center-Offset(0f,6.dp.toPx()),center+Offset(0f,6.dp.toPx()),1.5.dp.toPx(),cap=StrokeCap.Round)
        }
    }
}
