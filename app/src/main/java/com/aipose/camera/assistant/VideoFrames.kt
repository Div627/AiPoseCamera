package com.aipose.camera.assistant

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import kotlin.math.roundToInt

object VideoFrames {
    fun read(reader: MediaMetadataRetriever, timeMs: Long, maxSide: Int): Bitmap? {
        if(Build.VERSION.SDK_INT >= 27) return reader.getScaledFrameAtTime(timeMs * 1000,
            MediaMetadataRetriever.OPTION_CLOSEST, maxSide, maxSide)
        val bitmap = reader.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
        if(scale == 1f) return bitmap
        return try {Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1), true)} finally {bitmap.recycle()}
    }
}
