package com.aipose.camera.pose

import org.junit.Assert.*
import org.junit.Test

class AutoFramingTest {
    private val t = PoseTemplate.ALL.first()
    private fun step(g: AutoFraming, now: Long, points: Landmarks = t.points,
                     enabled: Boolean = true, settled: Boolean = true, zoom: Float = 1f) =
        g.evaluate(points,t,100,now,zoom,1f,3f,enabled,settled)
    @Test fun stableForRealTimeThenLocks() {
        val g = AutoFraming()
        for (time in 1000L..2500L step 100) assertFalse(step(g,time).shoot)
        assertTrue(step(g,2600).shoot)
        assertFalse(step(g,4500).shoot)
        assertFalse(g.armed)
        g.reset(rearm=true)
        for (time in 5000L..6600L step 100) step(g,time)
        assertFalse(g.armed)
    }
    @Test fun missingAnkleCancelsCountdown() {
        val g=AutoFraming()
        for(time in 1000L..2200L step 100) step(g,time)
        assertEquals(0f,step(g,2300,t.points-27).progress,0f)
        assertFalse(step(g,2600).shoot)
    }
    @Test fun frameGapCannotCompleteCountdown() {
        val g=AutoFraming(); step(g,1000)
        assertEquals(0f,step(g,3000).progress,0f)
    }
    @Test fun gradualMotionComparedWithAnchor() {
        val g=AutoFraming()
        for(i in 0..16) {
            val p=t.points.mapValues { (_,v)->v.first+i*.0015f to v.second }
            assertFalse(step(g,1000L+i*100,p).shoot)
        }
    }
    @Test fun disabledOrZoomingCannotCapture() {
        val g=AutoFraming()
        for(time in 1000L..4000L step 100) {
            assertFalse(step(g,time,enabled=false).shoot)
            assertFalse(step(g,time,settled=false).shoot)
        }
    }
    @Test fun smallPersonRequestsBoundedZoom() {
        val p=t.points.mapValues { (_,v)-> .5f+(v.first-.5f)*.6f to .5f+(v.second-.5f)*.6f }
        val d=step(AutoFraming(),1000,p)
        assertNotNull(d.zoom); assertTrue(d.zoom!! in 1f..1.06f); assertFalse(d.shoot)
    }
    @Test fun largePersonZoomsOutAndNeverPastHardwareMinimum() {
        val p=t.points.mapValues { (_,v)-> .5f+(v.first-.5f)*1.15f to .5f+(v.second-.5f)*1.15f }
        assertTrue(step(AutoFraming(),1000,p,zoom=2f).zoom!! < 2f)
        val d=step(AutoFraming(),1000,p)
        assertNull(d.zoom); assertFalse(d.shoot)
    }
    @Test fun offCenterCannotCapture() {
        val p=t.points.mapValues { (_,v)->v.first+.08f to v.second }
        val g=AutoFraming()
        for(time in 1000L..4000L step 100) assertFalse(step(g,time,p).shoot)
    }
    @Test fun invalidLandmarksCannotCapture() {
        val p=t.points.toMutableMap(); p[0]=Float.NaN to .2f
        val d=step(AutoFraming(),1000,p)
        assertFalse(d.shoot); assertNull(d.zoom); assertEquals(0f,d.progress,0f)
    }
    @Test fun lowScoreCannotCapture() {
        val g=AutoFraming()
        for(time in 1000L..4000L step 100) assertFalse(g.evaluate(t.points,t,40,time,1f,1f,3f,true,true).shoot)
    }
    @Test fun cropMappingExpandsInsteadOfCompressing() {
        val points = mapOf(0 to (.125f to .5f), 11 to (.875f to .5f))
        val result=PoseAligner.mapToDisplay(points,300f,400f,400f,400f)
        assertEquals(0f,result.getValue(0).first,.0001f)
        assertEquals(1f,result.getValue(11).first,.0001f)
    }
    @Test fun proportionRemainsConstantAcrossPreviewSizes() {
        fun proportion(w:Float,h:Float):Float {
            val p=t.forViewport(w,h).points
            return (p.getValue(12).first-p.getValue(11).first)*w / ((p.getValue(27).second-p.getValue(0).second)*h)
        }
        assertEquals(proportion(300f,400f),proportion(400f,400f),.0001f)
        assertEquals(proportion(300f,400f),proportion(300f,600f),.0001f)
    }
    @Test fun verticalCorrectionPointsTowardTarget() {
        val p=t.points.toMutableMap();p[15]=p.getValue(15).first to .3f
        assertTrue(PoseAligner.evaluate(t,p).tips.any { it.contains("放低") })
    }
}
