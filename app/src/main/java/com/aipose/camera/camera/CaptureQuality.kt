package com.aipose.camera.camera

import kotlin.math.abs

/** Conservative preview checks, not a full-resolution sharpness or blink classifier. */
data class CaptureQuality(val light: Float, val highlights: Float, val detail: Float, val available: Boolean) {
    val permitsAutomatic get() = available && light in .08f.. .92f && detail >= .012f
    val hint get() = when {
        !available -> "轻点主体确认对焦，再按快门"
        light < .08f -> "光线偏暗，稳住手机或移到更亮的位置"
        light > .92f || highlights > .20f -> "亮部偏亮，换个角度保留更多细节"
        detail < .012f -> "画面细节偏少，轻点主体确认对焦"
        else -> "稳住手机，检查画面四边后按快门"
    }

    companion object {
        val UNKNOWN = CaptureQuality(.5f, 0f, 0f, false)
        fun measure(signature: FloatArray): CaptureQuality {
            if (signature.size != 1024 || signature.any { !it.isFinite() || it !in 0f..1f }) return UNKNOWN
            var detail = 0f
            for (y in 0 until 31) for (x in 0 until 31) {
                val i = y * 32 + x
                detail += abs(signature[i] - signature[i + 1]) + abs(signature[i] - signature[i + 32])
            }
            return CaptureQuality(signature.average().toFloat(), signature.count { it > .94f } / 1024f, detail / (31 * 31 * 2), true)
        }
    }
}
