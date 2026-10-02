package com.aipose.camera.update

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DownloadProgress(val bytes: Long = 0, val total: Long = -1) {
    val percent: Int? get() = if (total > 0) (bytes * 100 / total).toInt().coerceIn(0, 100) else null
}

/** Official fallback for the exact same public release attachment, without third-party mirrors. */
data class GithubReleaseAsset(val owner: String, val repository: String, val tag: String, val name: String) {
    val metadataUrl get() = HttpUrl.Builder().scheme("https").host("api.github.com")
        .addPathSegment("repos").addPathSegment(owner).addPathSegment(repository)
        .addPathSegment("releases").addPathSegment("tags").addPathSegment(tag).build()

    fun request(metadata: String): Request {
        val assets = JSONObject(metadata).getJSONArray("assets")
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name") == name && asset.getLong("id") > 0) {
                val url = HttpUrl.Builder().scheme("https").host("api.github.com")
                    .addPathSegment("repos").addPathSegment(owner).addPathSegment(repository)
                    .addPathSegment("releases").addPathSegment("assets").addPathSegment(asset.getLong("id").toString()).build()
                return Request.Builder().url(url).header("Accept", "application/octet-stream").build()
            }
        }
        throw UpdateFailure(UpdateProblem.NOT_FOUND)
    }

    companion object {
        fun from(value: String): GithubReleaseAsset? {
            val url = value.toHttpUrlOrNull() ?: return null
            val p = url.pathSegments
            if (url.scheme != "https" || url.host != "github.com" || p.size != 6 || p[2] != "releases" || p[3] != "download") return null
            return GithubReleaseAsset(p[0], p[1], p[4], p[5])
        }
    }
}

/** Body consumption stays on OkHttp workers; cancellation closes the actual network call. */
class UpdateDownload(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
    .callTimeout(5, TimeUnit.MINUTES).followSslRedirects(false).build()) {
    private suspend fun <T> execute(request: Request, cleanup: () -> Unit = {}, consume: (Response, () -> Boolean) -> T): T =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request.newBuilder().header("Accept-Encoding", "identity").build())
            continuation.invokeOnCancellation { call.cancel(); cleanup() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!continuation.isActive) return
                        try {
                            if (!response.isSuccessful) throw UpdateFailure(UpdateErrors.http(response.code))
                            val result = consume(response) { continuation.isActive }
                            if (continuation.isActive) continuation.resume(result)
                        } catch (e: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(e)
                        } finally {
                            if (continuation.isCancelled) cleanup()
                        }
                    }
                }
            })
        }

    suspend fun metadata(request: Request): String = execute(request) { response, active ->
        val body = response.body ?: throw UpdateFailure(UpdateProblem.INCOMPLETE)
        body.byteStream().use { stream ->
            val bytes = readLimited(stream, 1_048_577)
            if (!active() || bytes.size > 1_048_576) throw UpdateFailure(UpdateProblem.SERVER)
            bytes.toString(Charsets.UTF_8)
        }
    }

    suspend fun apk(request: Request, directory: File, progress: (DownloadProgress) -> Unit): File {
        val target = try { directory.mkdirs(); File.createTempFile("yingke-update-", ".apk", directory) }
        catch (e: IOException) { throw UpdateFailure(UpdateProblem.STORAGE, e) }
        try {
            execute(request, cleanup = { target.delete() }) { response, active ->
                val body = response.body ?: throw UpdateFailure(UpdateProblem.INCOMPLETE)
                if (body.contentType()?.subtype?.contains("html") == true) throw UpdateFailure(UpdateProblem.NOT_APK)
                val total = body.contentLength()
                if (total == 0L || total > MAXIMUM_BYTES) throw UpdateFailure(UpdateProblem.INVALID_APK)
                progress(DownloadProgress(total = total))
                val output = try { target.outputStream() } catch (e: IOException) { throw UpdateFailure(UpdateProblem.STORAGE, e) }
                var bytes = 0L
                var notified = 0L
                body.byteStream().use { input -> output.use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (active()) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (bytes + count > MAXIMUM_BYTES) throw UpdateFailure(UpdateProblem.INVALID_APK)
                        try { out.write(buffer, 0, count) } catch (e: IOException) { throw UpdateFailure(UpdateProblem.STORAGE, e) }
                        bytes += count
                        val now = System.nanoTime()
                        if (now - notified >= 150_000_000) { notified = now; progress(DownloadProgress(bytes, total)) }
                    }
                } }
                if (!active() || bytes == 0L || (total >= 0 && bytes != total)) throw UpdateFailure(UpdateProblem.INCOMPLETE)
                if (!target.inputStream().use { readLimited(it, 4) }.contentEquals(byteArrayOf(0x50, 0x4b, 0x03, 0x04)))
                    throw UpdateFailure(UpdateProblem.NOT_APK)
                progress(DownloadProgress(bytes, total))
            }
            return target
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    private fun readLimited(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(minOf(limit, 8192))
        while (output.size() < limit) {
            val read = input.read(buffer, 0, minOf(buffer.size, limit - output.size()))
            if (read < 0) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    companion object { private const val MAXIMUM_BYTES = 250L * 1024 * 1024 }
}
