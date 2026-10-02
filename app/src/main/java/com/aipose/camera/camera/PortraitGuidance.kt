package com.aipose.camera.camera

import com.aipose.camera.pose.Landmarks
import kotlin.math.abs

/** Upright preview coordinates, mirrored for the front camera before entering UI logic. */
data class FaceRegion(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val cx get() = (left + right) / 2
    val cy get() = (top + bottom) / 2
    val height get() = bottom - top
    val valid get() = listOf(left, top, right, bottom).all { it.isFinite() && it in 0f..1f } && right > left && bottom > top
}

object PortraitGuidance {
    enum class Kind { FACE, HALF, FULL, UNKNOWN }
    data class Hint(val kind: Kind, val message: String, val ready: Boolean = false)
    private fun valid(points: Landmarks) = points.values.all { it.first.isFinite() && it.second.isFinite() && it.first in 0f..1f && it.second in 0f..1f }

    fun kind(points: Landmarks?, face: FaceRegion?): Kind = when {
        points != null && valid(points) && listOf(0, 11, 12, 23, 24).all { points.containsKey(it) } && (points.containsKey(27) || points.containsKey(28)) -> Kind.FULL
        points != null && valid(points) && listOf(0, 11, 12).all { points.containsKey(it) } -> Kind.HALF
        face?.valid == true -> Kind.FACE
        else -> Kind.UNKNOWN
    }

    fun next(points: Landmarks?, faces: List<FaceRegion>, count: Int?, intent: TravelIntent, poseTip: String): Hint {
        val face = faces.firstOrNull { it.valid }
        val body = points?.takeIf { valid(it) }
        val kind = kind(body, face)
        if (count == null) return Hint(kind, "对准人物，轻点脸部确认对焦")
        if (count == 0) return Hint(kind, "让人物进入画面，即可获得拍摄指引")
        if (count > 1) {
            val allVisible = faces.size == count && faces.all { it.valid && it.left > .025f && it.right < .975f && it.top > .025f && it.bottom < .975f }
            return Hint(kind, if (allVisible) "肩膀稍错开，自然看向镜头或同伴" else "给每个人留出空间，避免遮住脸部", allVisible)
        }
        if (kind == Kind.FULL && body != null && (body[27] == null || body[28] == null || body.values.any { it.first !in .025f.. .975f || it.second !in .025f.. .975f }))
            return Hint(kind, "稍微后退，给人物四肢留出空间")
        if (face != null) {
            if (face.top < .035f || face.left < .025f || face.right > .975f || face.bottom > .975f)
                return Hint(kind, "稍微后退，给头顶和脸部留出空间")
            if (face.height > .62f) return Hint(kind, "稍微后退，镜头保持在眼睛附近的高度")
            if (face.height < .10f && kind != Kind.FULL) return Hint(kind, "靠近人物一点，让脸部更清晰")
        }
        val position = face?.let { it.cx to it.cy } ?: body?.get(0)
            ?: return Hint(kind, "轻点脸部对焦，保持自然表情")
        val targetX = if (intent == TravelIntent.ENVIRONMENT) .33f else .5f
        val toleranceX = if (intent == TravelIntent.ENVIRONMENT) .12f else .17f
        if (abs(position.first - targetX) > toleranceX) return Hint(kind,
            if (position.first < targetX) "镜头稍向左转，让人物在画面中右移" else "镜头稍向右转，让人物在画面中左移")
        if (position.second < .17f) return Hint(kind, "镜头稍向上抬，为头顶留出空间")
        if (position.second > .55f) return Hint(kind, "镜头稍向下压，让脸部靠近画面上方")
        return Hint(kind, when (kind) {
            Kind.FACE -> "镜头与眼睛齐平，肩膀放松，自然看向镜头"
            Kind.HALF -> "身体稍微侧转，肩膀放松，手臂自然落下"
            Kind.FULL -> poseTip
            Kind.UNKNOWN -> "轻点脸部对焦，保持自然表情"
        }, kind != Kind.UNKNOWN)
    }
}

/** Face-only stability never fabricates missing body landmarks or requests full-body zoom. */
class FaceCaptureGate {
    private var anchor: List<FaceRegion>? = null
    private var since = 0L
    private var last = 0L
    fun reset() { anchor = null; since = 0; last = 0 }
    fun update(now: Long, faces: List<FaceRegion>, count: Int?, ready: Boolean): SceneCaptureGate.Decision {
        if (!ready || count !in 1..4 || faces.size != count || faces.any { !it.valid }) {
            reset(); return SceneCaptureGate.Decision("调整构图后，保持片刻稳定")
        }
        if (last > 0 && now - last > 500) reset()
        last = now
        val old = anchor
        if (old == null || old.size != faces.size || faces.indices.any { i ->
                val a = old[i]; val b = faces[i]
                abs(a.cx - b.cx) > .018f || abs(a.cy - b.cy) > .018f || abs(a.height - b.height) > .025f
            }) { anchor = faces.toList(); since = now }
        val progress = ((now - since) / 1600f).coerceIn(0f, 1f)
        return SceneCaptureGate.Decision("构图就绪，保持自然表情", progress, progress >= 1f)
    }
}
