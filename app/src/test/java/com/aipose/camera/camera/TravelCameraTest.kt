package com.aipose.camera.camera

import com.aipose.camera.pose.*
import org.junit.Assert.*
import org.junit.Test

class TravelCameraTest {
    @Test fun everyTimerUsesDeadlineAndOnlyFiresOnce() {
        for(seconds in listOf(0,2,5,10)) {
            val g=CountdownGate();g.start(seconds,1000)
            if(seconds>0) {assertEquals(seconds,g.remaining(1000));assertFalse(g.consume(1000L+seconds*1000L-1))}
            assertTrue(g.consume(1000L+seconds*1000L));assertFalse(g.consume(50000))
        }
    }
    @Test fun cancelAndRestartCannotFireAnOldCountdown() {
        val g=CountdownGate();g.start(10,1000);g.cancel();assertFalse(g.consume(99999))
        g.start(2,2000);g.start(5,2100);assertFalse(g.consume(4000));assertTrue(g.consume(7100))
    }
    @Test fun timerIsNotFrameCountDependent() {
        val g=CountdownGate();g.start(5,1000);assertEquals(2,g.remaining(4999));assertTrue(g.consume(9000));assertFalse(g.consume(9001))
    }
    @Test fun levelIsMeasuredOnlyWhenGravityProjectionIsMeaningful() {
        assertEquals(0f,TravelGuidance.roll(0f,9.81f)!!,.001f)
        assertEquals(45f,TravelGuidance.roll(6.9f,6.9f)!!,.001f)
        assertNull(TravelGuidance.roll(0f,.3f));assertNull(TravelGuidance.roll(Float.NaN,8f))
    }
    @Test fun offCenterAndTinySelectionsNeverPretendToPanZoom() {
        assertNull(SubjectFraming.choose(SubjectFraming.Box(.02f,.05f,.2f,.3f),1f,1f,8f).zoom)
        assertNull(SubjectFraming.choose(SubjectFraming.Box(.49f,.49f,.51f,.51f),1f,1f,8f).zoom)
        assertNull(SubjectFraming.choose(SubjectFraming.Box(Float.NaN,.2f,.5f,.8f),1f,1f,8f).zoom)
    }
    @Test fun selectionZoomRespectsFrameEdgesHardwareAndQualityCap() {
        val box=SubjectFraming.Box(.35f,.35f,.65f,.65f)
        assertEquals(2f,SubjectFraming.choose(box,1f,1f,8f).zoom!!,.001f)
        assertEquals(1.4f,SubjectFraming.choose(box,1f,1f,1.4f).zoom!!,.001f)
        assertEquals(3f,SubjectFraming.choose(box,2f,1f,8f).zoom!!,.001f)
        assertNull(SubjectFraming.choose(box,3f,1f,8f).zoom)
    }
    @Test fun centeredRegionRemainsWithinSafeViewportAfterChosenZoom() {
        val box=SubjectFraming.Box(.3f,.25f,.72f,.70f)
        val z=SubjectFraming.choose(box,1f,1f,5f).zoom!!
        assertTrue(.5f+(box.left-.5f)*z>=.06f)
        assertTrue(.5f+(box.right-.5f)*z<=.94f)
        assertTrue(.5f+(box.top-.5f)*z>=.09f)
        assertTrue(.5f+(box.bottom-.5f)*z<=.91f)
    }
    @Test fun guidanceSeparatesUserIntentAndMeasuredTilt() {
        assertTrue(TravelGuidance.next(TravelIntent.MOUNTAIN,0,null,8f,"").contains("左右倾斜"))
        assertEquals(TravelIntent.MOUNTAIN.advice,TravelGuidance.next(TravelIntent.MOUNTAIN,0,null,0f,""))
        assertTrue(TravelGuidance.next(TravelIntent.COAST,null,null,null,"").contains("不确定"))
        assertTrue(TravelIntent.COAST.advice.contains("不向浪区"))
    }
    @Test fun referencePreferenceReordersWithoutInferringOrRemovingPoses() {
        for(style in ReferenceStyle.entries) assertEquals(PoseTemplate.ALL.map{it.id}.toSet(),GroupPoses.options(1,reference=style).map{PoseTemplate.ALL[it.slots.single().pose].id}.toSet())
        assertNotEquals(GroupPoses.options(1,reference=ReferenceStyle.MASCULINE).first(),GroupPoses.options(1,reference=ReferenceStyle.FEMININE).first())
    }
    @Test fun twoPersonInteractionUsesRealMirroringAndStaggeredLevels() {
        val pair=GroupPoses.options(2).first{it.name=="回身交流"}
        assertTrue(pair.slots.last().mirror)
        val targets=GroupPoses.targets(pair,300f,400f)
        assertTrue(targets.all{it.points.values.all{p->p.first in 0f..1f && p.second in 0f..1f}})
        val seated=GroupPoses.options(2).first{it.name=="坐着聊旅途"}
        assertNotEquals(seated.slots[0].y,seated.slots[1].y)
    }
    @Test fun originalPixelsAndZeroStrengthStayUnchanged() {
        val g=ColorGrade();val pixel=0xffa07951.toInt()
        assertEquals(pixel,g.transform(pixel,g.matrix(PhotoStyle.ORIGINAL),g.lut()))
        val zero=g.copy(strength=0f)
        assertEquals(pixel,zero.transform(pixel,zero.matrix(PhotoStyle.ICELAND),zero.lut()))
        assertFalse(g.needsCopy(PhotoStyle.ORIGINAL));assertFalse(zero.needsCopy(PhotoStyle.ICELAND))
        assertTrue(g.copy(shadows=.5f).needsCopy(PhotoStyle.ORIGINAL))
    }
    @Test fun shadowsAndHighlightsAreRealBoundedMonotonicCurves() {
        for(sh in listOf(-1f,0f,1f)) for(hi in listOf(-1f,0f,1f)) {
            val lut=ColorGrade(shadows=sh,highlights=hi).lut()
            assertTrue(lut.all{it in 0..255});assertTrue(lut.toList().zipWithNext().all{(a,b)->a<=b})
        }
        assertTrue(ColorGrade(shadows=1f).curve(.1f)>.1f)
        assertTrue(ColorGrade(highlights=-1f).curve(.9f)<.9f)
    }
    @Test fun postExposureAndTintReallyChangePixels() {
        val pixel=0xff404040.toInt();val g=ColorGrade(postEv=1f)
        assertEquals(128,g.transform(pixel,g.matrix(PhotoStyle.ORIGINAL),g.lut()) and 255)
        val t=ColorGrade(tint=1f);val p=t.transform(pixel,t.matrix(PhotoStyle.ORIGINAL),t.lut())
        assertTrue((p ushr 16 and 255)>(p ushr 8 and 255))
    }
    @Test fun copyFailureRollsBackOnlyTheCopyAndLeavesOriginal() {
        val files=mutableSetOf("original");val actions=mutableListOf<String>()
        try {EditedCopyTransaction.publish("original",{files.add("copy");"copy"},{actions.add("write:$it");error("full disk")},{actions.add("commit:$it")},{files.remove(it);actions.add("delete:$it")});fail()}
        catch(_:IllegalStateException){}
        assertEquals(setOf("original"),files);assertEquals(listOf("write:copy","delete:copy"),actions)
    }
    @Test fun accidentalOriginalUriCannotBeWrittenOrDeleted() {
        val actions=mutableListOf<String>()
        try {EditedCopyTransaction.publish("original",{"original"},{actions.add("write")},{actions.add("commit")},{actions.add("delete")});fail()}
        catch(_:IllegalArgumentException){}
        assertTrue(actions.isEmpty())
    }
    @Test fun successfulCopyCommitsAfterWritingAndRetainsBothFiles() {
        val actions=mutableListOf<String>()
        assertEquals("copy",EditedCopyTransaction.publish("original",{actions.add("create");"copy"},{actions.add("write")},{actions.add("commit")},{actions.add("delete")}))
        assertEquals(listOf("create","write","commit"),actions)
    }
}
