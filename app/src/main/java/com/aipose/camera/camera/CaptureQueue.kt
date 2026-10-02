package com.aipose.camera.camera

/** Bounded processing and monotonic publication keep old edits from replacing newer shots. */
class CaptureQueue(private val capacity: Int = 2) {
    init { require(capacity > 0) }
    private var next = 0L
    private val pending = mutableSetOf<Long>()
    private var shooting: Long? = null
    var latest = 0L
        private set
    val size get() = pending.size
    val capturing get() = shooting != null
    val available get() = size < capacity && !capturing

    fun reserve(): Long? = if (available) (++next).also { pending.add(it); shooting = it } else null
    fun publish(id: Long): Boolean {
        if (shooting == id) shooting = null
        if (id !in pending || id < latest) return false
        latest = id
        return true
    }
    fun complete(id: Long): Boolean {
        if (shooting == id) shooting = null
        return pending.remove(id)
    }
    fun isLatest(id: Long) = id == latest
}
