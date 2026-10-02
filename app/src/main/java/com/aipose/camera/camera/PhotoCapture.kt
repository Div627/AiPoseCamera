package com.aipose.camera.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.aipose.camera.diagnostics.Diagnostics
import com.aipose.camera.diagnostics.DiagnosticStore.Event
import com.aipose.camera.diagnostics.DiagnosticStore.Field
import java.io.File
import java.util.concurrent.Executors

/** Release the shutter once the original is durable; process bounded edits on one worker. */
object PhotoCapture {
    data class Result(val original: Uri? = null, val edited: Uri? = null, val width: Int = 0, val height: Int = 0, val error: String? = null)
    private val worker = Executors.newSingleThreadExecutor()

    fun take(context: Context, capture: ImageCapture, style: PhotoStyle, grade: ColorGrade,
             onOriginalSaved: (Uri) -> Unit, done: (Result) -> Unit) {
        val app = context.applicationContext
        val main = ContextCompat.getMainExecutor(app)
        val stem = "yingke_${System.currentTimeMillis()}"
        fun values(suffix: String) = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${stem}_${suffix}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AiPoseCamera")
        }
        try {
            capture.takePicture(ImageCapture.OutputFileOptions.Builder(app.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values("原片")).build(), main,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Diagnostics.error(Event.CAPTURE_ERROR, exc)
                        done(Result(error = "拍摄失败，请重试"))
                    }
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val original = output.savedUri ?: run { done(Result(error = "照片已保存，但暂时无法查看")); return }
                        onOriginalSaved(original)
                        worker.execute {
                            var bitmap: Bitmap? = null
                            var temp: File? = null
                            var edited: Uri? = null
                            var width = 0
                            var height = 0
                            var failure: String? = null
                            try {
                                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                app.contentResolver.openInputStream(original)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                                width = bounds.outWidth.coerceAtLeast(0); height = bounds.outHeight.coerceAtLeast(0)
                                Diagnostics.event(Event.CAPTURE, Field.WIDTH to width, Field.HEIGHT to height, Field.STYLE to style.ordinal)
                                if (grade.needsCopy(style)) {
                                    bitmap = app.contentResolver.openInputStream(original)?.use {
                                        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                                            inMutable = true; inPreferredConfig = Bitmap.Config.ARGB_8888
                                        })
                                    } ?: error("Missing original")
                                    val image = bitmap!!
                                    val lut = StyleLut.create(style, grade)
                                    val rows = 32
                                    val pixels = IntArray(image.width * rows)
                                    for (y in 0 until image.height step rows) {
                                        val h = minOf(rows, image.height - y)
                                        image.getPixels(pixels, 0, image.width, 0, y, image.width, h)
                                        for (i in 0 until image.width * h) pixels[i] = lut.apply(pixels[i])
                                        image.setPixels(pixels, 0, image.width, 0, y, image.width, h)
                                    }
                                    temp = File.createTempFile("yingke-edited-", ".jpg", app.cacheDir)
                                    temp!!.outputStream().use { check(image.compress(Bitmap.CompressFormat.JPEG, 98, it)) }
                                    copyExif(app, original, temp!!)
                                    val metadata = values("成片")
                                    if (Build.VERSION.SDK_INT >= 29) metadata.put(MediaStore.Images.Media.IS_PENDING, 1)
                                    edited = EditedCopyTransaction.publish(original,
                                        create = { app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, metadata) ?: error("Insert failed") },
                                        write = { uri -> app.contentResolver.openOutputStream(uri)?.use { out -> temp!!.inputStream().use { it.copyTo(out) } } ?: error("Write failed") },
                                        commit = { uri -> if (Build.VERSION.SDK_INT >= 29) app.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null) },
                                        delete = { uri -> app.contentResolver.delete(uri, null, null) })
                                }
                            } catch (e: Exception) {
                                Diagnostics.error(Event.CAPTURE_ERROR, e)
                                failure = "原片已保存，成片处理失败，可查看原片"
                            } catch (e: OutOfMemoryError) {
                                Diagnostics.error(Event.CAPTURE_ERROR, e)
                                failure = "原片已保存，内存不足，可查看原片"
                            } finally {
                                bitmap?.recycle(); temp?.delete()
                                main.execute { done(Result(original, edited, width, height, failure)) }
                            }
                        }
                    }
                })
        } catch (e: Exception) {
            Diagnostics.error(Event.CAPTURE_ERROR, e)
            done(Result(error = "拍摄失败，请重试"))
        }
    }

    private fun copyExif(context: Context, original: Uri, target: File) {
        val destination = ExifInterface(target)
        context.contentResolver.openInputStream(original)?.use { stream ->
            val source = ExifInterface(stream)
            for (tag in listOf(ExifInterface.TAG_ORIENTATION, ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_FOCAL_LENGTH))
                source.getAttribute(tag)?.let { destination.setAttribute(tag, it) }
        }
        destination.saveAttributes()
    }
}
