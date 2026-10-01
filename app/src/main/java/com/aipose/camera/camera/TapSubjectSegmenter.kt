package com.aipose.camera.camera

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.interactivesegmenter.InteractiveSegmenter
import java.nio.ByteOrder

/** Lazy, on-device inference. Calls and close are confined to a single background worker. */
class TapSubjectSegmenter(private val context: Context) : AutoCloseable {
    private var task: InteractiveSegmenter? = null
    fun select(bitmap: Bitmap, x: Float, y: Float): TapSubjectMask.Selection? {
        val segmenter=task ?: InteractiveSegmenter.createFromOptions(context,
            InteractiveSegmenter.InteractiveSegmenterOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("magic_touch.tflite").build())
                .setOutputConfidenceMasks(true).setOutputCategoryMask(false).build()).also { task=it }
        val image=BitmapImageBuilder(bitmap).build()
        try {
            val result=segmenter.segment(image,InteractiveSegmenter.RegionOfInterest.create(NormalizedKeypoint.create(x,y)))
            val masks=result.confidenceMasks().orElse(emptyList())
            try {
                val foreground=masks.getOrNull(1) ?: return null
                val buffer=ByteBufferExtractor.extract(foreground).order(ByteOrder.nativeOrder()).asFloatBuffer()
                val confidence=FloatArray(foreground.width*foreground.height);buffer.get(confidence)
                return TapSubjectMask.select(confidence,foreground.width,foreground.height,x,y)
            } finally { masks.forEach { it.close() } }
        } finally { image.close() }
    }
    override fun close() {task?.close();task=null}
}
