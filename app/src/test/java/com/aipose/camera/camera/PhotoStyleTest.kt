package com.aipose.camera.camera

import org.junit.Assert.*
import org.junit.Test

class PhotoStyleTest {
    @Test fun automaticNaturalStylesPreserveSkinColorsAndDetail() {
        val skin=0xffd9ab91.toInt();val grade=ColorGrade(strength=.6f)
        for(style in listOf(PhotoStyle.PORTRAIT,PhotoStyle.SCENIC)) {
            val output=grade.transform(skin,grade.matrix(style),grade.lut())
            for(shift in listOf(0,8,16)) assertTrue(kotlin.math.abs((skin ushr shift and 255)-(output ushr shift and 255))<=4)
        }
        assertEquals(1f,grade.contrast,0f)
        assertEquals(1f,grade.saturation,0f)
    }

    @Test fun originalPreservesEveryChannelAndAlpha() {
        val m=PhotoStyle.ORIGINAL.matrix()
        for(a in listOf(0,64,128,255)) for(r in 0..255 step 17) for(g in 0..255 step 17) for(b in 0..255 step 17) {
            val color=(a shl 24) or (r shl 16) or (g shl 8) or b
            assertEquals(color,PhotoStyle.transform(color,m))
        }
    }
    @Test fun softStyleActuallyBrightensPixels() {
        val input=0xff202020.toInt();val output=PhotoStyle.transform(input,PhotoStyle.SOFT.matrix())
        assertTrue((output and 255)>(input and 255))
    }
    @Test fun highlightStyleReducesWhite() {
        val result=PhotoStyle.transform(-1,PhotoStyle.HIGHLIGHT.matrix())
        assertTrue((result and 255)<255)
        assertEquals(255,result ushr 24)
    }
    @Test fun portraitStyleHasSmallWarmBias() {
        val result=PhotoStyle.transform(0xff808080.toInt(),PhotoStyle.PORTRAIT.matrix())
        assertTrue((result ushr 16 and 255)>(result and 255))
    }
    @Test fun sceneLuminanceUsesAllChannels() {
        val reading=SceneOptimizer.measure(intArrayOf(0xff000000.toInt(),-1))
        assertEquals(.5f,reading.mean,.0001f);assertEquals(.5f,reading.highlights,.0001f)
    }
    @Test fun highlightProtectionTakesPriorityOverPortrait() {
        assertEquals(PhotoStyle.HIGHLIGHT,PhotoStyle.choose(.6f,.2f,true))
    }
    @Test fun darkSceneUsesSoftStyle() {assertEquals(PhotoStyle.SOFT,PhotoStyle.choose(.2f,0f,false))}
    @Test fun normalLandscapeUsesGentleScenicStyle() {assertEquals(PhotoStyle.SCENIC,PhotoStyle.choose(.5f,0f,false))}
    @Test fun sceneNeedsThreeConsistentSamples() {
        val meter=SceneOptimizer();val dark=SceneOptimizer.Reading(.2f,0f)
        assertNull(meter.update(dark,false,0f));assertNull(meter.update(dark,false,0f))
        assertEquals(PhotoStyle.SOFT,meter.update(dark,false,0f)?.style)
    }
    @Test fun alternatingSceneDoesNotFlicker() {
        val meter=SceneOptimizer()
        repeat(10) {
            assertNull(meter.update(SceneOptimizer.Reading(.2f,0f),false,0f))
            assertNull(meter.update(SceneOptimizer.Reading(.6f,0f),false,0f))
        }
    }
    @Test fun positiveExposureDoesNotOscillateAfterBrightening() {
        val meter=SceneOptimizer();var choice:SceneOptimizer.Choice?=null
        repeat(3) {choice=meter.update(SceneOptimizer.Reading(.27f,0f),false,.33f)}
        assertEquals(.33f,choice!!.exposureEv,.001f)
    }
    @Test fun highlightProtectionIsHeldAfterClippingDrops() {
        val meter=SceneOptimizer();var choice:SceneOptimizer.Choice?=null
        repeat(3) {choice=meter.update(SceneOptimizer.Reading(.65f,.02f),false,-.33f)}
        assertEquals(-.33f,choice!!.exposureEv,.001f)
    }
    @Test fun darkSceneReleasesHighlightExposure() {
        val meter=SceneOptimizer();var choice:SceneOptimizer.Choice?=null
        repeat(3) {choice=meter.update(SceneOptimizer.Reading(.1f,0f),false,-.33f)}
        assertEquals(.33f,choice!!.exposureEv,.001f)
    }
}
