package com.aipose.camera.assistant

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ProjectMedia {
    fun directory(context: Context, projectId: String): File {
        require(projectId.matches(Regex("[a-zA-Z0-9-]+")))
        return File(context.filesDir,"projects/$projectId").apply { mkdirs() }
    }
    suspend fun import(context: Context, projectId: String, shotId: String, uri: Uri): LocalClip = withContext(Dispatchers.IO) {
        val directory=directory(context,projectId)
        val temporary=File(directory,"${newId()}.import")
        try {
            require(directory.usableSpace>64L*1024*1024)
            context.contentResolver.openInputStream(uri)?.use { input -> temporary.outputStream().use { output ->
                val buffer=ByteArray(65536); var total=0L
                while(true) {
                    val size=input.read(buffer); if(size<0) break
                    total+=size; require(total<512L*1024*1024 && directory.usableSpace>16L*1024*1024)
                    output.write(buffer,0,size)
                }
            } } ?: error("Unreadable input")
            val duration=duration(temporary)
            require(duration>=500)
            val result=File(directory,"${newId()}.mp4")
            check(temporary.renameTo(result))
            LocalClip(shotId,result.absolutePath,duration)
        } finally { temporary.delete() }
    }
    fun duration(file: File): Long {
        val reader=MediaMetadataRetriever()
        return try {
            reader.setDataSource(file.absolutePath)
            require(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)=="yes")
            reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally { reader.release() }
    }
    fun shareUri(context: Context, file: String): Uri = FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",File(file))
    suspend fun saveToGallery(context: Context, path: String): Uri = withContext(Dispatchers.IO) {
        val values=ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME,"yingke_vlog_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE,"video/mp4")
            if(Build.VERSION.SDK_INT>=29) {put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/AiPoseCamera");put(MediaStore.Video.Media.IS_PENDING,1)}
        }
        val uri=context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values) ?: error("No gallery entry")
        try {
            context.contentResolver.openOutputStream(uri)?.use { out -> File(path).inputStream().use { it.copyTo(out) } } ?: error("No output")
            if(Build.VERSION.SDK_INT>=29) context.contentResolver.update(uri,ContentValues().apply {put(MediaStore.Video.Media.IS_PENDING,0)},null,null)
            uri
        } catch(e:Exception) {context.contentResolver.delete(uri,null,null);throw e}
    }
}
