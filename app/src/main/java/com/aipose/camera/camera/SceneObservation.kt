package com.aipose.camera.camera

import com.aipose.camera.pose.Landmarks
import com.aipose.camera.pose.PoseTemplate

/** A null count means inference unavailable or at least one detection lacks reliable torso landmarks. */
data class SceneObservation(val people:List<Landmarks>,val count:Int?,val signature:FloatArray)

object GroupFraming {
    fun zoom(people:List<Landmarks>,current:Float,min:Float,max:Float,targets:List<PoseTemplate>?=null):Float? {
        if(people.size !in 2..4 || people.any{p->listOf(0,11,12,23,24,27,28).any{p[it]==null}}) return null
        val p=people.flatMap{it.values}
        val width=p.maxOf{it.first}-p.minOf{it.first}+.08f
        val height=p.maxOf{it.second}-p.minOf{it.second}+.10f
        val targetPoints=targets?.takeIf{it.size==people.size}?.flatMap{it.points.values}
        val goalHeight=if(!targetPoints.isNullOrEmpty()) (targetPoints.maxOf{it.second}-targetPoints.minOf{it.second}+.10f).coerceIn(.2f,.86f)
            else when(people.size){2->.68f;3->.58f;else->.48f}
        val ratio=minOf(.86f/width.coerceAtLeast(.1f),goalHeight/height.coerceAtLeast(.1f))
        if(kotlin.math.abs(ratio-1)<.08f) return null
        val wanted=(current*ratio.coerceIn(.94f,1.06f)).coerceIn(min,max)
        return wanted.takeIf{kotlin.math.abs(it-current)>.025f}
    }
}

/** A non-responsive detector must not drive an endless sequence of zoom commands. */
class GroupZoomSession {
    data class Decision(val zoom:Float?=null,val blocked:Boolean=false)
    private var attempts=0
    fun reset(){attempts=0}
    fun evaluate(people:List<Landmarks>,current:Float,min:Float,max:Float,targets:List<PoseTemplate>?):Decision {
        if(attempts>=18) return Decision(blocked=true)
        val next=GroupFraming.zoom(people,current,min,max,targets)
        if(next==null) attempts=0 else attempts++
        return Decision(next)
    }
}
