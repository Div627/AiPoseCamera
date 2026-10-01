package com.aipose.camera.pose

import kotlin.math.abs
import kotlin.math.hypot

/** Pure, timestamp-based gate: no frame-rate assumptions and no repeat shot until explicitly armed. */
class AutoFraming {
    data class Decision(val tip: String, val zoom: Float? = null, val progress: Float = 0f, val shoot: Boolean = false)
    private var anchor: Landmarks? = null
    private var since = 0L
    private var lastFrame = 0L
    private var lastZoom = 0L
    private var zoomAttempts=0
    private var targetId=""
    var armed = true
        private set
    fun reset(rearm: Boolean = false) {
        anchor = null; since = 0L; lastFrame = 0L
        if (rearm) {armed = true;zoomAttempts=0}
    }
    fun lock() { armed = false; reset() }
    fun evaluate(points: Landmarks, target: PoseTemplate, score: Int, now: Long,
                 zoom: Float, minZoom: Float, maxZoom: Float, enabled: Boolean,
                 zoomSettled: Boolean): Decision {
        if (!enabled || !armed) {
            reset()
            return Decision(if (!enabled) "手动拍摄 · 可自行调整倍率" else "本次拍摄已完成，点「再拍一张」重新准备")
        }
        if(targetId!=target.id) {reset();targetId=target.id;zoomAttempts=0}
        val ids = target.points.keys
        if (!points.keys.containsAll(ids) || points.values.any { !it.first.isFinite() || !it.second.isFinite() }) {
            reset(); return Decision("关节识别不足，请露出四肢或手动拍摄")
        }
        if (lastFrame > 0 && now - lastFrame > 500) reset()
        lastFrame = now
        val p = ids.map { points.getValue(it) }
        val t = target.points.values
        val top = p.minOf { it.second } - .10f
        val bottom = p.maxOf { it.second } + .05f
        val left = p.minOf { it.first } - .035f
        val right = p.maxOf { it.first } + .035f
        val height = bottom - top
        val targetHeight = t.maxOf { it.second } - t.minOf { it.second } + .15f
        val ratio = minOf(targetHeight / height.coerceAtLeast(.1f), .88f / (right-left).coerceAtLeast(.1f))
        val clipped = left < .025f || right > .975f || top < .025f || bottom > .975f
        val desired = (zoom * if (clipped) minOf(ratio, .94f) else ratio).coerceIn(minZoom, maxZoom)
        if (abs(ratio - 1f) > .055f && abs(desired - zoom) > .025f) {
            reset()
            if(zoomAttempts>=18) return Decision("自动变焦已暂停，请调整机位或手动拍摄")
            if (zoomSettled && now - lastZoom >= 450) {
                zoomAttempts++
                lastZoom = now
                return Decision("正在自动调整人物大小…", zoom = desired.coerceIn(zoom * .94f, zoom * 1.06f).coerceIn(minZoom, maxZoom))
            }
            return Decision("等待变焦稳定…")
        }
        if (clipped || abs(height - targetHeight) > .065f) {
            reset(); return Decision(if (height > targetHeight || clipped) "已到广角边界，请后退并让四肢完整入镜" else "变焦已到上限，请靠近一些")
        }
        zoomAttempts=0
        val cx = (points.getValue(23).first + points.getValue(24).first) / 2
        val tx = (target.points.getValue(23).first + target.points.getValue(24).first) / 2
        val cy = points.getValue(0).second
        val ty = target.points.getValue(0).second
        if (abs(cx - tx) > .045f || abs(cy - ty) > .04f) {
            reset(); return Decision(when {
                cx < tx - .045f -> "人物向画面右侧移动一点"
                cx > tx + .045f -> "人物向画面左侧移动一点"
                cy < ty -> "将镜头稍向上抬，让人物在画面中下移"
                else -> "将镜头稍向下压，让人物在画面中上移"
            })
        }
        if (score < 78 || !zoomSettled) {
            reset(); return Decision(if (!zoomSettled) "等待相机稳定…" else "按轮廓调整姿势，达标后自动拍摄")
        }
        val previous = anchor
        val moved = previous == null || ids.any { id ->
            val a = previous.getValue(id); val b = points.getValue(id)
            hypot(a.first - b.first, a.second - b.second) > .018f
        }
        if (moved) { anchor = points.toMap(); since = now }
        val progress = ((now - since) / 1600f).coerceIn(0f, 1f)
        if (progress >= 1f) { lock(); return Decision("正在拍摄…", progress = 1f, shoot = true) }
        return Decision("构图就绪，保持稳定 ${((1600 - (now-since))/1000f).coerceAtLeast(0f).let { "%.1f".format(it) }} 秒", progress = progress)
    }
}
