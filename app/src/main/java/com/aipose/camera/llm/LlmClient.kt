package com.aipose.camera.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 极简 OpenAI 兼容 /chat/completions 客户端。
 * 支持任意兼容服务商：DeepSeek、通义千问（DashScope compatible-mode）等。
 */
class LlmClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 发起对话。onToken 回调可选（仅当服务商支持流式时使用；MVP 用非流式整体返回）。
     * 出错抛 IOException / IllegalStateException，由调用方处理。
     */
    suspend fun chat(
        config: LlmConfig,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 300,
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", config.model)
            put("max_tokens", maxTokens)
            put("temperature", 0.8)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", userPrompt))
            })
        }.toString()

        val url = config.baseUrl.trimEnd('/') + "/chat/completions"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        suspendCancellableCoroutine { continuation ->
            val call=client.newCall(request)
            continuation.invokeOnCancellation {call.cancel()}
            call.enqueue(object:Callback {
                override fun onFailure(call:Call,e:IOException) {
                    if(continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call:Call,response:Response) {
                    response.use { resp ->
                        try {
                            val text=resp.body?.string().orEmpty()
                            if(!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}: ${text.take(200)}")
                            val answer=JSONObject(text).getJSONArray("choices").getJSONObject(0)
                                .getJSONObject("message").getString("content").trim()
                            if(continuation.isActive) continuation.resume(answer)
                        } catch(e:Exception) {if(continuation.isActive) continuation.resumeWithException(e)}
                    }
                }
            })
        }
    }
}
