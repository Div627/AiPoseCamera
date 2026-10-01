package com.aipose.camera.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 应用内更新：
 * - 扫码更新：扫描包含 APK 直链的二维码 -> 下载 -> 拉起安装器
 * - 在线检查：对比云端 version.json 的 versionCode，有新版自动下载
 */
object Updater {

    const val VERSION_JSON_URL = "https://ai-photo-apk.app.workbuddy.host/version.json"

    data class RemoteVersion(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** 拉取云端最新版本信息 */
    suspend fun fetchRemoteVersion(): RemoteVersion = withContext(Dispatchers.IO) {
        val body = client.newCall(Request.Builder().url(VERSION_JSON_URL).build())
            .execute().use { resp ->
                if (!resp.isSuccessful) error("服务器返回 ${resp.code}")
                resp.body?.string() ?: error("响应为空")
            }
        val json = JSONObject(body)
        RemoteVersion(
            versionCode = json.getInt("versionCode"),
            versionName = json.getString("versionName"),
            apkUrl = json.getString("apkUrl"),
        )
    }

    /** 下载 APK 到应用缓存目录，onProgress 回调 0~100 */
    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val parsed = UpdateLink.parse(url) ?: error("需要有效的 HTTPS APK 下载链接")
        val dir = File(context.cacheDir, "apks").apply { mkdirs() }
        val file = File(dir, "aipose-update.apk")
        try {
            client.newCall(Request.Builder().url(parsed).build()).execute().use { resp ->
                if (!resp.isSuccessful) error("下载失败 HTTP ${resp.code}")
                val body = resp.body ?: error("下载内容为空")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        var lastPct = -1
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buf)
                            if (read == -1) break
                            output.write(buf, 0, read)
                            done += read
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt().coerceIn(0, 100)
                                if (pct != lastPct) { onProgress(pct); lastPct = pct }
                            }
                        }
                        if (total >= 0 && done != total) error("下载不完整，请重试")
                    }
                }
            }
            val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                ?: error("下载内容不是有效 APK，链接可能已过期")
            if (archive.packageName != context.packageName) error("二维码指向的不是 映刻相机安装包")
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        file
    }

    /**
     * 拉起 APK 安装器。
     * 返回 true = 已拉起安装器；false = 缺少"安装未知应用"权限，已跳转授权页（授权后重试即可）。
     */
    fun installApk(context: Context, apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }
}
