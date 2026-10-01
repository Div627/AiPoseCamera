package com.aipose.camera.camera

import com.aipose.camera.pose.Landmarks
import kotlin.math.abs
import kotlin.math.hypot

/** Landscape: whole-frame stability; groups: everyone visible and stable. Neither uses single-person pose score. */
class SceneCaptureGate {
    data class Decision(val message:String,val progress:Float=0f,val shoot:Boolean=false)
    private var anchor:FloatArray?=null
    private var peopleAnchor:List<Landmarks>?=null
    private var since=0L
    private var last=0L
    fun reset(){anchor=null;peopleAnchor=null;since=0;last=0}
    fun update(now:Long,count:Int?,settled:Boolean,signature:FloatArray,persons:List<Landmarks>,ready:Boolean):Decision {
        if(!settled || count==null || count>4 || !ready || signature.isEmpty()) {reset();return Decision("等待识别、对焦与参数稳定")}
        if(last>0 && now-last>900) reset()
        last=now
        if(count>0 && (persons.size!=count || persons.any { p->
            listOf(0,11,12,23,24,27,28).any {p[it]==null} || p.values.any {it.first !in .025f.. .975f || it.second !in .025f.. .975f}
        })) {reset();return Decision("请让所有人的头脚完整入镜，避免相互遮挡")}
        val mean=signature.average()
        if(signature.any{!it.isFinite()} || mean !in .06.. .94 || signature.maxOrNull()!!-signature.minOrNull()!!<.025f) {
            reset();return Decision("光线或画面细节不足，可调整光线或手动拍摄")
        }
        val a=anchor
        val old=peopleAnchor
        val shifted=a==null || a.size!=signature.size || a.indices.sumOf {abs(a[it]-signature[it]).toDouble()}/signature.size>.025 ||
            (count>0 && (old==null || old.size!=persons.size || persons.indices.any {i->
                listOf(0,11,12,23,24,27,28).any {id->
                    val x=old[i][id];val y=persons[i][id]
                    x==null || y==null || hypot(x.first-y.first,x.second-y.second)>.018f
                }
            }))
        if(shifted){anchor=signature.copyOf();peopleAnchor=persons.map{it.toMap()};since=now}
        val duration=if(count==0) 2400f else 2000f
        val progress=((now-since)/duration).coerceIn(0f,1f)
        return Decision(if(count==0) "风景画面稳定中，请留意水平线与主体位置" else "所有人物已入镜，保持稳定",progress,progress>=1f)
    }
}
