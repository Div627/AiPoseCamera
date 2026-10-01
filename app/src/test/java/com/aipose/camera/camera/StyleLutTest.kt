package com.aipose.camera.camera

import org.junit.Assert.*
import org.junit.Test

class StyleLutTest {
    @Test fun identityPreservesColorAndAlpha() {
        val lut=StyleLut.create(PhotoStyle.ORIGINAL,ColorGrade())
        for(r in 0..255 step 17) for(g in 0..255 step 23) for(b in 0..255 step 31) {
            val p=0x7f000000 or (r shl 16) or (g shl 8) or b;val q=lut.apply(p)
            assertEquals(0x7f,q ushr 24)
            for(shift in listOf(0,8,16)) assertTrue(kotlin.math.abs((p ushr shift and 255)-(q ushr shift and 255))<=1)
        }
    }
    @Test fun zeroStrengthRestoresOriginalAcrossEveryFilm() {
        val reference=StyleLut.create(PhotoStyle.ORIGINAL)
        PhotoStyle.entries.filter{FilmProfiles.profile(it)!=null}.forEach {style->
            val lut=StyleLut.create(style,ColorGrade(strength=0f))
            assertArrayEquals(style.name,reference.colors,lut.colors)
        }
    }
    @Test fun monochromeRemovesChromaAndMaintainsOrderedGrey() {
        for(style in listOf(PhotoStyle.A_APX,PhotoStyle.F_ACROS,PhotoStyle.R_HARD_MONO)) {
            val lut=StyleLut.create(style,ColorGrade(strength=1f));var previous=-1
            for(i in 0..255) {val out=lut.apply(0xff000000.toInt() or (i shl 16) or (i shl 8) or i);assertTrue((out and 255)>=previous);previous=out and 255}
            for(p in listOf(0xffa87351.toInt(),0xff349bea.toInt(),0xff459652.toInt())) {
                val out=lut.apply(p);assertEquals(out and 255,out ushr 8 and 255);assertEquals(out and 255,out ushr 16 and 255)
            }
        }
    }
    @Test fun profilesAreDistinct() {
        val profiles=PhotoStyle.entries.filter{FilmProfiles.profile(it)!=null}
        assertEquals(profiles.size,profiles.map{StyleLut.create(it,ColorGrade(strength=1f)).colors.contentHashCode()}.toSet().size)
    }
}
