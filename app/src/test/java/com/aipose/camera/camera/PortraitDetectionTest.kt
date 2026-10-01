package com.aipose.camera.camera
import org.junit.Assert.*
import org.junit.Test
class PortraitDetectionTest {
    @Test fun closeFaceCountsWithoutInventingBody() {assertEquals(1,PortraitDetection.count(0,1));assertFalse(PortraitDetection.guidable(mapOf(0 to (.5f to .5f))))}
    @Test fun unavailableIsDifferentFromEmptyScene() {assertNull(PortraitDetection.count(null,null));assertEquals(0,PortraitDetection.count(0,0))}
    @Test fun poseAndFaceAreNotDoubleCounted() {assertEquals(2,PortraitDetection.count(2,2))}
    @Test fun lowConfidenceAndOutOfFrameJointsAreRejected() {assertFalse(PortraitDetection.visible(.5f,.5f,.1f,.9f));assertFalse(PortraitDetection.visible(-.2f,.5f,.9f,.9f));assertFalse(PortraitDetection.visible(Float.NaN,.5f,1f,1f));assertTrue(PortraitDetection.visible(.5f,.5f,.55f,.55f))}
}
