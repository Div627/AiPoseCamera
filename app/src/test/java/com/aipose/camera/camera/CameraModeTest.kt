package com.aipose.camera.camera

import org.junit.Assert.*
import org.junit.Test

class CameraModeTest {
    @Test fun portraitWaitsForPeopleRatherThanUsingLandscapeShutter() {
        val m=CameraMode.PORTRAIT
        assertTrue(m.analyzesFrames);assertTrue(m.usesPoseModel);assertFalse(m.allowsSubjectSelection)
        assertFalse(m.permitsAutoCapture(null));assertFalse(m.permitsAutoCapture(0));assertFalse(m.permitsAutoCapture(5))
        for(count in 1..4) assertTrue(m.permitsAutoCapture(count))
        assertEquals(0,m.effectiveCount(0))
    }
    @Test fun landscapeKeepsItsOwnPolicyEvenWithPassersByOrMissingDetector() {
        val m=CameraMode.LANDSCAPE
        assertTrue(m.analyzesFrames);assertFalse(m.usesPoseModel);assertTrue(m.allowsSubjectSelection)
        for(count in listOf(null,0,1,2,4,5)) {
            assertEquals(0,m.effectiveCount(count));assertTrue(m.permitsAutoCapture(count))
        }
    }
    @Test fun toolsKeepPortraitAndSceneryIntentsSeparate() {
        assertTrue(TravelIntent.TOGETHER in CameraMode.PORTRAIT.intents)
        assertFalse(TravelIntent.TOGETHER in CameraMode.LANDSCAPE.intents)
        assertTrue(TravelIntent.AURORA in CameraMode.LANDSCAPE.intents)
        assertFalse(TravelIntent.AURORA in CameraMode.PORTRAIT.intents)
    }
    @Test fun allModesAreExplicitAndHaveDistinctLabels() {
        assertEquals(listOf("人像","风景"),CameraMode.entries.map{it.label})
        assertEquals(1,CameraMode.entries.count{it.usesPoseModel})
        assertEquals(1,CameraMode.entries.count{it.allowsSubjectSelection})
    }
}
