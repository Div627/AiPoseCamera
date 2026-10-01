package com.aipose.camera.pose

/** 检测到的人体关键点：索引 -> (x, y)，归一化显示坐标 */
typealias Landmarks = Map<Int, Pair<Float, Float>>

/**
 * 姿势对齐计算 + 即时调整建议（纯本地、毫秒级响应，不依赖大模型）。
 */
object PoseAligner {

    data class Result(
        val score: Int,          // 0~100
        val tips: List<String>,  // 即时调整建议（最多 2 条）
    )

    /** MediaPipe 关键点索引 -> 人类可读部位名（用于即时提示） */
    private val partNames = mapOf(
        0 to "头", 11 to "左肩", 12 to "右肩", 13 to "左手肘", 14 to "右手肘",
        15 to "左手", 16 to "右手", 23 to "左胯", 24 to "右胯",
        25 to "左膝", 26 to "右膝", 27 to "左脚", 28 to "右脚",
    )

    /**
     * 把 MediaPipe 归一化坐标（相对于分析帧）映射到屏幕归一化坐标，
     * 对应 PreviewView FILL_CENTER（cover + 居中裁切）的显示方式。
     * 这样检测结果、模板坐标、预览画面三者统一在同一坐标系。
     */
    fun mapToDisplay(
        points: Landmarks,
        screenW: Float,
        screenH: Float,
        srcW: Float,
        srcH: Float,
    ): Landmarks {
        if (screenW <= 0f || screenH <= 0f || srcW <= 0f || srcH <= 0f) return points
        val scale = maxOf(screenW / srcW, screenH / srcH)
        val visW = screenW / (scale * srcW)  // 源画面宽度可见比例（0~1]
        val visH = screenH / (scale * srcH)
        val offX = (1f - visW) / 2f
        val offY = (1f - visH) / 2f
        return points.mapValues { (_, p) ->
            ((p.first - offX) / visW) to ((p.second - offY) / visH)
        }
    }

    /** Pick the better anatomical-label orientation for mirrored/front/back references.
     * Coordinates never move: drawing, crop and visibility remain in display space.
     */
    fun orient(template:PoseTemplate,points:Landmarks):Landmarks {
        val swaps=mapOf(11 to 12,12 to 11,13 to 14,14 to 13,15 to 16,16 to 15,23 to 24,24 to 23,25 to 26,26 to 25,27 to 28,28 to 27)
        val flipped=points.mapKeys{(id,_)->swaps[id] ?: id}
        fun error(p:Landmarks)=template.points.entries.sumOf {(id,t)->p[id]?.let {kotlin.math.hypot((it.first-t.first).toDouble(),(it.second-t.second).toDouble())} ?: 1.0}
        return if(error(flipped)<error(points)) flipped else points
    }

    fun evaluate(template: PoseTemplate, raw: Landmarks): Result {
        val detected=orient(template,raw)
        if (detected.size < 4) return Result(0, listOf("未检测到完整人像，请退后一点"))

        // 以肩宽为尺度基准，保证远近拍都稳定
        val ls = detected[11] ?: detected[12] ?: return Result(0, emptyList())
        val rs = detected[12] ?: detected[11] ?: return Result(0, emptyList())
        val scale = kotlin.math.hypot(
            (ls.first - rs.first).toDouble(),
            (ls.second - rs.second).toDouble(),
        ).toFloat().coerceAtLeast(0.02f)

        var sum = 0.0f
        var count = 0
        val jointErrors = mutableListOf<Triple<Int, Float, Float>>() // index, dx, dy（以肩宽为单位）

        for ((idx, tp) in template.points) {
            val dp = detected[idx] ?: continue
            val dx = (dp.first - tp.first) / scale
            val dy = (dp.second - tp.second) / scale
            val d = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            sum += d
            count++
            jointErrors.add(Triple(idx, dx, dy))
        }
        if (count == 0) return Result(0, listOf("未检测到关键点"))

        val avg = sum / count
        val score = (100 - (avg * 130).toInt()).coerceIn(0, 100)

        // 生成即时建议：挑偏差最大的 1~2 个部位
        val tips = jointErrors
            .sortedByDescending { kotlin.math.hypot(it.second.toDouble(), it.third.toDouble()) }
            .take(2)
            .filter { (idx, dx, dy) ->
                kotlin.math.hypot(dx.toDouble(), dy.toDouble()) > 0.45
            }
            .map { (idx, dx, dy) ->
                val part = partNames[idx] ?: "身体"
                val vertical = when {
                    dy < -0.45f -> "${part}放低一点"
                    dy > 0.45f -> "${part}抬高一点"
                    else -> null
                }
                val horizontal = when {
                    dx < -0.45f -> "${part}向右移"
                    dx > 0.45f -> "${part}向左移"
                    else -> null
                }
                listOfNotNull(vertical, horizontal).joinToString("、").ifEmpty { "调整${part}位置" }
            }
            .filter { it.isNotBlank() }

        return Result(score, if (tips.isEmpty()) listOf("非常好！保持住这个姿势 ✨") else tips)
    }
}
