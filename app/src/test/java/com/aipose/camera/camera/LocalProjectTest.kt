package com.aipose.camera.camera

import com.aipose.camera.assistant.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LocalProjectTest {
    @Test fun destinationAloneDoesNotForceColdColor() {
        val plan=LocalShootingPlanner.respond("我到了冰岛，想拍自然风景照").plan
        assertEquals("photo",plan.kind)
        assertEquals(PhotoStyle.SCENIC,plan.style)
        assertEquals(CameraMode.LANDSCAPE,plan.mode)
    }
    @Test fun explicitStyleAndSubjectAreActionable() {
        val plan=LocalShootingPlanner.respond("想拍冷调人像").plan
        assertEquals(CameraMode.PORTRAIT,plan.mode)
        assertEquals(PhotoStyle.ICELAND,plan.style)
    }
    @Test fun videoEmotionBecomesStableFillableShots() {
        val plan=LocalShootingPlanner.respond("个人vlog，从孤独到有生命力").plan
        assertEquals("video",plan.kind)
        assertEquals(6,plan.shots.size)
        assertEquals(6,plan.shots.map {it.id}.distinct().size)
        assertTrue(plan.shots.first().title.contains("远景"))
        assertTrue(plan.shots[3].title.contains("生命"))
        assertTrue(LocalShootingPlanner.respond("改成暖一点",plan).plan.kind=="video")
        assertEquals("photo",LocalShootingPlanner.respond("现在拍照片",plan).plan.kind)
    }
    @Test fun localRoundTripKeepsDialogMediaAndExportAssociations() {
        val plan=LocalShootingPlanner.respond("旅行vlog").plan
        val project=ShootingProject(id="local-project",messages=listOf(ChatMessage(role="user",text="旅行"),ChatMessage(role="assistant",text="## 分镜\n\n保留原声")),plan=plan,
            clips=listOf(LocalClip(plan.shots.first().id,"/local/clip.mp4",7000)),exports=listOf("/local/export.mp4"),photos=listOf("content://photo/42"))
        assertEquals(project,ProjectCodec.decode(ProjectCodec.encode(project)))
    }
    @Test fun malformedRemotePlanCannotCreateDuplicateShotActions() {
        val objectValue=ProjectCodec.planJson(LocalShootingPlanner.respond("vlog").plan)
        val shots=objectValue.getJSONArray("shots")
        shots.getJSONObject(1).put("id",shots.getJSONObject(0).getString("id"))
        assertThrows(IllegalArgumentException::class.java) {ProjectCodec.plan(objectValue)}
    }
    @Test fun remoteUnknownStyleFallsBackToSupportedStyle() {
        val objectValue=ProjectCodec.planJson(LocalShootingPlanner.respond("拍照").plan).put("style","arbitrary-system-command")
        assertEquals(PhotoStyle.SCENIC,ProjectCodec.plan(objectValue).style)
        assertThrows(IllegalArgumentException::class.java) {ProjectCodec.plan(objectValue.put("kind","execute"))}
    }
    @Test fun localHistorySupportsLongMarkdownWithoutChangingContent() {
        val content="## 旅途\n\n**自然光**与[来源](https://example.org)\n\n```\n尚未闭合\n".repeat(100)
        val p=ShootingProject(messages=List(500){ChatMessage(role=if(it%2==0) "user" else "assistant",text=content.take(1200))})
        assertEquals(p,ProjectCodec.decode(ProjectCodec.encode(p)))
    }
}
