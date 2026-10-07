package com.aipose.camera.assistant

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class VideoStudyState(val study: VideoStudy? = null, val loading: Boolean = true,
    val progress: Float = 0f, val analyzing: Boolean = false, val saving: Boolean = false,
    val saved: String? = null, val error: String? = null)
class VideoStudyViewModel(app: Application, private val projectId: String): AndroidViewModel(app) {
    private val store = VideoStudyStore(app, projectId)
    private val mutable = MutableStateFlow(VideoStudyState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    init {viewModelScope.launch {mutable.value = mutable.value.copy(study = withContext(Dispatchers.IO) {store.load()}, loading = false)}}
    fun import(uri: Uri) {
        if(state.value.analyzing || state.value.saving || state.value.loading) return
        mutable.value = state.value.copy(analyzing = true, progress = 0f, error = null, saved = null)
        job = viewModelScope.launch {
            var imported: File? = null
            var completed = false
            try {
                val clip = ProjectMedia.import(getApplication(), projectId, "source", uri)
                imported = File(clip.file)
                val study = VideoFrameAnalyzer.analyze(imported, store) {progress -> mutable.value = state.value.copy(progress = progress)}
                completed = true
                mutable.value = state.value.copy(study = study)
            } catch(e: CancellationException) {throw e}
            catch(_: Exception) {mutable.value = state.value.copy(error = "这段视频暂时无法分析。支持十分钟内、512 MB 以下的视频，可重新选择。")}
            finally {
                if(!completed && imported != null) withContext(NonCancellable + Dispatchers.IO) {
                    if(store.load()?.source != imported.absolutePath) imported.delete()
                }
                mutable.value = state.value.copy(analyzing = false)
            }
        }
    }
    fun cancel() {job?.cancel()}
    fun clearSaved() {if(!state.value.saving) mutable.value=state.value.copy(saved=null)}
    fun save(frame: FrameCandidate, edit: FrameEdit, associated: (String) -> Unit) {
        val study = state.value.study ?: return
        if(state.value.analyzing || state.value.saving) return
        mutable.value = state.value.copy(saving = true, saved = null, error = null)
        viewModelScope.launch {
            try {
                val (uri, dimensions) = FramePhotoEditor.save(getApplication(), study, frame, edit)
                associated(uri.toString())
                mutable.value = state.value.copy(saved = "已保存到相册 · $dimensions")
            } catch(e: CancellationException) {throw e}
            catch(_: Exception) {mutable.value = state.value.copy(error = "照片保存失败，原视频仍保留，可以重试。")}
            finally {mutable.value = state.value.copy(saving = false)}
        }
    }
}
