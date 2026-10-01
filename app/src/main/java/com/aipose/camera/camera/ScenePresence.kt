package com.aipose.camera.camera

/** A missing/uncertain inference is not evidence for an empty scene. */
class ScenePresence {
    var people: Int? = null
        private set
    var settled = false
        private set
    private var candidate: Int? = null
    private var since = 0L
    private var last = 0L
    fun reset() { people=null;candidate=null;since=0;last=0;settled=false }
    fun update(count: Int?, now: Long): Int? {
        if(count==null || count<0) {reset();return null}
        if(last>0 && now-last>900) reset()
        last=now
        if(candidate!=count) {candidate=count;since=now;settled=false}
        val required=if(count==0) 2200L else 1100L
        if(now-since>=required) {people=count;settled=true}
        return people
    }
    companion object {
        const val MAX_GUIDED_PEOPLE=4
        fun label(count:Int?)=when(count) {null->"正在识别画面";0->"风景模式";1->"单人模式";in 2..4->"${count}人合影";else->"至少5人 · 手动拍摄"}
    }
}
