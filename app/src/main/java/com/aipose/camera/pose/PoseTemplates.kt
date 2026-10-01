package com.aipose.camera.pose

/**
 * 拍照姿势模板。
 * 坐标为归一化显示坐标 (x: 0~1, y: 0~1)，以画面中心人物为基准。
 * key 使用 MediaPipe Pose 的 33 关键点索引：
 * 0=鼻 11/12=左右肩 13/14=左右肘 15/16=左右腕 23/24=左右髋 25/26=左右膝 27/28=左右脚踝
 */
enum class PoseCategory(val label:String) { STANDING("站立"), SEATED("坐姿"), LEANING("倚靠"), LOW("蹲跪"), DYNAMIC("轻动态") }
enum class PoseSupport(val label:String) { NONE(""), CHAIR("需稳固椅子"), STEPS("需宽稳台阶"), GROUND("需平整地面"), WALL("需墙面"), RAIL("需稳固矮栏"), LEDGE("需稳固台面") }

data class PoseTemplate(
    val id: String,
    val name: String,
    val scene: String,
    val points: Map<Int, Pair<Float, Float>>,
    val quickTip: String,
    val category: PoseCategory = PoseCategory.STANDING,
    val support: PoseSupport = PoseSupport.NONE,
) {
    /** Preserve physical proportions of the 3:4 reference on any preview rectangle. */
    fun forViewport(width: Float, height: Float): PoseTemplate {
        if (width <= 0 || height <= 0) return this
        val aspect = width / height
        val sx = minOf(1f, .75f / aspect)
        val sy = minOf(1f, aspect / .75f)
        return copy(points = points.mapValues { (_, p) ->
            (.5f + (p.first-.5f)*sx) to (.5f + (p.second-.5f)*sy)
        })
    }

    companion object {
        // 骨架连线（仅绘制模板中定义的点）
        val CONNECTIONS = listOf(
            11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,
            11 to 23, 12 to 24, 23 to 24,
            23 to 25, 25 to 27, 24 to 26, 26 to 28,
        )

        val ALL: List<PoseTemplate> = PoseAtlas.entries.mapIndexed { index,e ->
            PoseTemplate(e.id,e.name,e.support.label,PoseAtlas.points(index),e.tip,e.category,e.support)
        }
        fun byId(id: String): PoseTemplate? = ALL.firstOrNull { it.id == id }
    }
}
