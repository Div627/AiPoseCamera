package com.aipose.camera.camera

import org.junit.Assert.*
import org.junit.Test

class LevelFeedbackGateTest {
    @Test fun stableEntryBuzzesOnceAndNoiseDoesNotRearm() {
        val gate=LevelFeedbackGate()
        assertFalse(gate.update(1f,0))
        assertFalse(gate.update(.5f,200))
        assertTrue(gate.update(.8f,260))
        assertFalse(gate.update(1f,500))
        assertFalse(gate.update(3f,1000))
        assertFalse(gate.update(0f,3000))
        assertFalse(gate.update(0f,3400))
    }
    @Test fun realTiltRearmsButCooldownPreventsRepeatedFeedback() {
        val gate=LevelFeedbackGate()
        gate.update(0f,0);assertTrue(gate.update(0f,300))
        gate.update(5f,400);gate.update(0f,450)
        assertFalse(gate.update(0f,800))
        assertTrue(gate.update(0f,2200))
    }
    @Test fun missingInvalidAndUnstableReadingsCannotTrigger() {
        val gate=LevelFeedbackGate()
        assertFalse(gate.update(null,0));assertFalse(gate.update(Float.NaN,1000))
        gate.update(0f,2000);gate.update(3f,2100);assertFalse(gate.update(0f,2200))
        assertFalse(gate.update(0f,2400));assertTrue(gate.update(0f,2500))
    }
}
