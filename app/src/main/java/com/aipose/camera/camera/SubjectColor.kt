package com.aipose.camera.camera

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Coordinates live in the shared, upright cropped PreviewView viewport (including front mirror). */
object SubjectColor {
    data class Point(val x:Float,val y:Float)
    data class Region(val points:List<Point>,val mask:BooleanArray)
    const val GRID=32
    fun contains(points:List<Point>,x:Float,y:Float):Boolean {
        var inside=false;var j=points.lastIndex
        for(i in points.indices) {
            val a=points[i];val b=points[j]
            if((a.y>y)!=(b.y>y) && x<(b.x-a.x)*(y-a.y)/(b.y-a.y)+a.x) inside=!inside
            j=i
        }
        return inside
    }
    fun region(points:List<Point>):Region? {
        if(points.size<6 || points.any{!it.x.isFinite() || !it.y.isFinite() || it.x !in 0f..1f || it.y !in 0f..1f}) return null
        val mask=BooleanArray(GRID*GRID){contains(points,(it%GRID+.5f)/GRID,(it/GRID+.5f)/GRID)}
        return if(mask.count{it}<12) null else Region(points.toList(),mask)
    }
    fun sampleX(column:Int,width:Int,mirrored:Boolean):Int {
        val x=((column+.5f)*width/GRID).toInt().coerceIn(0,width-1)
        return if(mirrored) width-1-x else x
    }
    data class Stats(val mean:Float,val highlights:Float,val saturation:Float,val cool:Float)
    data class Reading(val subject:Stats,val full:Stats,val signature:FloatArray)
    fun measure(pixels:IntArray,region:Region):Reading {
        fun stats(mask:BooleanArray?):Stats {
            var n=0;var lum=0f;var high=0;var sat=0f;var cool=0f
            for(i in pixels.indices) if(mask==null || mask[i]) {
                val p=pixels[i];val r=(p ushr 16 and 255)/255f;val g=(p ushr 8 and 255)/255f;val b=(p and 255)/255f
                val l=.213f*r+.715f*g+.072f*b;val hi=max(r,max(g,b));val lo=min(r,min(g,b))
                lum+=l;if(hi>.94f) high++;sat+=if(hi>0) (hi-lo)/hi else 0f;cool+=b-r;n++
            }
            return Stats(lum/n,high.toFloat()/n,sat/n,cool/n)
        }
        val signature=FloatArray(64) {i-> val p=pixels[(i/8*4+2)*GRID+i%8*4+2];(.213f*(p ushr 16 and 255)+.715f*(p ushr 8 and 255)+.072f*(p and 255))/255f}
        return Reading(stats(region.mask),stats(null),signature)
    }
    data class Choice(val style:PhotoStyle,val strength:Float)
    fun choose(r:Reading):Choice=when {
        r.full.highlights>.12f || r.subject.highlights>.16f -> Choice(PhotoStyle.HIGHLIGHT,.65f)
        r.subject.mean<.28f && r.full.highlights<.06f -> Choice(PhotoStyle.SOFT,.55f)
        r.subject.saturation>.65f -> Choice(PhotoStyle.ORIGINAL,0f)
        r.subject.cool>.08f -> Choice(PhotoStyle.SCENIC,.40f)
        else -> Choice(PhotoStyle.SCENIC,.60f)
    }
    /** Three consistent readings, >=1.8 s between changes; a changed scene invalidates the ROI. */
    class Session {
        private var baseline:FloatArray?=null
        private var changed=0;private var candidate:Choice?=null;private var repeats=0;private var last=Long.MIN_VALUE/2
        var invalidated=false;private set
        fun update(r:Reading,now:Long,automatic:Boolean):Choice? {
            if(invalidated) return null
            val base=baseline ?: r.signature.copyOf().also{baseline=it}
            val delta=base.indices.sumOf{abs(base[it]-r.signature[it]).toDouble()}/base.size
            changed=if(delta>.19) changed+1 else 0
            if(changed>=3){invalidated=true;return null}
            val next=choose(r)
            repeats=if(candidate==next) repeats+1 else 1;candidate=next
            if(!automatic || repeats<3 || now-last<1800) return null
            last=now;return next
        }
    }
}
