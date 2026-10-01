package com.aipose.camera.settings

import android.content.Context
import com.aipose.camera.BuildConfig
import com.aipose.camera.llm.LlmConfig

/**
 * 设置持久化（SharedPreferences）。
 * beta 模型密钥来自 local.properties，经 BuildConfig 注入；不读取历史用户密钥。
 */
class SettingsRepository(context: Context) {

    private val sp = context.getSharedPreferences("ai_pose_settings", Context.MODE_PRIVATE)

    var providerId: String
        get() = sp.getString(KEY_PROVIDER, LlmConfig.DEEPSEEK) ?: LlmConfig.DEEPSEEK
        set(value) = sp.edit().putString(KEY_PROVIDER, value).apply()

    val deepseekKey: String get() = BuildConfig.DEEPSEEK_KEY
    val qwenKey: String get() = BuildConfig.QWEN_KEY

    var deepseekModel: String
        get() = sp.getString(KEY_DS_MODEL, "deepseek-chat") ?: "deepseek-chat"
        set(value) = sp.edit().putString(KEY_DS_MODEL, value).apply()

    var qwenModel: String
        get() = sp.getString(KEY_QWEN_MODEL, "qwen-plus") ?: "qwen-plus"
        set(value) = sp.edit().putString(KEY_QWEN_MODEL, value).apply()

    var autoCapture: Boolean
        get() = sp.getBoolean(KEY_AUTO_CAPTURE, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_CAPTURE, value).apply()

    var aiReviewEnabled: Boolean
        get() = sp.getBoolean(KEY_AI_REVIEW, true)
        set(value) = sp.edit().putBoolean(KEY_AI_REVIEW, value).apply()

    /** 当前生效的 LLM 配置 */
    fun currentConfig(): LlmConfig = when (providerId) {
        LlmConfig.DEEPSEEK -> LlmConfig.preset(LlmConfig.DEEPSEEK, deepseekKey, deepseekModel)
        LlmConfig.QWEN -> LlmConfig.preset(LlmConfig.QWEN, qwenKey, qwenModel)
        else -> LlmConfig.preset(providerId, "", "")
    }

    companion object {
        private const val KEY_PROVIDER = "provider"
        private const val KEY_DS_MODEL = "deepseek_model"
        private const val KEY_QWEN_MODEL = "qwen_model"
        private const val KEY_AUTO_CAPTURE = "auto_capture"
        private const val KEY_AI_REVIEW = "ai_review"
    }
}
