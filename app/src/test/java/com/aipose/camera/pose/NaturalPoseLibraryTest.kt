package com.aipose.camera.pose

import com.aipose.camera.camera.*
import org.junit.Assert.*
import org.junit.Test

class NaturalPoseLibraryTest {
    @Test fun twentyFourDifferentDrawingsAndTargetsCoverEveryCategory() {
        val all=PoseTemplate.ALL
        assertEquals(24,all.size);assertEquals(24,all.map{it.id}.toSet().size)
        assertEquals(24,all.map{it.points}.toSet().size)
        assertEquals(mapOf(PoseCategory.STANDING to 6,PoseCategory.SEATED to 6,PoseCategory.LEANING to 3,PoseCategory.LOW to 3,PoseCategory.DYNAMIC to 6),all.groupingBy{it.category}.eachCount())
        all.forEach {t->assertEquals(13,t.points.size);assertTrue(t.points.values.all{it.first in .025f.. .975f && it.second in .025f.. .975f})}
    }
    @Test fun filtersAndSlotsResolveToTheSameReference() {
        for(count in 1..4) for(category in PoseCategory.entries) {
            val options=GroupPoses.options(count,category);assertTrue(options.isNotEmpty())
            options.forEach {o->
                assertEquals(count,o.slots.size)
                assertTrue(o.slots.all{PoseTemplate.ALL[it.pose].category==category})
                assertEquals(1,o.slots.map{PoseTemplate.ALL[it.pose].support}.distinct().size)
                assertEquals(o.support,PoseTemplate.ALL[o.slots.first().pose].support)
                assertEquals(o.slots.map{PoseTemplate.ALL[it.pose].id},GroupPoses.targets(o,300f,400f).map{it.id})
            }
        }
    }
    @Test fun allPosesIncludingSeatedAndSquattingCanFinishStableCapture() {
        for(t in PoseTemplate.ALL) {
            val g=AutoFraming();var fired=false
            for(time in 1000L..3000L step 100) {
                val d=g.evaluate(t.points,t,PoseAligner.evaluate(t,t.points).score,time,1f,1f,3f,true,true)
                assertNull("unexpected zoom ${t.name}",d.zoom)
                if(d.shoot) fired=true
            }
            assertTrue("cannot reach capture ${t.name}",fired)
        }
    }
    @Test fun changingPoseDuringCountdownRestartsIt() {
        val a=PoseTemplate.ALL[6];val b=PoseTemplate.ALL[15];val g=AutoFraming()
        for(time in 1000L..2400L step 100) g.evaluate(a.points,a,100,time,1f,1f,3f,true,true)
        assertEquals(0f,g.evaluate(b.points,b,100,2500,1f,1f,3f,true,true).progress,0f)
        assertFalse(g.evaluate(b.points,b,100,2600,1f,1f,3f,true,true).shoot)
    }
    @Test fun lowConfidenceSeatedLimbsNeverTriggerZoomOrShutter() {
        for(t in PoseTemplate.ALL.filter{it.category==PoseCategory.SEATED || it.category==PoseCategory.LOW}) {
            val g=AutoFraming()
            for(time in 1000L..5000L step 200) {
                val d=g.evaluate(t.points-27,t,100,time,1f,1f,3f,true,true)
                assertFalse(d.shoot);assertNull(d.zoom);assertTrue(d.tip.contains("手动"))
            }
        }
    }
    @Test fun frozenSingleDetectorStopsZoomingRatherThanLooping() {
        val t=PoseTemplate.ALL[15];val p=t.points.mapValues{(_,p)->.5f+(p.first-.5f)*.5f to .5f+(p.second-.5f)*.5f}
        val g=AutoFraming();var requests=0;var last=AutoFraming.Decision("")
        for(time in 1000L..18000L step 600) {last=g.evaluate(p,t,100,time,1f,1f,3f,true,true);if(last.zoom!=null) requests++}
        assertEquals(18,requests);assertTrue(last.tip.contains("暂停"));assertFalse(last.shoot)
    }
    @Test fun everyGroupFitsAndUsesItsOwnCompactHeight() {
        for(count in 2..4) for(option in GroupPoses.options(count)) {
            val targets=GroupPoses.targets(option,300f,400f)
            assertTrue(targets.flatMap{it.points.values}.all{it.first in .025f.. .975f && it.second in .025f.. .975f})
            assertNull(GroupFraming.zoom(targets.map{it.points},1f,1f,3f,targets))
        }
    }
    @Test fun seatedGroupsCanShootAndMissingAnklesStopThem() {
        val sig=FloatArray(256){if(it%2==0) .3f else .7f}
        for(count in 2..4) for(category in listOf(PoseCategory.SEATED,PoseCategory.LOW)) {
            val p=GroupPoses.targets(GroupPoses.options(count,category).first(),300f,400f).map{it.points}
            val g=SceneCaptureGate();var fired=false
            for(time in 1000L..3400L step 200) if(g.update(time,count,true,sig,p,true).shoot) fired=true
            assertTrue(fired)
            assertFalse(g.update(3600,count,true,sig,p.map{it-27},true).shoot)
        }
    }
    @Test fun frozenGroupDetectorStopsZoomAndSelectionCanResetBudget() {
        val targets=GroupPoses.targets(GroupPoses.options(2,PoseCategory.SEATED).first(),300f,400f)
        val p=targets.map{t->t.points.mapValues{(_,v)->.5f+(v.first-.5f)*.5f to .5f+(v.second-.5f)*.5f}}
        val s=GroupZoomSession()
        repeat(18){assertNotNull(s.evaluate(p,1f,1f,3f,targets).zoom)}
        assertTrue(s.evaluate(p,1f,1f,3f,targets).blocked)
        s.reset();assertFalse(s.evaluate(p,1f,1f,3f,targets).blocked)
    }
    @Test fun portraitAspectChangesPreserveSittingProportions() {
        val t=PoseTemplate.ALL[11]
        fun ratio(w:Float,h:Float):Float {val p=t.forViewport(w,h).points.values;return (p.maxOf{it.first}-p.minOf{it.first})*w/((p.maxOf{it.second}-p.minOf{it.second})*h)}
        assertEquals(ratio(300f,400f),ratio(400f,400f),.0001f)
        assertEquals(ratio(300f,400f),ratio(300f,650f),.0001f)
    }
}
