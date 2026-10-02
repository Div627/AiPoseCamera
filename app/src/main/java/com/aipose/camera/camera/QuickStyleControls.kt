package com.aipose.camera.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aipose.camera.ui.theme.Accent
import com.aipose.camera.ui.theme.CameraDesign
import com.aipose.camera.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class QuickStyle(val label: String, val preset: PhotoStyle) {
    NATURAL("自然",PhotoStyle.SCENIC), CLEAR("清透",PhotoStyle.F_PROVIA), WARM("暖调",PhotoStyle.K_PORTRA), MONO("黑白",PhotoStyle.F_ACROS)
}

@Composable
fun QuickStyleControls(auto: Boolean, style: PhotoStyle, grade: ColorGrade, mode: CameraMode, thumbnail: ImageBitmap?,
                       onNatural: () -> Unit, onPreset: (PhotoStyle, ColorGrade) -> Unit,
                       advanced: @Composable () -> Unit) {
    var more by remember { mutableStateOf(false) }
    val natural = if (mode == CameraMode.PORTRAIT) PhotoStyle.PORTRAIT else PhotoStyle.SCENIC
    val previews by produceState<Map<QuickStyle,ImageBitmap>>(emptyMap(),thumbnail,natural,auto,style,grade) {
        value=withContext(Dispatchers.Default) {
            thumbnail?.asAndroidBitmap()?.let {source->
                val pixels=IntArray(source.width*source.height)
                source.getPixels(pixels,0,source.width,0,0,source.width,source.height)
                QuickStyle.entries.associateWith {choice->
                    val selected=if(choice==QuickStyle.NATURAL) auto else !auto && style==choice.preset
                    val preset=if(choice==QuickStyle.NATURAL) (if(auto) style else natural) else choice.preset
                    val lut=StyleLut.create(preset,if(selected) grade else ColorGrade(strength=.65f))
                    Bitmap.createBitmap(IntArray(pixels.size){lut.apply(pixels[it])},source.width,source.height,Bitmap.Config.ARGB_8888).asImageBitmap()
                }
            }.orEmpty()
        }
    }
    LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        items(QuickStyle.entries) {choice->
            val selected=if(choice==QuickStyle.NATURAL) auto else !auto && style==choice.preset
            Column(Modifier.width(94.dp).semantics{this.selected=selected}.clickable {
                if(choice==QuickStyle.NATURAL) onNatural() else onPreset(choice.preset,ColorGrade(strength=.65f))
            },horizontalAlignment=Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(12.dp))
                    .border(if(selected) 2.dp else 1.dp,if(selected) Accent else CameraDesign.Border,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
                    previews[choice]?.let{Image(it,choice.label,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
                }
                Text(choice.label,Modifier.padding(vertical=8.dp),color=if(selected) Accent else MaterialTheme.colorScheme.onSurface,style=MaterialTheme.typography.labelLarge)
            }
        }
    }
    Text(if(auto) "自然调色随当前光线自动调整" else "当前风格：${style.label}",style=MaterialTheme.typography.bodySmall,color=TextSecondary)
    TextButton(onClick={more=!more}) {Text(if(more) "收起更多风格" else "更多胶片风格与精细调整")}
    if(more) advanced()
}
