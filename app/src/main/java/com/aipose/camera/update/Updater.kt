package com.aipose.camera.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.security.MessageDigest

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

    /** Scan stays in place; only this explicit installation action opens system UI. */
    suspend fun downloadApk(
        context: Context,
        url: String,
        onReconnect: () -> Unit,
        onProgress: (DownloadProgress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val parsed = UpdateLink.parse(url) ?: throw UpdateFailure(UpdateProblem.NOT_APK)
        val downloader = UpdateDownload()
        val dir = File(context.cacheDir, "apks")
        try {
            return@withContext downloader.apk(Request.Builder().url(parsed).build(), dir, onProgress)
        } catch (e: java.io.IOException) {
            val problem = UpdateErrors.classify(e)
            val fallback = GithubReleaseAsset.from(parsed)
            if (fallback == null || problem !in setOf(UpdateProblem.NETWORK, UpdateProblem.TIMEOUT, UpdateProblem.SERVER)) throw e
            onReconnect()
            val metadata = downloader.metadata(Request.Builder().url(fallback.metadataUrl).header("Accept", "application/vnd.github+json").build())
            return@withContext downloader.apk(fallback.request(metadata), dir, onProgress)
        }
    }

    data class VerifiedApk(val file: File, val versionName: String)

    @Suppress("DEPRECATION")
    suspend fun verifyApk(context: Context, file: File): VerifiedApk = withContext(Dispatchers.IO) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val manager = context.packageManager
        val archive = manager.getPackageArchiveInfo(file.absolutePath, flags) ?: throw UpdateFailure(UpdateProblem.INVALID_APK)
        val installed = manager.getPackageInfo(context.packageName, flags)
        fun signers(info: android.content.pm.PackageInfo, history: Boolean): Set<String> {
            val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.let {
                if (history && !it.hasMultipleSigners()) it.signingCertificateHistory else it.apkContentsSigners
            } else info.signatures
            return signatures.orEmpty().map { signature ->
                MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
            }.toSet()
        }
        fun version(info: android.content.pm.PackageInfo) = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        UpdatePackagePolicy.check(archive.packageName, context.packageName, version(archive), version(installed),
            signers(installed, false), signers(archive, true), allowLineage = Build.VERSION.SDK_INT >= 28 &&
                archive.signingInfo?.hasMultipleSigners() == false && installed.signingInfo?.hasMultipleSigners() == false)?.let { throw UpdateFailure(it) }
        VerifiedApk(file, archive.versionName ?: "更新版本")
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
