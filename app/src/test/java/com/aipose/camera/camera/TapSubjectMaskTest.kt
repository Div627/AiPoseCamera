package com.aipose.camera.camera

import org.junit.Assert.*
import org.junit.Test

class TapSubjectMaskTest {
    private val n=TapSubjectMask.GRID
    private fun confidence(predicate:(Int,Int)->Boolean)=FloatArray(n*n){if(predicate(it%n,it/n)) .95f else .05f}
    @Test fun selectsOnlyObjectUnderTapAndKeepsConcaveEdges() {
        val mask=confidence {x,y->(x in 12..36 && y in 16..50 && !(x>24 && y<30)) || (x in 65..85 && y in 50..80)}
        val result=TapSubjectMask.select(mask,n,n,.2f,.4f)!!
        assertTrue(result.box.right<.5f)
        assertTrue(result.edges.any {it.from.x in .24f.. .27f && it.from.y in .16f.. .32f})
        assertFalse(result.region.mask[22*32+24])
    }
    @Test fun backgroundTapDoesNotSelectNearbyUnrelatedObject() {
        assertNull(TapSubjectMask.select(confidence {x,y->x in 15..35 && y in 15..35},n,n,.8f,.8f))
    }
    @Test fun rejectsNoiseFullFrameAndInvalidCoordinates() {
        assertNull(TapSubjectMask.select(confidence {x,y->x==48 && y==48},n,n,.5f,.5f))
        assertNull(TapSubjectMask.select(FloatArray(n*n){.99f},n,n,.5f,.5f))
        assertNull(TapSubjectMask.select(floatArrayOf(),n,n,.5f,.5f))
        assertNull(TapSubjectMask.select(FloatArray(n*n),n,n,Float.NaN,.5f))
    }
    @Test fun borderObjectRemainsClampedToThePreview() {
        val result=TapSubjectMask.select(confidence {x,y->x<28 && y in 20..70},n,n,.03f,.4f)!!
        assertEquals(0f,result.box.left,.001f)
        assertTrue(result.edges.all {it.from.x in 0f..1f && it.to.y in 0f..1f})
    }
    @Test fun resamplesModelOutputToTheSameViewportWithoutSquareCropping() {
        val w=192;val h=288
        val frame=FloatArray(w*h){i->if(i%w in 48..95 && i/w in 72..143) .9f else .1f}
        val result=TapSubjectMask.select(frame,w,h,.375f,.375f)!!
        assertEquals(.25f,result.box.left,.015f);assertEquals(.5f,result.box.bottom,.015f)
    }
}
