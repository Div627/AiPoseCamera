package com.aipose.camera.camera

import kotlin.math.abs

/** Stable entry, hysteresis and cooldown keep noisy sensors from repeatedly buzzing. */
class LevelFeedbackGate {
    private var armed = true
    private var stableSince: Long? = null
    private var lastFeedback = -2000L
    fun update(roll: Float?, now: Long): Boolean {
        if (roll == null || !roll.isFinite()) { stableSince = null; return false }
        if (abs(roll) >= 4f) { armed = true; stableSince = null; return false }
        if (abs(roll) > 2f) { stableSince = null; return false }
        if (stableSince == null) stableSince = now
        if (!armed || now - stableSince!! < 250 || now - lastFeedback < 1800) return false
        armed = false
        lastFeedback = now
        return true
    }
}
