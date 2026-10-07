package com.aipose.camera.assistant

import com.aipose.camera.camera.CameraMode
import com.aipose.camera.camera.PhotoStyle
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()
data class ChatMessage(val id: String = newId(), val role: String, val text: String)
data class Shot(val id: String, val title: String, val direction: String)
data class ShootingPlan(
    val id: String = newId(), val kind: String, val title: String,
    val description: String, val style: PhotoStyle = PhotoStyle.SCENIC,
    val mode: CameraMode = CameraMode.LANDSCAPE, val shots: List<Shot> = emptyList()
)
data class LocalClip(val shotId: String, val file: String, val durationMs: Long)
data class ShootingProject(
    val id: String = newId(), val title: String = "新的拍摄想法", val updated: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList(), val plan: ShootingPlan? = null,
    val clips: List<LocalClip> = emptyList(), val exports: List<String> = emptyList(),
    val photos: List<String> = emptyList()
)
data class ProjectSummary(val id: String, val title: String, val updated: Long)
data class AssistantReply(val text: String, val plan: ShootingPlan)

/** Offline recipes are labelled as local planning, never represented as a model response. */
object LocalShootingPlanner {
    fun respond(text: String, previous: ShootingPlan? = null): AssistantReply {
        val video = listOf("vlog", "视频", "分镜", "剪辑").any { text.lowercase().contains(it) } ||
            previous?.kind == "video" && !text.contains("照片") && !text.contains("拍照")
        val portrait = listOf("人像", "自拍", "合照").any(text::contains)
        val style = when {
            text.contains("黑白") -> PhotoStyle.A_APX
            text.contains("暖") -> PhotoStyle.K_GOLD
            text.contains("冷") || text.contains("孤独") -> PhotoStyle.ICELAND
            portrait -> PhotoStyle.PORTRAIT
            else -> PhotoStyle.SCENIC
        }
        if (!video) {
            val direction = if (portrait) "先给头顶和四肢留空间，选择柔和光线；取景后根据人脸位置调整。"
                else "用一处岩石、小路或植物作前景，留住远景亮部；检查画面四边后按快门。"
            val title = if (portrait) "自然人像" else if (style == PhotoStyle.ICELAND) "克制冷调风景" else "自然纪实风景"
            val plan = ShootingPlan(kind = "photo", title = title, description = direction, style = style,
                mode = if (portrait) CameraMode.PORTRAIT else CameraMode.LANDSCAPE)
            return AssistantReply("先从**自然光和简单构图**开始。选好下面的风格就能拍；需要找地点时，点“去哪里拍”。", plan)
        }
        val transition = text.contains("孤独") || text.contains("生命") || text.contains("生机")
        val shots = if (transition) listOf(
            Shot("wide", "01 · 空旷远景", "固定手机，让人物很小或暂不入镜；录 6–10 秒，保留环境声。"),
            Shot("walk", "02 · 独自前行", "从背后拍一段步行，手机平稳，不追着快速移动。"),
            Shot("detail", "03 · 光与细节", "拍光落在岩石、叶片或水面上；缓慢靠近，留下变化。"),
            Shot("motion", "04 · 生命的流动", "选择流水、风吹树叶或当地生活，拍完整动作。"),
            Shot("person", "05 · 与环境互动", "拍人物停下、抬头或自然微笑，不需要表演夸张动作。"),
            Shot("ending", "06 · 开阔收尾", "固定一个开阔画面或继续前行的背影，结尾多留两秒。")
        ) else listOf(
            Shot("wide", "01 · 到达这里", "拍清环境的大景，固定手机，录 6–10 秒。"),
            Shot("walk", "02 · 进入场景", "拍走路、开门或转身，留下动作开始和结束。"),
            Shot("detail", "03 · 发现细节", "选一个让你停下的细节，慢慢靠近，不反复变焦。"),
            Shot("motion", "04 · 当地的声音", "录一段水、风或生活动态，保留自然声。"),
            Shot("person", "05 · 你的片刻", "拍自己的背影或自然互动，让观众知道你在这里。"),
            Shot("ending", "06 · 留一点余韵", "拍一个稳定的收尾画面，动作结束后再多录两秒。")
        )
        val title = if (transition) "从孤独，到有生命力" else "我的旅行片刻"
        val plan = ShootingPlan(kind = "video", title = title,
            description = "六个镜头逐段填充；每段录 6–10 秒。合成取每段最多 5 秒，保留原声和原素材，可跳过或替换。", shots = shots)
        return AssistantReply("先用**远景**建立环境，再用细节与动作推进。按下面的镜头逐段拍，也可以从相册选素材；至少两段就能生成草稿。", plan)
    }
}
