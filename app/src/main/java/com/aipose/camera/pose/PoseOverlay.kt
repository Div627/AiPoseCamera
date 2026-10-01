package com.aipose.camera.pose

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.aipose.camera.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Two 1152x768 RGBA atlases, ~6.75 MiB; decoding and waiting both stay off the UI thread. */
private object AtlasCache {
    private val ids=listOf(R.drawable.pose_natural_0,R.drawable.pose_natural_1,R.drawable.pose_natural_2,R.drawable.pose_natural_3,R.drawable.pose_outline_0,R.drawable.pose_outline_1,R.drawable.pose_outline_2,R.drawable.pose_outline_3)
    private val mutex=Mutex()
    private val cache=LinkedHashMap<Int,ImageBitmap>(4,.75f,true)
    suspend fun load(resources:Resources,index:Int):ImageBitmap = withContext(Dispatchers.IO) {
        mutex.withLock {
            cache[index] ?: BitmapFactory.decodeResource(resources,ids[index]).asImageBitmap().also {
                cache[index]=it
                if(cache.size>2) cache.remove(cache.keys.first())
            }
        }
    }
}

/** Original AI illustrations. Atlas pixel anchors and rendering share the identical transform. */
@Composable
fun HumanPoseGuide(option:GroupPoses.Option,modifier:Modifier=Modifier,alpha:Float=.88f,thumbnail:Boolean=false,matched:Boolean=false) {
    val resources=LocalContext.current.resources
    val editorial=if(thumbnail && option.slots.size==1) EditorialPhotos.ids[option.slots.first().pose] else null
    if(editorial!=null) {
        val photo by produceState<ImageBitmap?>(null,editorial) {value=EditorialPhotos.load(resources,editorial)}
        photo?.let{androidx.compose.foundation.Image(it,option.name,modifier,contentScale=androidx.compose.ui.layout.ContentScale.Fit)}
        return
    }
    val sheets=remember(option,thumbnail) {option.slots.map{it.pose/6+if(thumbnail) 0 else 4}.distinct()}
    val atlases by produceState<Map<Int,ImageBitmap>>(emptyMap(),resources,sheets) {
        value=sheets.associateWith{AtlasCache.load(resources,it)}
    }
    Canvas(modifier) {
        for(slot in option.slots) {
            val atlas=atlases[slot.pose/6+if(thumbnail) 0 else 4] ?: continue
            val cell=slot.pose%6;val cellW=atlas.width/3;val cellH=atlas.height/2
            val side=PoseAtlas.sizeFactor*minOf(size.height,size.width/.75f)*slot.scale
            val left=size.width*slot.x-PoseAtlas.centers[slot.pose]*side
            val top=size.height*slot.y-PoseAtlas.centerY*side
            withTransform({if(slot.mirror) scale(-1f,1f,pivot=Offset(size.width*slot.x,size.height*slot.y))}) {
            if(!thumbnail) drawImage(atlas,IntOffset(cell%3*cellW,cell/3*cellH),IntSize(cellW,cellH),
                IntOffset(left.toInt()+2,top.toInt()+2),IntSize(side.toInt().coerceAtLeast(1),side.toInt().coerceAtLeast(1)),alpha=.65f,colorFilter=ColorFilter.tint(Color.Black))
            drawImage(atlas,IntOffset(cell%3*cellW,cell/3*cellH),IntSize(cellW,cellH),
                IntOffset(left.toInt(),top.toInt()),IntSize(side.toInt().coerceAtLeast(1),side.toInt().coerceAtLeast(1)),alpha=alpha,colorFilter=if(thumbnail) null else ColorFilter.tint(if(matched) Color(0xFF75E6B4) else Color.White))
            }
        }
    }
}

@Composable
fun PoseSilhouetteIcon(template:PoseTemplate,modifier:Modifier=Modifier) {
    val i=PoseTemplate.ALL.indexOfFirst {it.id==template.id}.coerceAtLeast(0)
    HumanPoseGuide(GroupPoses.options(1).first{it.slots.first().pose==i},modifier,1f,thumbnail=true)
}

/** Small full photographic thumbnails; separate from the transparent viewfinder atlases. */
private object EditorialPhotos {
    val ids=mapOf(0 to R.drawable.pose_editorial_0,1 to R.drawable.pose_editorial_1,6 to R.drawable.pose_editorial_6,7 to R.drawable.pose_editorial_7,12 to R.drawable.pose_editorial_12,16 to R.drawable.pose_editorial_16)
    private val cache=LinkedHashMap<Int,ImageBitmap>(4,.75f,true)
    private val mutex=Mutex()
    suspend fun load(resources:Resources,id:Int):ImageBitmap=withContext(Dispatchers.IO) {
        mutex.withLock {cache[id] ?: BitmapFactory.decodeResource(resources,id).asImageBitmap().also{cache[id]=it;if(cache.size>4) cache.remove(cache.keys.first())}}
    }
}
