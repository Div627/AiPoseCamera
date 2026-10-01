package com.aipose.camera.llm

/**
 * LLM 提供商配置。DeepSeek 与通义千问均采用 OpenAI 兼容协议，
 * 只需 base URL + 模型名 + Key 即可调用。
 */
data class LlmConfig(
    val providerId: String,   // deepseek / qwen / custom
    val displayName: String,
    val baseUrl: String,      // 如 https://api.deepseek.com
    val model: String,        // 如 deepseek-chat / qwen-plus
    val apiKey: String,
) {
    companion object {
        const val DEEPSEEK = "deepseek"
        const val QWEN = "qwen"

        fun preset(providerId: String, apiKey: String, modelOverride: String? = null): LlmConfig =
            when (providerId) {
                DEEPSEEK -> LlmConfig(
                    DEEPSEEK, "DeepSeek",
                    "https://api.deepseek.com",
                    modelOverride ?: "deepseek-chat", apiKey,
                )
                QWEN -> LlmConfig(
                    QWEN, "通义千问",
                    "https://dashscope.aliyuncs.com/compatible-mode/v1",
                    modelOverride ?: "qwen-plus", apiKey,
                )
                else -> LlmConfig(providerId, "自定义", "", modelOverride ?: "", apiKey)
            }
    }
}
