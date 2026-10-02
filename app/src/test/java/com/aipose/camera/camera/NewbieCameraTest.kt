package com.aipose.camera.camera

import com.aipose.camera.pose.PoseTemplate
import org.junit.Assert.*
import org.junit.Test

class NewbieCameraTest {
    private val face = FaceRegion(.35f,.20f,.65f,.55f)
    private val half = mapOf(0 to (.5f to .28f),11 to (.4f to .44f),12 to (.6f to .44f))
    private fun hint(points: Map<Int,Pair<Float,Float>>? = null, faces: List<FaceRegion> = listOf(face), count: Int? = 1,
                     intent: TravelIntent = TravelIntent.AUTO) = PortraitGuidance.next(points,faces,count,intent,"身体侧转，手臂自然放松")

    @Test fun nearFaceGetsGuidanceWithoutBodyOrZoom() {
        val h=hint();assertEquals(PortraitGuidance.Kind.FACE,h.kind);assertTrue(h.ready);assertTrue(h.message.contains("眼睛"))
    }
    @Test fun halfPortraitDoesNotRequireHipsOrAnkles() {
        val h=hint(half);assertEquals(PortraitGuidance.Kind.HALF,h.kind);assertTrue(h.ready)
    }
    @Test fun fullPortraitKeepsItsActionTip() {
        val h=hint(PoseTemplate.ALL.first().points,emptyList())
        assertEquals(PortraitGuidance.Kind.FULL,h.kind)
    }
    @Test fun oneMissingFootRequestsRoomInsteadOfAutomaticShot() {
        val h=hint(PoseTemplate.ALL.first().points-27,emptyList())
        assertFalse(h.ready);assertTrue(h.message.contains("四肢"))
    }
    @Test fun clippedFacePreventsAutomaticReadiness() {
        val h=hint(faces=listOf(FaceRegion(.2f,0f,.8f,.6f)))
        assertFalse(h.ready);assertTrue(h.message.contains("头顶"))
    }
    @Test fun invalidCoordinatesNeverProduceReadyHint() {
        val h=hint(half.mapValues{Float.NaN to it.value.second},listOf(FaceRegion(Float.NaN,.1f,.5f,.5f)))
        assertFalse(h.ready);assertEquals(PortraitGuidance.Kind.UNKNOWN,h.kind)
    }
    @Test fun absentAndUnknownCountsAreActionable() {
        assertFalse(hint(count=null).ready);assertTrue(hint(count=0).message.contains("进入画面"))
    }
    @Test fun environmentIntentUsesSidePlacement() {
        val h=hint(faces=listOf(FaceRegion(.64f,.2f,.94f,.5f)),intent=TravelIntent.ENVIRONMENT)
        assertFalse(h.ready);assertTrue(h.message.contains("向右转"))
    }
    @Test fun hiddenFaceInGroupDoesNotArmShot() {
        assertFalse(hint(count=2).ready)
        assertTrue(hint(faces=listOf(face,face.copy(left=.7f,right=.9f)),count=2).ready)
    }
    @Test fun faceGateUsesTimeAndResetsOnMotion() {
        val gate=FaceCaptureGate()
        for(t in 1000L..2400L step 200) assertFalse(gate.update(t,listOf(face),1,true).shoot)
        assertTrue(gate.update(2600,listOf(face),1,true).shoot)
        assertEquals(0f,gate.update(2800,listOf(face.copy(left=.4f,right=.7f)),1,true).progress,0f)
    }
    @Test fun faceGateRejectsMissingEvidenceAndFrameGaps() {
        val gate=FaceCaptureGate()
        assertFalse(gate.update(1000,listOf(face),1,false).shoot)
        gate.update(1200,listOf(face),1,true)
        assertEquals(0f,gate.update(4000,listOf(face),1,true).progress,0f)
        assertFalse(gate.update(4200,listOf(face),2,true).shoot)
        assertFalse(gate.update(4400,listOf(face),null,true).shoot)
    }
    @Test fun flatDarkAndInvalidPreviewsDoNotTriggerAutomaticCapture() {
        assertFalse(CaptureQuality.measure(FloatArray(1024){.5f}).permitsAutomatic)
        assertFalse(CaptureQuality.measure(FloatArray(1024){.01f}).permitsAutomatic)
        assertFalse(CaptureQuality.measure(FloatArray(1024){Float.NaN}).available)
        assertFalse(CaptureQuality.measure(FloatArray(100)).available)
    }
    @Test fun detailedNormalPreviewPermitsAutomaticCapture() {
        assertTrue(CaptureQuality.measure(FloatArray(1024){if(it%2==0) .35f else .65f}).permitsAutomatic)
    }
    @Test fun preserveSceneTargetsThirdsWithoutRequestingZoom() {
        assertEquals(1f/3,SubjectFraming.target(SubjectFraming.Box(.1f,.3f,.3f,.7f),true).x,.001f)
        assertEquals(2f/3,SubjectFraming.target(SubjectFraming.Box(.7f,.3f,.9f,.7f),true).x,.001f)
        assertEquals(.5f,SubjectFraming.target(SubjectFraming.Box(.1f,.3f,.9f,.7f),true).x,.001f)
        assertEquals(.5f,SubjectFraming.target(SubjectFraming.Box(.1f,.3f,.3f,.7f),false).x,.001f)
    }
    @Test fun zoomShortcutsRemainSmallEvenOnWideZoomRanges() {
        assertEquals(listOf(.5f,1f,2f),ZoomScale.quickPresets(.5f,10f))
        assertEquals(listOf(1f,2f),ZoomScale.quickPresets(1f,10f))
        assertEquals(listOf(1f),ZoomScale.quickPresets(1f,1f))
    }
    @Test fun backlitFaceLiftsShadowsWithoutRaisingClippedHighlights() {
        val optimizer=SceneOptimizer();val reading=SceneOptimizer.Reading(.65f,.2f,.22f)
        optimizer.update(reading,true,0f);optimizer.update(reading,true,0f)
        val choice=optimizer.update(reading,true,0f)!!
        assertTrue(choice.shadows>0);assertTrue(choice.highlights<0);assertTrue(choice.exposureEv<=0)
    }
    @Test fun faceBrightnessRequiresSamplesFromAnActualFaceRegion() {
        val pixels=IntArray(1024){0xffaaaaaa.toInt()}
        assertNull(SceneOptimizer.measure(pixels).faceMean)
        assertNotNull(SceneOptimizer.measure(pixels,listOf(face)).faceMean)
        assertNull(SceneOptimizer.measure(pixels,listOf(FaceRegion(.5f,.5f,.501f,.501f))).faceMean)
    }
}
