package com.aipose.camera.camera

import kotlin.math.abs

/** Conservative image-edge candidate, not semantic sky or scene recognition. */
class LandscapeGuidance {
    private var anchor: Float? = null
    private var since = 0L
    private var last = 0L
    private var missing = 0
    var horizon: Float? = null
        private set

    fun update(signature: FloatArray, now: Long): String? {
        if (last > 0 && now - last > 900) reset()
        last = now
        val candidate = estimate(signature)
        if (candidate == null) {
            missing++
            if (missing >= 2) reset()
            return null
        }
        missing = 0
        if (anchor == null || abs(candidate - anchor!!) > .055f) {
            anchor = candidate; since = now; horizon = null
            return null
        }
        if (now - since < 1000) return null
        horizon = candidate
        val target = if (candidate <= .5f) 1f / 3 else 2f / 3
        return when {
            abs(candidate - target) < .065f -> "层次位置合适，留意画面四边后按快门"
            candidate > target -> "镜头稍向下压，让横向分界靠近三分线"
            else -> "镜头稍向上抬，让横向分界靠近三分线"
        }
    }

    fun reset() { anchor = null; since = 0; last = 0; missing = 0; horizon = null }

    companion object {
        fun estimate(frame: FloatArray): Float? {
            if (frame.size != 1024 || frame.any { !it.isFinite() || it !in 0f..1f }) return null
            val energies = FloatArray(31) { y -> (0 until 32).sumOf { x -> abs(frame[(y + 1) * 32 + x] - frame[y * 32 + x]).toDouble() }.toFloat() / 32 }
            val row = (5..25).maxByOrNull { energies[it] } ?: return null
            val peak = energies[row]
            if (peak < .075f) return null
            val supported = (0 until 32).count { x -> abs(frame[(row + 1) * 32 + x] - frame[row * 32 + x]) > peak * .45f }
            if (supported < 24) return null
            // Flat ceilings/walls, striped textures and distributed edges are not useful evidence.
            val upper = energies.take(row).average().toFloat()
            val lower = energies.drop(row + 1).average().toFloat()
            if (upper > .028f || lower < .012f || peak < upper * 3 || peak < lower * 2) return null
            val above = frame.take((row + 1) * 32).average()
            val below = frame.drop((row + 1) * 32).average()
            if (above - below < .12) return null
            return (row + 1) / 32f
        }
    }
}
