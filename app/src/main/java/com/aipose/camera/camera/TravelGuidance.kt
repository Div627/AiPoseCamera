package com.aipose.camera.camera

import com.aipose.camera.pose.Landmarks
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot

enum class TravelIntent(val label:String,val advice:String) {
    AUTO("自动","拍摄者：先确定一个主体，检查画面四边"),
    ENVIRONMENT("环境人像","拍摄者：给景物留出空间，人物放在一侧；从安全位置调整机位"),
    TOGETHER("合照","被摄者：肩膀稍错开，一起看景或自然交谈，避免遮脸"),
    MOUNTAIN("山景","拍摄者：试着把近处岩石或小路放入前景，保持远山完整"),
    WATERFALL("瀑布","拍摄者：先保护水流亮部，带一点前景；不要为取景靠近湿滑边缘"),
    COAST("海岸","拍摄者：从安全陆侧取景，留意海浪与警戒线，不向浪区移动"),
    AURORA("极光","拍摄者：稳固三脚架，使用延时；需要长曝或夜景时打开原生相机")
}
enum class ReferenceStyle(val label:String) { ALL("通用"), MASCULINE("男生参考"), FEMININE("女生参考") }

object TravelGuidance {
    fun next(intent:TravelIntent,count:Int?,points:Landmarks?,roll:Float?,poseTip:String):String {
        if(roll!=null && abs(roll)>3f) return "拍摄者：调整手机左右倾斜，让水平线变平"
        if(count==null) return "识别暂不确定，可按快门手动拍摄"
        if(count>4) return "超过四人引导范围，请手动拍摄"
        if(count>0 && points!=null && points.values.any {it.first !in .06f.. .94f || it.second !in .06f.. .94f})
            return "拍摄者：缩小倍率或从安全位置重新取景，留出四肢空间"
        if(intent!=TravelIntent.AUTO) return intent.advice
        return if(count>0) "被摄者：$poseTip" else TravelIntent.AUTO.advice
    }
    /** Fixed portrait UI; suppress unstable roll while the phone is approximately face-up/down. */
    fun roll(x:Float,y:Float):Float? = if(!x.isFinite() || !y.isFinite() || hypot(x,y)<2f) null else (atan2(x,y)*180/Math.PI).toFloat()
}

/** Absolute monotonic deadline, one-shot consumption, cancellable by all UI/lifecycle exits. */
class CountdownGate {
    private var deadline:Long?=null
    fun start(seconds:Int,now:Long) {require(seconds in listOf(0,2,5,10));deadline=now+seconds*1000L}
    fun cancel(){deadline=null}
    fun remaining(now:Long)=deadline?.let {ceil((it-now).coerceAtLeast(0)/1000.0).toInt()} ?: 0
    fun consume(now:Long):Boolean {val end=deadline ?: return false;if(now<end) return false;deadline=null;return true}
}

/** CameraX zoom is centered. Off-center regions require explicit reframing before enlargement. */
object SubjectFraming {
    data class Box(val left:Float,val top:Float,val right:Float,val bottom:Float) {
        val width get()=right-left;val height get()=bottom-top
        val cx get()=(left+right)/2;val cy get()=(top+bottom)/2
    }
    data class Choice(val zoom:Float?,val message:String)
    fun choose(box:Box,current:Float,min:Float,max:Float):Choice {
        if(listOf(box.left,box.top,box.right,box.bottom).any{!it.isFinite() || it !in 0f..1f} || box.width<.08f || box.height<.08f) return Choice(null,"主体范围太小，请靠近些再点击")
        if(abs(box.cx-.5f)>.14f || abs(box.cy-.5f)>.14f) return Choice(null,"先从安全位置转动手机，让主体靠近中央")
        val safeHorizontal=.43f/(maxOf(abs(box.left-.5f),abs(box.right-.5f)).coerceAtLeast(.1f))
        val safeVertical=.40f/(maxOf(abs(box.top-.5f),abs(box.bottom-.5f)).coerceAtLeast(.1f))
        val zoom=(current*minOf(safeHorizontal,safeVertical,2f).coerceAtLeast(1f)).coerceIn(min,minOf(max,3f).coerceAtLeast(min))
        if(zoom<=current+.02f) return Choice(null,"当前范围已足够或已到倍率上限，可直接手动拍摄")
        return Choice(zoom,"主体放大后检查边缘；倍率可能含数字裁切，会损失细节")
    }
}
