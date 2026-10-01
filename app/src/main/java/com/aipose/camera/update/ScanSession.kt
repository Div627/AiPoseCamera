package com.aipose.camera.update

import java.util.concurrent.atomic.AtomicBoolean

/** Owns one scanner visit; an in-flight ML task may finish after the screen closes. */
class ScanSession {
    private val active = AtomicBoolean(true)
    private val busy = AtomicBoolean(false)
    private val delivered = AtomicBoolean(false)
    fun begin(): Boolean = active.get() && !delivered.get() && busy.compareAndSet(false, true)
    fun finish() { busy.set(false) }
    fun close() { active.set(false) }
    fun isActive() = active.get()
    fun deliver(value: String?, resumed: Boolean): String? {
        val url = UpdateLink.parse(value) ?: return null
        return if (resumed && active.get() && delivered.compareAndSet(false, true)) url else null
    }
}
