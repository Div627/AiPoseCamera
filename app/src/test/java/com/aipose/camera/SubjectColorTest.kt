package com.aipose.camera

import com.aipose.camera.camera.*
import org.junit.Assert.*
import org.junit.Test

class SubjectColorTest {
    private fun shape()=listOf(.02f to .02f,.25f to .02f,.46f to .02f,.46f to .48f,.25f to .48f,.02f to .48f).map{SubjectColor.Point(it.first,it.second)}
    private fun reading(mean:Float=.5f,high:Float=0f,sat:Float=.3f,cool:Float=0f,signature:Float=.5f)=SubjectColor.Reading(SubjectColor.Stats(mean,high,sat,cool),SubjectColor.Stats(.5f,high,.3f,0f),FloatArray(64){signature})
    @Test fun offCenterRegionIsAcceptedIndependentlyOfZoom(){
        val r=SubjectColor.region(shape())!!
        assertTrue(r.mask.count{it}>150)
        assertNull(SubjectFraming.choose(SubjectFraming.Box(.02f,.02f,.46f,.48f),1f,1f,3f).zoom)
    }
    @Test fun polygonUsesConcaveMaskRatherThanBoundingRectangle(){
        val p=listOf(.1f to .1f,.8f to .1f,.8f to .3f,.3f to .3f,.3f to .8f,.1f to .8f).map{SubjectColor.Point(it.first,it.second)}
        assertTrue(SubjectColor.contains(p,.2f,.6f));assertFalse(SubjectColor.contains(p,.6f,.6f))
    }
    @Test fun tinyInvalidAndNonFinitePathsRejected(){
        assertNull(SubjectColor.region(emptyList()))
        assertNull(SubjectColor.region(shape().map{SubjectColor.Point(it.x/100,it.y/100)}))
        assertNull(SubjectColor.region(shape()+SubjectColor.Point(Float.NaN,.5f)))
        assertNull(SubjectColor.region(shape()+SubjectColor.Point(1.1f,.5f)))
    }
    @Test fun previewMirrorMapsToOppositeImageColumn(){
        for(i in 0..31) assertEquals(639,SubjectColor.sampleX(i,640,false)+SubjectColor.sampleX(i,640,true))
        assertEquals(10,SubjectColor.sampleX(0,640,false))
        assertEquals(630,SubjectColor.sampleX(31,640,false))
    }
    @Test fun statsOnlyCountMaskButAlsoProtectFullFrame(){
        val r=SubjectColor.region(shape())!!
        val px=IntArray(1024){if(r.mask[it]) 0xff202040.toInt() else 0xffffffff.toInt()}
        val measured=SubjectColor.measure(px,r)
        assertTrue(measured.subject.mean<.2f);assertEquals(0f,measured.subject.highlights,0f)
        assertTrue(measured.full.highlights>.5f)
        assertEquals(PhotoStyle.HIGHLIGHT,SubjectColor.choose(measured).style)
    }
    @Test fun automaticColorStaysNaturalInsteadOfAddingAColdLook(){
        assertEquals(PhotoStyle.SCENIC,SubjectColor.choose(reading(cool=.15f)).style)
        assertEquals(PhotoStyle.ORIGINAL,SubjectColor.choose(reading(sat=.8f)).style)
        assertEquals(PhotoStyle.SOFT,SubjectColor.choose(reading(mean=.2f)).style)
    }
    @Test fun consecutiveReadingsAndRateLimitPreventFlicker(){
        val s=SubjectColor.Session()
        assertNull(s.update(reading(),0,true));assertNull(s.update(reading(),200,true))
        assertNotNull(s.update(reading(),400,true));assertNull(s.update(reading(),600,true))
        assertNotNull(s.update(reading(),2200,true))
    }
    @Test fun manualOverrideIsNeverReplaced(){
        val s=SubjectColor.Session();repeat(20){assertNull(s.update(reading(),it*2000L,false))}
    }
    @Test fun sceneChangeInvalidatesSelectionAndNewSessionClearsIt(){
        val s=SubjectColor.Session();s.update(reading(),0,true)
        repeat(3){s.update(reading(signature=.9f),1000L+it*500,true)}
        assertTrue(s.invalidated);assertNull(s.update(reading(),5000,true))
        assertFalse(SubjectColor.Session().invalidated)
    }
    @Test fun cancelAndLensChangesCanDiscardRegionWithoutMutatingPixels(){
        val region=java.util.concurrent.atomic.AtomicReference(SubjectColor.region(shape()))
        val pixels=IntArray(1024){0xff808080.toInt()};val original=pixels.copyOf()
        SubjectColor.measure(pixels,region.get()!!);region.set(null)
        assertNull(region.get());assertArrayEquals(original,pixels)
    }
    @Test fun recommendedGradeUsesSameMatrixForPreviewAndSavedPixels(){
        val c=SubjectColor.choose(reading(cool=.15f));val g=ColorGrade(strength=c.strength)
        assertTrue(g.needsCopy(c.style));val pixel=0xff667788.toInt()
        assertEquals(PhotoStyle.transform(pixel,g.matrix(c.style)),g.transform(pixel,g.matrix(c.style),g.lut()))
    }
}
