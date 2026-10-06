package com.aipose.camera.camera

import org.junit.Assert.*
import org.junit.Test

class AdaptivePhotographyTest {
    @Test fun shadowAndHighlightAdjustmentsPreserveBlackAndWhiteAnchors() {
        for (shadow in listOf(-1f, 0f, 1f)) for (highlight in listOf(-1f, 0f, 1f)) {
            val grade=ColorGrade(shadows=shadow,highlights=highlight)
            assertEquals(0f,grade.curve(0f),0f)
            assertEquals(1f,grade.curve(1f),0f)
            assertEquals(0xff000000.toInt(),grade.transform(0xff000000.toInt(),grade.matrix(PhotoStyle.ORIGINAL),grade.lut()))
        }
    }
    @Test fun luminanceTonePreservesHueRatiosAndAlpha() {
        val grade=ColorGrade(shadows=.8f,highlights=-.4f)
        val input=0x80402010.toInt()
        val out=grade.transform(input,grade.matrix(PhotoStyle.ORIGINAL),grade.lut())
        assertEquals(128,out ushr 24)
        val red=(out ushr 16 and 255).toFloat();val green=(out ushr 8 and 255).toFloat();val blue=(out and 255).toFloat()
        assertEquals(2f,red/green,.08f);assertEquals(2f,green/blue,.12f)
        assertTrue(red>64)
    }
    @Test fun brightSaturatedColorCannotAcquireNewChannelClipping() {
        val grade=ColorGrade(shadows=1f)
        for(pixel in listOf(0xfff04020.toInt(),0xff20f040.toInt(),0xff4020f0.toInt())) {
            val out=grade.transform(pixel,grade.matrix(PhotoStyle.ORIGINAL),grade.lut())
            assertTrue(listOf(0,8,16).all {(out ushr it and 255) < 255})
            // Channels move together rather than tinting a saturated subject.
            val inChannels=listOf(0,8,16).map {pixel ushr it and 255}
            val outChannels=listOf(0,8,16).map {out ushr it and 255}
            assertEquals(inChannels.indexOf(inChannels.max()),outChannels.indexOf(outChannels.max()))
        }
    }
    @Test fun darkForegroundWithBrightSkyDoesNotIncreaseSensorExposure() {
        val pixels=IntArray(1024) {if(it<180) 0xffe8e8e8.toInt() else 0xff151515.toInt()}
        val reading=SceneOptimizer.measure(pixels)
        val meter=SceneOptimizer();var choice:SceneOptimizer.Choice?=null
        repeat(3){choice=meter.update(reading,false,0f)}
        assertEquals(0f,choice!!.exposureEv,0f)
        assertTrue(choice!!.highlights<0f)
    }
    @Test fun genuinelyClippedHighlightsOverrideDarkMean() {
        val reading=SceneOptimizer.measure(IntArray(1024){if(it<160) -1 else 0xff101010.toInt()})
        val meter=SceneOptimizer();var choice:SceneOptimizer.Choice?=null
        repeat(3){choice=meter.update(reading,false,0f)}
        assertTrue(choice!!.exposureEv<0f)
    }
    @Test fun intentionalNightToneIsNotForcedToMidGray() {
        val meter=SceneOptimizer();val reading=SceneOptimizer.measure(IntArray(1024){0xff080808.toInt()})
        var choice:SceneOptimizer.Choice?=null;repeat(3){choice=meter.update(reading,false,0f)}
        assertEquals(0f,choice!!.exposureEv,0f);assertEquals(0f,choice!!.shadows,0f);assertEquals(PhotoStyle.ORIGINAL,choice!!.style)
    }
    private fun horizon(row:Int)=FloatArray(1024){i->if(i/32<row) .78f else if((i%32+i/32)%2==0) .25f else .30f}
    @Test fun horizonGuidanceWaitsForStableEvidence() {
        val guide=LandscapeGuidance()
        assertNull(guide.update(horizon(13),0));assertNull(guide.update(horizon(13),500))
        assertTrue(guide.update(horizon(13),1100)!!.contains("向下"))
        assertEquals(13/32f,guide.horizon!!,.0001f)
    }
    @Test fun panningAndMissingEvidenceCannotKeepOldCompositionTip() {
        val guide=LandscapeGuidance();guide.update(horizon(13),100);guide.update(horizon(13),1200)
        assertNull(guide.update(horizon(22),1300));assertNull(guide.horizon)
        assertNull(guide.update(FloatArray(1024){.5f},1400));assertNull(guide.update(FloatArray(1024){.5f},1500));assertNull(guide.horizon)
    }
    @Test fun flatWallsNoisyTextureAndInvalidFramesAreRejected() {
        assertNull(LandscapeGuidance.estimate(FloatArray(1024){.5f}))
        assertNull(LandscapeGuidance.estimate(FloatArray(1024){if(it/32%2==0) .8f else .2f}))
        assertNull(LandscapeGuidance.estimate(FloatArray(1024){Float.NaN}))
        assertNull(LandscapeGuidance.estimate(FloatArray(1)))
        // A simple two-tone wall lacks the textured lower region.
        assertNull(LandscapeGuidance.estimate(FloatArray(1024){if(it<512) .8f else .2f}))
    }
}
