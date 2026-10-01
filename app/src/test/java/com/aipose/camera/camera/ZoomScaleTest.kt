package com.aipose.camera.camera

import org.junit.Assert.*
import org.junit.Test

class ZoomScaleTest {
    @Test fun scaleIsSmoothAcrossWideAndTelephotoRatios() {
        for(r in listOf(.7f,1f,1.3f,2f,5f,10f)) assertEquals(r,ZoomScale.ratio(ZoomScale.position(r,.7f,10f),.7f,10f),.001f)
        assertEquals(kotlin.math.sqrt(7f),ZoomScale.ratio(.5f,.7f,10f),.001f)
    }
    @Test fun clampAndSingleRatioDevicesNeverRequestUnsupportedZoom() {
        assertEquals(.7f,ZoomScale.ratio(-2f,.7f,10f),.001f)
        assertEquals(10f,ZoomScale.ratio(4f,.7f,10f),.001f)
        assertEquals(1f,ZoomScale.ratio(.5f,1f,1f),.001f)
        assertEquals(0f,ZoomScale.position(1f,1f,1f),.001f)
    }
    @Test fun presetsFollowActualDeviceCapabilities() {
        assertEquals(listOf(.7f,1f,2f,5f,10f),ZoomScale.presets(.7f,10f))
        assertEquals(listOf(1f,2f),ZoomScale.presets(1f,3f))
        assertEquals(listOf(1f),ZoomScale.presets(1f,1f))
    }
}
