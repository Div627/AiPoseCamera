package com.aipose.camera.camera

import com.aipose.camera.pose.GroupPoses
import com.aipose.camera.pose.PoseAtlas
import com.aipose.camera.pose.PoseTemplate
import com.aipose.camera.llm.PhotographyGuide
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SceneModeTest {
    private val signature=FloatArray(256){if(it%2==0) .3f else .7f}
    @Test fun startupAndInferenceFailureAreNotLandscape() {
        val s=ScenePresence();assertNull(s.people)
        for(time in 1000L..6000L step 200) assertNull(s.update(null,time))
        assertFalse(s.settled)
    }
    @Test fun zeroNeedsContinuousEvidence() {
        val s=ScenePresence()
        for(time in 1000L..3000L step 200) assertNull(s.update(0,time))
        assertEquals(0,s.update(0,3200));assertTrue(s.settled)
    }
    @Test fun transientPersonLossDoesNotFlipToLandscape() {
        val s=ScenePresence()
        for(time in 1000L..2200L step 200) s.update(1,time)
        assertEquals(1,s.update(0,2400));assertFalse(s.settled)
        assertEquals(1,s.update(1,2600));assertFalse(s.settled)
    }
    @Test fun uncertainDetectionClearsCountRatherThanInventingZero() {
        val s=ScenePresence();for(time in 1000L..2200L step 200) s.update(2,time)
        assertNull(s.update(null,2400));assertFalse(s.settled)
    }
    @Test fun longGapRestartsModeConfirmation() {
        val s=ScenePresence();s.update(0,1000);assertNull(s.update(0,5000))
    }
    @Test fun personCountChangesNeedConfirmation() {
        val s=ScenePresence();for(time in 1000L..2200L step 200) s.update(1,time)
        for(time in 2400L..3400L step 200) {assertEquals(1,s.update(3,time));assertFalse(s.settled)}
        assertEquals(3,s.update(3,3600));assertTrue(s.settled)
    }
    @Test fun overflowIsExplicitAndNoFictionalPoseCount() {
        assertTrue(GroupPoses.options(5).isEmpty());assertTrue(ScenePresence.label(5).contains("至少5"))
    }
    @Test fun everyGroupOptionMatchesActualCount() {
        for(count in 1..4) for(option in GroupPoses.options(count)) {
            assertEquals(count,option.slots.size)
            assertTrue(option.slots.all{it.pose in PoseTemplate.ALL.indices})
            assertEquals(count,GroupPoses.targets(option,400f,500f).size)
        }
        assertTrue(GroupPoses.options(0).isEmpty())
    }
    @Test fun groupSlotsHaveOrderedSpacingAndInsideFrame() {
        for(count in 2..4) for(option in GroupPoses.options(count)) {
            assertTrue(option.slots.zipWithNext().all{(a,b)->b.x-a.x>.15f})
            for(target in GroupPoses.targets(option,400f,500f)) assertTrue(target.points.values.all{it.first in 0f..1f && it.second in 0f..1f})
        }
    }
    @Test fun changingOptionChangesComposition() {
        val options=GroupPoses.options(3)
        assertNotEquals(options[0].slots,options[1].slots)
    }
    @Test fun atlasTargetsHaveAllPoseLandmarks() {
        for(i in PoseTemplate.ALL.indices) assertEquals(13,PoseAtlas.points(i).size)
    }
    @Test fun landscapeCanShootWithoutAnyHuman() {
        val gate=SceneCaptureGate();var shot=false
        for(time in 1000L..3600L step 200) if(gate.update(time,0,true,signature,emptyList(),true).shoot) shot=true
        assertTrue(shot)
    }
    @Test fun movingLandscapeResetsCountdown() {
        val gate=SceneCaptureGate()
        for(time in 1000L..2800L step 200) gate.update(time,0,true,signature,emptyList(),true)
        val changed=signature.map{it+.12f}.toFloatArray()
        assertEquals(0f,gate.update(3000,0,true,changed,emptyList(),true).progress,0f)
    }
    @Test fun unknownAndExposureTransitionBlockAutomaticShot() {
        val g=SceneCaptureGate()
        for(time in 1000L..6000L step 200) {
            assertFalse(g.update(time,null,false,signature,emptyList(),true).shoot)
            assertFalse(g.update(time,0,true,signature,emptyList(),false).shoot)
        }
    }
    @Test fun blankOrBlackSceneRequiresManualFallback() {
        val g=SceneCaptureGate()
        assertFalse(g.update(6000,0,true,FloatArray(256),emptyList(),true).shoot)
        assertEquals(0f,g.update(6200,0,true,FloatArray(256){.5f},emptyList(),true).progress,0f)
    }
    @Test fun groupCaptureUsesVisibilityNotSinglePoseScore() {
        val people=GroupPoses.targets(GroupPoses.options(2)[0],400f,500f).map{it.points}
        val g=SceneCaptureGate();var fired=false
        for(time in 1000L..3400L step 200) if(g.update(time,2,true,signature,people,true).shoot) fired=true
        assertTrue(fired)
    }
    @Test fun missingGroupAnkleBlocksShot() {
        val people=listOf(PoseTemplate.ALL[0].points,PoseTemplate.ALL[1].points-27)
        val g=SceneCaptureGate()
        for(time in 1000L..5000L step 200) assertFalse(g.update(time,2,true,signature,people,true).shoot)
    }
    @Test fun groupZoomIsRateLimitedAndHardwareBounded() {
        val people=GroupPoses.targets(GroupPoses.options(2)[0],400f,500f).map{p->p.points.mapValues{(_,v)->(.5f+(v.first-.5f)*.5f) to (.5f+(v.second-.5f)*.5f)}}
        val value=GroupFraming.zoom(people,1f,1f,3f)
        assertNotNull(value);assertTrue(value!! in 1f..1.06f)
        assertNull(GroupFraming.zoom(people,3f,1f,3f))
    }
    @Test fun modelResourceIsVersionedAndIncludesSceneAndCapabilityLimits() {
        val text=File("src/main/assets/photography/v2/photography-guide.txt").readText()
        val system=PhotographyGuide.systemPrompt(text)
        assertTrue(system.contains("多人"));assertTrue(system.contains("风景"));assertTrue(system.contains("没有接收照片"));assertTrue(system.contains("ISO"))
        assertTrue(File("src/main/assets/photography/v2/SOURCES.txt").readText().contains("fujifilm-x.com"))
        assertTrue(system.contains("阴影"));assertFalse(system.contains("apply-rule-of-thirds"))
    }
    @Test fun landscapePromptDoesNotIncludeHiddenPortraitPose() {
        val text=PhotographyGuide.sceneContext(0,true,"通透风景",0f,"单手叉腰")
        assertFalse(text.contains("单手叉腰"));assertTrue(text.contains("风景/非人像"))
        assertTrue(PhotographyGuide.sceneContext(null,false,"原色",0f,null).contains("未知"))
    }
}
