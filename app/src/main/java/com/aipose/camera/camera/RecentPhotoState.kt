package com.aipose.camera.camera

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class RecentPhoto(val id: Long, val original: Uri, val edited: Uri? = null, val width: Int = 0, val height: Int = 0,
                       val processing: Boolean = false, val error: String? = null) {
    val display get() = edited ?: original
    val dimensions get() = if (width > 0 && height > 0) "${width}×${height} · ${"%.1f".format(width.toLong() * height / 1_000_000f)} MP" else ""
}

/** Owned by navigation, not a camera mode; all events arrive on the main executor. */
class RecentPhotoState(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("recent_photo", Context.MODE_PRIVATE)
    private val queue = CaptureQueue()
    var latest by mutableStateOf(restore())
        private set
    var processingCount by mutableIntStateOf(0)
        private set
    var capturing by mutableStateOf(false)
        private set
    val canCapture get() = processingCount < 2 && !capturing

    fun reserve(): Long? = queue.reserve()?.also { syncQueue() }
    fun original(id: Long, uri: Uri) {
        if (queue.publish(id)) { latest = RecentPhoto(id, uri, processing = true); persist() }
        syncQueue()
    }
    fun finish(id: Long, result: PhotoCapture.Result) {
        if (queue.isLatest(id) && result.original != null) {
            latest = RecentPhoto(id, result.original, result.edited, result.width, result.height, error = result.error)
            persist()
        }
        queue.complete(id); syncQueue()
    }
    private fun syncQueue() { processingCount = queue.size; capturing = queue.capturing }
    private fun restore(): RecentPhoto? {
        val original = preferences.getString("original", null) ?: return null
        return RecentPhoto(0, Uri.parse(original), preferences.getString("edited", null)?.let(Uri::parse),
            preferences.getInt("width", 0), preferences.getInt("height", 0))
    }
    private fun persist() {
        val photo = latest ?: return
        preferences.edit().putString("original", photo.original.toString()).putString("edited", photo.edited?.toString())
            .putInt("width", photo.width).putInt("height", photo.height).apply()
    }
}
