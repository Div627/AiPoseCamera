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

/** Save the original first. A processing error can only remove the unfinished edited copy. */
object PhotoCapture {
    private val worker=Executors.newSingleThreadExecutor()
    fun take(context:Context,capture:ImageCapture,style:PhotoStyle=PhotoStyle.ORIGINAL,grade:ColorGrade=ColorGrade(),onSavedSize:((Int,Int)->Unit)?=null,done:(String?)->Unit) {
        val app=context.applicationContext;val main=ContextCompat.getMainExecutor(app)
        val stem="ziying_${System.currentTimeMillis()}"
        fun values(suffix:String)=ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"${stem}_${suffix}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
            if(Build.VERSION.SDK_INT>=29) put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/AiPoseCamera")
        }
        try {
            capture.takePicture(ImageCapture.OutputFileOptions.Builder(app.contentResolver,MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values("原片")).build(),main,
                object:ImageCapture.OnImageSavedCallback {
                    override fun onError(exc:ImageCaptureException) {com.aipose.camera.diagnostics.Diagnostics.error(com.aipose.camera.diagnostics.DiagnosticStore.Event.CAPTURE_ERROR,exc);done("拍摄失败：${exc.message}")}
                    override fun onImageSaved(output:ImageCapture.OutputFileResults) {
                        output.savedUri?.let {uri->
                            runCatching {
                                val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
                                app.contentResolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,bounds)}
                                if(bounds.outWidth>0 && bounds.outHeight>0) {onSavedSize?.invoke(bounds.outWidth,bounds.outHeight);Diagnostics.event(Event.CAPTURE,Field.WIDTH to bounds.outWidth,Field.HEIGHT to bounds.outHeight,Field.STYLE to style.ordinal)}
                            }
                        }
                        if(!grade.needsCopy(style)) {done(null);return}
                        val original=output.savedUri ?: run {done("原片已保存；无法读取调色源文件");return}
                        worker.execute {
                            var bitmap:Bitmap?=null;var edited:Uri?=null;var temp:File?=null;var failure:String?=null
                            try {
                                bitmap=app.contentResolver.openInputStream(original)?.use {BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inMutable=true;inPreferredConfig=Bitmap.Config.ARGB_8888})} ?: error("无法读取原片")
                                val b=bitmap!!;val lut=StyleLut.create(style,grade);val rows=32;val pixels=IntArray(b.width*rows)
                                for(y in 0 until b.height step rows) {
                                    val h=minOf(rows,b.height-y);b.getPixels(pixels,0,b.width,0,y,b.width,h)
                                    for(i in 0 until b.width*h) pixels[i]=lut.apply(pixels[i])
                                    b.setPixels(pixels,0,b.width,0,y,b.width,h)
                                }
                                temp=File.createTempFile("ziying-edited-",".jpg",app.cacheDir)
                                temp!!.outputStream().use {check(b.compress(Bitmap.CompressFormat.JPEG,98,it)){"照片编码失败"}}
                                val targetExif=ExifInterface(temp!!)
                                app.contentResolver.openInputStream(original)?.use {stream->
                                    val sourceExif=ExifInterface(stream)
                                    for(tag in listOf(ExifInterface.TAG_ORIENTATION,ExifInterface.TAG_DATETIME,ExifInterface.TAG_DATETIME_ORIGINAL,ExifInterface.TAG_MAKE,ExifInterface.TAG_MODEL,ExifInterface.TAG_EXPOSURE_TIME,ExifInterface.TAG_F_NUMBER,ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,ExifInterface.TAG_FOCAL_LENGTH)) sourceExif.getAttribute(tag)?.let {targetExif.setAttribute(tag,it)}
                                }
                                targetExif.saveAttributes()
                                val metadata=values("调色");if(Build.VERSION.SDK_INT>=29) metadata.put(MediaStore.Images.Media.IS_PENDING,1)
                                edited=EditedCopyTransaction.publish(original,
                                    create={app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,metadata) ?: error("无法创建调色副本")},
                                    write={uri->app.contentResolver.openOutputStream(uri)?.use {out->temp!!.inputStream().use {it.copyTo(out)}} ?: error("无法写入副本")},
                                    commit={uri->if(Build.VERSION.SDK_INT>=29) app.contentResolver.update(uri,ContentValues().apply {put(MediaStore.Images.Media.IS_PENDING,0)},null,null)},
                                    delete={uri->app.contentResolver.delete(uri,null,null)})
                            } catch(e:Exception) {Diagnostics.error(Event.CAPTURE_ERROR,e);failure="原片已保存；调色副本失败：${e.message}"}
                            catch(e:OutOfMemoryError) {Diagnostics.error(Event.CAPTURE_ERROR,e);failure="原片已保存；内存不足，未生成调色副本"}
                            finally {
                                bitmap?.recycle();temp?.delete()
                                if(failure!=null) edited?.let {runCatching {app.contentResolver.delete(it,null,null)}}
                                main.execute {done(failure)}
                            }
                        }
                    }
                })
        } catch(e:Exception) {done("拍摄失败：${e.message}")}
    }
}
