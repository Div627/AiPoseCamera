package com.aipose.camera.camera

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class CompositionTrackingTest {
    private val box=SubjectFraming.Box(.25f,.25f,.50f,.50f)
    private fun texture(seed:Int)=Random(seed).let{r->FloatArray(1024){r.nextFloat()}}
    @Test fun followsTranslationInPreviewCoordinates() {
        val tracker=CompositionTracking();val frame=texture(1)
        assertNotNull(tracker.update(frame,box))
        val moved=FloatArray(1024){i->if(i%32>=3 && i/32>=2) frame[(i/32-2)*32+i%32-3] else 0f}
        val result=tracker.update(moved,box)!!
        assertEquals(box.cx+3f/32,result.cx,.001f)
        assertEquals(box.cy+2f/32,result.cy,.001f)
    }
    @Test fun rejectsFlatTextureAndOccludedSubject() {
        assertNull(CompositionTracking().update(FloatArray(1024){.5f},box))
        val tracker=CompositionTracking();tracker.update(texture(2),box)
        assertNull(tracker.update(texture(3),box))
    }
    @Test fun rejectsRepeatingTextureInsteadOfGuessing() {
        val frame=FloatArray(1024){if((it%32+it/32)%2==0) .1f else .9f}
        assertNull(CompositionTracking().update(frame,box))
    }
    @Test fun resetAllowsNewSubjectAndRejectsMissingFrame() {
        val tracker=CompositionTracking();tracker.update(texture(4),box);tracker.reset()
        val other=SubjectFraming.Box(.5f,.5f,.8f,.8f)
        assertEquals(other,tracker.update(texture(5),other))
        assertNull(tracker.update(floatArrayOf(),other))
    }
    @Test fun offCenterSubjectNeverTriggersUnsafeZoom() {
        assertNull(SubjectFraming.choose(SubjectFraming.Box(.7f,.3f,.95f,.6f),1f,1f,10f).zoom)
        val decision=SubjectFraming.choose(SubjectFraming.Box(.35f,.35f,.65f,.65f),2f,1f,10f)
        assertTrue(decision.zoom!!<=3f)
    }
}
