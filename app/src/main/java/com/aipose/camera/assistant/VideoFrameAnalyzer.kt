package com.aipose.camera.assistant

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/** Sequential bounded sampling. Both ML models are bundled and inference stays on device. */
object VideoFrameAnalyzer {
    suspend fun analyze(source: File, store: VideoStudyStore, progress: (Float) -> Unit): VideoStudy = withContext(Dispatchers.IO) {
        val reader = MediaMetadataRetriever()
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(.65f).build())
        val faces = FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).build())
        val generated = mutableListOf<File>()
        var saved = false
        try {
            reader.setDataSource(source.absolutePath)
            val duration = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            require(duration in 500..600000) {"Video length outside supported range"}
            val rawWidth = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawHeight = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation=reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val width=if(rotation%180!=0) rawHeight else rawWidth
            val height=if(rotation%180!=0) rawWidth else rawHeight
            require(width > 0 && height > 0)
            val count = (duration / 350).toInt().coerceIn(2, 90)
            val found = mutableListOf<FrameCandidate>()
            var models = true
            for(i in 0 until count) {
                currentCoroutineContext().ensureActive()
                val time = i * (duration - 100) / (count - 1)
                val bitmap = VideoFrames.read(reader,time, minOf(640,maxOf(width,height)))
                if(bitmap != null) try {
                    val small = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
                    val pixels = IntArray(96 * 96); small.getPixels(pixels, 0, 96, 0, 0, 96, 96); small.recycle()
                    val quality = FrameSelection.quality(pixels, 96, 96)
                    var labels = emptyList<String>(); var people = 0; var eyes: Float? = null
                    var x = .5f; var y = .5f
                    try {
                        val image = InputImage.fromBitmap(bitmap, 0)
                        labels = Tasks.await(labeler.process(image)).sortedByDescending {it.confidence}.take(4).map {it.text}
                        val detected = if(quality.score >= .45f) Tasks.await(faces.process(image)) else emptyList()
                        people = detected.size
                        detected.maxByOrNull {it.boundingBox.width() * it.boundingBox.height()}?.let {face ->
                            x = (face.boundingBox.exactCenterX() / bitmap.width).coerceIn(0f, 1f)
                            y = (face.boundingBox.exactCenterY() / bitmap.height).coerceIn(0f, 1f)
                            val open = listOfNotNull(face.leftEyeOpenProbability, face.rightEyeOpenProbability)
                            if(open.size == 2) eyes = open.minOrNull()
                        }
                    } catch(_: Exception) {models = false}
                    currentCoroutineContext().ensureActive()
                    val portraitBonus = if(people > 0) .06f else 0f
                    val eyePenalty = if(eyes != null && eyes!! < .35f) .25f else 0f
                    found += FrameCandidate(time, (quality.score + portraitBonus - eyePenalty).coerceIn(0f, 1f),
                        quality.signature, quality.brightness, labels, people, eyes, x, y)
                } finally {bitmap.recycle()}
                progress((i + 1f) / count * .9f)
            }
            require(found.isNotEmpty())
            val candidates = FrameSelection.photoCandidates(found).map {frame ->
                currentCoroutineContext().ensureActive()
                val bitmap = VideoFrames.read(reader,frame.timeMs,minOf(640,maxOf(width,height)))
                    ?: error("Frame unavailable")
                val file = File(store.directory, "frame-${newId()}.jpg"); generated += file
                try {file.outputStream().use {check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it))}} finally {bitmap.recycle()}
                frame.copy(preview = file.absolutePath)
            }
            val result = VideoStudy(source.absolutePath, duration, width, height, found, candidates, models)
            currentCoroutineContext().ensureActive()
            store.save(result); saved = true; progress(1f)
            result
        } finally {
            reader.release(); labeler.close(); faces.close()
            if(!saved) generated.forEach {it.delete()}
        }
    }
}
