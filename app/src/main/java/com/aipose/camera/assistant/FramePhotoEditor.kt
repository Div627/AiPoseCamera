package com.aipose.camera.assistant

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.aipose.camera.camera.ColorGrade
import com.aipose.camera.camera.PhotoStyle
import com.aipose.camera.camera.StyleLut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class FrameEdit(val style: PhotoStyle = PhotoStyle.SCENIC, val strength: Float = .6f,
    val ratio: Float? = null, val x: Float = .5f, val y: Float = .5f)
object FramePhotoEditor {
    suspend fun render(study: VideoStudy, frame: FrameCandidate, edit: FrameEdit, maxSide: Int): Bitmap = withContext(Dispatchers.Default) {
        val reader = MediaMetadataRetriever()
        val bitmap = try {
            reader.setDataSource(study.source)
            VideoFrames.read(reader,frame.timeMs,minOf(maxSide,maxOf(study.width,study.height),
                (maxOf(study.width,study.height)*minOf(1.0,kotlin.math.sqrt(8_500_000.0/(study.width.toDouble()*study.height)))).toInt()))
                ?: error("Frame unavailable")
        } finally {reader.release()}
        try {
            currentCoroutineContext().ensureActive()
            val ratio = edit.ratio
            val width = if(ratio == null) bitmap.width else minOf(bitmap.width, (bitmap.height * ratio).roundToInt()).coerceAtLeast(1)
            val height = if(ratio == null) bitmap.height else minOf(bitmap.height, (bitmap.width / ratio).roundToInt()).coerceAtLeast(1)
            val left = ((bitmap.width - width) * edit.x.coerceIn(0f, 1f)).roundToInt()
            val top = ((bitmap.height - height) * edit.y.coerceIn(0f, 1f)).roundToInt()
            val pixels = IntArray(width * height); bitmap.getPixels(pixels, 0, width, left, top, width, height)
            bitmap.recycle()
            val lut = StyleLut.create(edit.style, ColorGrade(strength = edit.strength))
            for(i in pixels.indices) {
                if(i % 65536 == 0) currentCoroutineContext().ensureActive()
                pixels[i] = lut.apply(pixels[i])
            }
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        } finally {bitmap.recycle()}
    }
    suspend fun save(context: Context, study: VideoStudy, frame: FrameCandidate, edit: FrameEdit): Pair<Uri, String> {
        val bitmap = render(study, frame, edit, minOf(4096, maxOf(study.width, study.height)))
        try {return withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "yingke_frame_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if(Build.VERSION.SDK_INT >= 29) {put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AiPoseCamera"); put(MediaStore.Images.Media.IS_PENDING, 1)}
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("No gallery entry")
            try {
                context.contentResolver.openOutputStream(uri)?.use {check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it))} ?: error("No output")
                currentCoroutineContext().ensureActive()
                if(Build.VERSION.SDK_INT >= 29) context.contentResolver.update(uri, ContentValues().apply {put(MediaStore.Images.Media.IS_PENDING, 0)}, null, null)
                uri to "${bitmap.width} × ${bitmap.height}"
            } catch(e: Exception) {context.contentResolver.delete(uri, null, null); throw e}
        }} finally {bitmap.recycle()}
    }
}
