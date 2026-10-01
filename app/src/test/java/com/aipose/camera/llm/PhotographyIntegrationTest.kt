package com.aipose.camera.llm

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PhotographyIntegrationTest {
    @Test fun actualModelRequestIncludesPackagedGuideAndSceneForBothProviders() = runBlocking {
        val server=MockWebServer();server.start()
        try {
            val guide=PhotographyGuide.systemPrompt(File("src/main/assets/photography/v2/photography-guide.txt").readText())
            for((provider,count) in listOf("deepseek" to 0,"qwen" to 3)) {
                server.enqueue(MockResponse().setBody("""{"choices":[{"message":{"content":"请保持画面稳定"}}]}"""))
                val config=LlmConfig(provider,provider,server.url("/").toString(),"test-model","test-only-key")
                val scene=PhotographyGuide.sceneContext(count,true,"原色",0f,if(count>0) "自然并肩" else null)
                assertEquals("请保持画面稳定",LlmClient().chat(config,guide,scene))
                val request=server.takeRequest(2,TimeUnit.SECONDS)!!
                assertEquals("/chat/completions",request.path)
                val json=JSONObject(request.body.readUtf8())
                val messages=json.getJSONArray("messages")
                assertEquals(guide,messages.getJSONObject(0).getString("content"))
                assertEquals("system",messages.getJSONObject(0).getString("role"))
                assertEquals(scene,messages.getJSONObject(1).getString("content"))
                assertFalse(json.has("image_url"))
            }
        } finally {server.shutdown()}
    }
    @Test fun leavingAiCanCancelNetworkRequest() = runBlocking {
        val server=MockWebServer();server.start()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        try {
            val job=launch(Dispatchers.Default) {
                LlmClient().chat(LlmConfig("test","test",server.url("/").toString(),"test","test"),"guide","scene")
            }
            assertNotNull(withContext(Dispatchers.IO){server.takeRequest(3,TimeUnit.SECONDS)})
            withTimeout(1000) {job.cancelAndJoin()}
            assertTrue(job.isCancelled)
        } finally {server.shutdown()}
    }
}
