package com.aipose.camera.camera

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable fun GradeControls(style:PhotoStyle,onStyle:(PhotoStyle)->Unit,grade:ColorGrade,onGrade:(ColorGrade)->Unit,thumbnail:ImageBitmap?=null) {
    var advanced by remember {mutableStateOf(false)}
    var family by remember {mutableStateOf(if(style.family==StyleFamily.NATURAL) StyleFamily.FUJI else style.family)}
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        StyleFamily.entries.forEach {f->FilterChip(selected=family==f,onClick={family=f},label={Text(f.label)})}
    }
    val options=remember(family) {listOf(PhotoStyle.ORIGINAL)+PhotoStyle.entries.filter {it.family==family && it!=PhotoStyle.ORIGINAL}}
    val previews by produceState<Map<PhotoStyle,ImageBitmap>>(emptyMap(),thumbnail,options,grade) {
        value=withContext(Dispatchers.Default) {
            thumbnail?.asAndroidBitmap()?.let {source->
                val pixels=IntArray(source.width*source.height);source.getPixels(pixels,0,source.width,0,0,source.width,source.height)
                options.associateWith {p->
                    val lut=StyleLut.create(p,grade.copy(strength=1f))
                    Bitmap.createBitmap(IntArray(pixels.size){lut.apply(pixels[it])},source.width,source.height,Bitmap.Config.ARGB_8888).asImageBitmap()
                }
            }.orEmpty()
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        options.forEach {p->Column(Modifier.width(126.dp).clip(RoundedCornerShape(14.dp))
            .border(if(style==p) 2.dp else 1.dp,if(style==p) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,RoundedCornerShape(14.dp))
            .clickable{onStyle(p);onGrade(grade.copy(strength=1f))}.padding(7.dp)) {
            previews[p]?.let {Image(it,contentDescription="${p.label}预览",modifier=Modifier.fillMaxWidth().height(114.dp).clip(RoundedCornerShape(9.dp)),contentScale=ContentScale.Crop)}
            Text(p.label,style=MaterialTheme.typography.labelLarge,modifier=Modifier.padding(top=8.dp))
            Text(p.description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
    }
    GradeSlider("预设强度",grade.strength,0f..1f){onGrade(grade.copy(strength=it))}
    TextButton(onClick={advanced=!advanced}) {Text(if(advanced) "收起精细调整" else "精细调整")}
    if(advanced) {
    GradeSlider("后期曝光（非相机EV）",grade.postEv,-2f..2f){onGrade(grade.copy(postEv=it))}
    GradeSlider("对比",grade.contrast,.5f..1.5f){onGrade(grade.copy(contrast=it))}
    GradeSlider("饱和",grade.saturation,0f..2f){onGrade(grade.copy(saturation=it))}
    GradeSlider("暖冷",grade.warmth,-1f..1f){onGrade(grade.copy(warmth=it))}
    GradeSlider("色调",grade.tint,-1f..1f){onGrade(grade.copy(tint=it))}
    GradeSlider("高光",grade.highlights,-1f..1f){onGrade(grade.copy(highlights=it))}
    GradeSlider("阴影",grade.shadows,-1f..1f){onGrade(grade.copy(shadows=it))}
    }
    Text("相机与胶片色彩近似，效果随光线变化。保留原片与同尺寸风格照片。",style=MaterialTheme.typography.bodySmall)
    TextButton(onClick={onStyle(PhotoStyle.ORIGINAL);onGrade(ColorGrade())}) {Text("恢复原色")}
}
@Composable private fun GradeSlider(label:String,value:Float,range:ClosedFloatingPointRange<Float>,onChange:(Float)->Unit) {
    Text("$label  ${"%.2f".format(value)}",style=MaterialTheme.typography.bodySmall)
    Slider(value=value,onValueChange=onChange,valueRange=range,modifier=Modifier.semantics{contentDescription=label})
}
