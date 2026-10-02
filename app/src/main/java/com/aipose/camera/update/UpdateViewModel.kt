package com.aipose.camera.update

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipose.camera.diagnostics.Diagnostics
import com.aipose.camera.diagnostics.DiagnosticStore.Event
import com.aipose.camera.diagnostics.DiagnosticStore.Field
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

enum class UpdatePhase { SCAN, CONNECTING, DOWNLOADING, VERIFYING, READY, ERROR, CANCELLED, PERMISSION, INSTALLING }
data class UpdateState(val phase: UpdatePhase = UpdatePhase.SCAN, val url: String? = null,
                       val progress: DownloadProgress = DownloadProgress(), val file: File? = null,
                       val version: String = "", val message: String = "", val problem: UpdateProblem? = null) {
    val busy get() = phase in setOf(UpdatePhase.CONNECTING, UpdatePhase.DOWNLOADING, UpdatePhase.VERIFYING)
}

/** Navigation-scoped: survives configuration changes and never opens a browser or another app page. */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow(UpdateState())
    val state = mutableState.asStateFlow()
    private var download: Job? = null
    @Volatile private var epoch = 0L

    fun start(value: String) {
        val url = UpdateLink.parse(value) ?: return
        val ticket = ++epoch
        download?.cancel()
        mutableState.value.file?.delete()
        mutableState.value = UpdateState(UpdatePhase.CONNECTING, url, message = "正在连接下载服务…")
        download = viewModelScope.launch {
            var file: File? = null
            try {
                file = Updater.downloadApk(getApplication(), url,
                    onReconnect = { mutableState.update { if (ticket == epoch) it.copy(phase = UpdatePhase.CONNECTING, progress = DownloadProgress(), message = "正在重新连接下载服务…") else it } },
                    onProgress = { progress -> mutableState.update {
                        if (ticket == epoch) it.copy(phase = UpdatePhase.DOWNLOADING, progress = progress, message = "请保持网络连接") else it
                    } })
                if (ticket != epoch) return@launch
                mutableState.update { it.copy(phase = UpdatePhase.VERIFYING, message = "正在核对安装包、版本和签名…") }
                val verified = Updater.verifyApk(getApplication(), file)
                if (ticket != epoch) return@launch
                mutableState.update { it.copy(phase = UpdatePhase.READY, file = file, version = verified.versionName, message = "下载完成，点击安装更新") }
                Diagnostics.event(Event.UPDATE, Field.BYTES to file.length(), Field.ACCEPTED to 1)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Diagnostics.error(Event.UPDATE_ERROR, e)
                Diagnostics.event(Event.UPDATE_ERROR, Field.STATUS to UpdateErrors.classify(e).ordinal)
                if (ticket == epoch) mutableState.update { it.copy(phase = UpdatePhase.ERROR, message = UpdateErrors.classify(e).message, problem = UpdateErrors.classify(e)) }
            } finally {
                if (mutableState.value.file != file) file?.delete()
            }
        }
    }

    fun retry() { mutableState.value.url?.let(::start) }
    fun cancel() {
        ++epoch
        download?.cancel()
        mutableState.value = mutableState.value.copy(phase = UpdatePhase.CANCELLED, message = "已取消下载，可以重试或重新扫码")
    }
    fun scanAgain() {
        cancel()
        mutableState.value.file?.delete()
        mutableState.value = UpdateState()
    }

    fun install() {
        val snapshot = mutableState.value
        val file = snapshot.file?.takeIf { it.isFile } ?: run {
            mutableState.update { it.copy(phase = UpdatePhase.ERROR, file = null, message = "下载文件已被清理，请重新下载") }; return
        }
        try {
            val opened = Updater.installApk(getApplication(), file)
            mutableState.update { it.copy(phase = if (opened) UpdatePhase.INSTALLING else UpdatePhase.PERMISSION,
                message = if (opened) "请在系统安装器中确认，返回这里可继续安装" else "请允许映刻相机安装更新，返回这里后继续安装") }
        } catch (e: Exception) {
            Diagnostics.error(Event.UPDATE_ERROR, e)
            mutableState.update { it.copy(phase = UpdatePhase.READY, message = "无法打开系统安装器，请重试") }
        }
    }

    fun resumed() {
        val phase = mutableState.value.phase
        if (phase == UpdatePhase.INSTALLING || (phase == UpdatePhase.PERMISSION &&
            (Build.VERSION.SDK_INT < 26 || getApplication<Application>().packageManager.canRequestPackageInstalls())))
            mutableState.update { it.copy(phase = UpdatePhase.READY, message = "安装包已就绪，点击继续安装") }
    }

    override fun onCleared() {
        ++epoch
        download?.cancel()
        mutableState.value.file?.delete()
        super.onCleared()
    }
}
