package com.aipose.camera.update

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.mockwebserver.*
import okio.Buffer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class UpdateDownloadTest {
    @get:Rule val folder = TemporaryFolder()
    private val zip = byteArrayOf(0x50, 0x4b, 0x03, 0x04, 1, 2, 3, 4)
    private fun apkResponse() = MockResponse().setBody(Buffer().write(zip)).setHeader("Content-Type", "application/vnd.android.package-archive")

    @Test fun completeDownloadAndProgress() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(apkResponse())
            val progress = mutableListOf<DownloadProgress>()
            val file = UpdateDownload().apk(Request.Builder().url(server.url("/app.apk")).build(), folder.root) { progress.add(it) }
            assertArrayEquals(zip, file.readBytes())
            assertEquals(DownloadProgress(8, 8), progress.last())
            assertEquals(100, progress.last().percent)
            assertEquals("identity", server.takeRequest().getHeader("Accept-Encoding"))
        }
    }
    @Test fun unknownLengthShowsBytesWithoutInventedPercent() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setChunkedBody(Buffer().write(zip), 2))
            var last = DownloadProgress()
            UpdateDownload().apk(Request.Builder().url(server.url("/app.apk")).build(), folder.root) { last = it }
            assertEquals(8L, last.bytes); assertNull(last.percent)
        }
    }
    @Test fun redirectDownloadsActualAttachment() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", server.url("/asset")))
            server.enqueue(apkResponse())
            val file = UpdateDownload().apk(Request.Builder().url(server.url("/app.apk")).build(), folder.root) {}
            assertArrayEquals(zip, file.readBytes()); assertEquals(2, server.requestCount)
        }
    }
    @Test fun expiredLinkDeletesTemporaryFile() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(404))
            val error = runCatching { UpdateDownload().apk(Request.Builder().url(server.url("/app.apk")).build(), folder.root) {} }.exceptionOrNull()!!
            assertEquals(UpdateProblem.NOT_FOUND, UpdateErrors.classify(error))
            assertTrue(folder.root.listFiles()!!.isEmpty())
        }
    }
    @Test fun htmlResponseCannotBecomeInstallable() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("<html>Expired</html>").setHeader("Content-Type", "text/html"))
            val error = runCatching { UpdateDownload().apk(Request.Builder().url(server.url("/app.apk")).build(), folder.root) {} }.exceptionOrNull()!!
            assertEquals(UpdateProblem.NOT_APK, UpdateErrors.classify(error)); assertTrue(folder.root.listFiles()!!.isEmpty())
        }
    }
    @Test fun mislabeledResponseCannotBecomeInstallable() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{\"message\":\"blocked\"}").setHeader("Content-Type", "application/octet-stream"))
            val error = runCatching { UpdateDownload().apk(Request.Builder().url(server.url("/app.apk")).build(), folder.root) {} }.exceptionOrNull()!!
            assertEquals(UpdateProblem.NOT_APK, UpdateErrors.classify(error)); assertTrue(folder.root.listFiles()!!.isEmpty())
        }
    }
    @Test fun truncatedBodyIsRejected() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(apkResponse().setHeader("Content-Length", "100").setSocketPolicy(SocketPolicy.DISCONNECT_AT_END))
            val error = runCatching { UpdateDownload().apk(Request.Builder().url(server.url("/app.apk")).build(), folder.root) {} }.exceptionOrNull()!!
            assertEquals(UpdateProblem.INCOMPLETE, UpdateErrors.classify(error)); assertTrue(folder.root.listFiles()!!.isEmpty())
        }
    }
    @Test fun cancellationClosesActualNetworkCallAndAllowsExit() = runBlocking {
        MockWebServer().use { server ->
            val failed = CountDownLatch(1)
            val client = OkHttpClient.Builder().eventListener(object : EventListener() {
                override fun callFailed(call: Call, ioe: java.io.IOException) { failed.countDown() }
            }).build()
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val job = async { UpdateDownload(client).apk(Request.Builder().url(server.url("/app.apk")).build(), folder.root) {} }
            withContext(Dispatchers.IO) { assertNotNull(server.takeRequest(3, TimeUnit.SECONDS)) }
            withTimeout(2000) { job.cancelAndJoin() }
            assertTrue(failed.await(3, TimeUnit.SECONDS)); assertTrue(folder.root.listFiles()!!.isEmpty())
        }
    }
    @Test fun storageFailureIsDistinctFromNetwork() = runBlocking {
        val error = runCatching { UpdateDownload().apk(Request.Builder().url("https://example.com/app.apk").build(), folder.newFile()) {} }.exceptionOrNull()!!
        assertEquals(UpdateProblem.STORAGE, UpdateErrors.classify(error))
    }
    @Test fun networkFailuresStayActionable() {
        assertEquals(UpdateProblem.TIMEOUT, UpdateErrors.classify(SocketTimeoutException("private-url")))
        assertEquals(UpdateProblem.NETWORK, UpdateErrors.classify(UnknownHostException("private-host")))
        assertFalse(UpdateErrors.classify(SocketTimeoutException("private-url")).message.contains("private-url"))
    }
    @Test fun githubFallbackPreservesExactRepositoryTagAndAttachment() {
        val release = GithubReleaseAsset.from("https://github.com/Div627/AiPoseCamera/releases/download/0.1.0-beta.6/yingke-camera.apk")!!
        assertEquals("https://api.github.com/repos/Div627/AiPoseCamera/releases/tags/0.1.0-beta.6", release.metadataUrl.toString())
        val request = release.request("""{"assets":[{"id":7,"name":"other.apk"},{"id":9,"name":"yingke-camera.apk"}]}""")
        assertEquals("https://api.github.com/repos/Div627/AiPoseCamera/releases/assets/9", request.url.toString())
        assertEquals("application/octet-stream", request.header("Accept"))
    }
    @Test fun fallbackDoesNotUseUnrelatedHostOrNonReleaseLink() {
        assertNull(GithubReleaseAsset.from("https://github.com.evil.test/a/b/releases/download/t/app.apk"))
        assertNull(GithubReleaseAsset.from("https://github.com/a/b/raw/main/app.apk"))
    }
    @Test fun missingFallbackAttachmentReportsExpiredLink() {
        val release = GithubReleaseAsset("a", "b", "t", "app.apk")
        val error = runCatching { release.request("""{"assets":[{"id":9,"name":"other.apk"}]}""") }.exceptionOrNull()!!
        assertEquals(UpdateProblem.NOT_FOUND, UpdateErrors.classify(error))
    }
    @Test fun validatesPackageVersionAndCompatibleSignature() {
        fun check(name: String = "com.aipose.camera", version: Long = 19, signatures: Set<String> = setOf("beta")) =
            UpdatePackagePolicy.check(name, "com.aipose.camera", version, 18, setOf("beta"), signatures)
        assertNull(check()); assertEquals(UpdateProblem.WRONG_APP, check(name = "other"))
        assertEquals(UpdateProblem.CURRENT_VERSION, check(version = 18)); assertEquals(UpdateProblem.CURRENT_VERSION, check(version = 17))
        assertEquals(UpdateProblem.SIGNATURE, check(signatures = setOf("foreign")))
        assertEquals(UpdateProblem.SIGNATURE, check(signatures = emptySet()))
    }
    @Test fun supportsVerifiedSingleSignerLineageButNotChangedMultipleSigners() {
        assertNull(UpdatePackagePolicy.check("app", "app", 2, 1, setOf("old"), setOf("old", "new"), allowLineage = true))
        assertEquals(UpdateProblem.SIGNATURE, UpdatePackagePolicy.check("app", "app", 2, 1, setOf("old"), setOf("old", "extra")))
        assertEquals(UpdateProblem.SIGNATURE, UpdatePackagePolicy.check("app", "app", 2, 1, emptySet(), setOf("beta")))
    }
}
