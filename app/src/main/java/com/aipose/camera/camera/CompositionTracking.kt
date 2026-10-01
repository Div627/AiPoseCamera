package com.aipose.camera.camera

import kotlin.math.abs

/** Small translation-only patch tracker. Rejects low texture, occlusion and ambiguous matches.
 * Coordinates use the same mirrored, cropped 32x32 luminance grid as the preview. */
class CompositionTracking {
    private var reference: FloatArray? = null
    private var origin: SubjectFraming.Box? = null
    private var dx = 0
    private var dy = 0
    fun reset() { reference=null; origin=null; dx=0; dy=0 }
    fun update(frame: FloatArray, box: SubjectFraming.Box): SubjectFraming.Box? {
        if(frame.size!=1024) return null
        if(reference==null) {reference=frame.copyOf();origin=box}
        val base=reference!!; val initial=origin!!
        val cells=(0 until 1024).filter { (it%32+.5f)/32 in initial.left..initial.right && (it/32+.5f)/32 in initial.top..initial.bottom }
        if(cells.size<12) return null
        val mean=cells.map {base[it]}.average()
        if(cells.map {abs(base[it]-mean)}.average()<.035) return null
        val candidates=mutableListOf<Triple<Float,Int,Int>>()
        for(y in dy-4..dy+4) for(x in dx-4..dx+4) {
            if(initial.left+x/32f<0 || initial.right+x/32f>1 || initial.top+y/32f<0 || initial.bottom+y/32f>1) continue
            val error=cells.sumOf {abs(base[it]-frame[(it/32+y)*32+it%32+x]).toDouble()}.toFloat()/cells.size
            candidates.add(Triple(error,x,y))
        }
        val best=candidates.minByOrNull {it.first} ?: return null
        val runner=candidates.filter {abs(it.second-best.second)+abs(it.third-best.third)>2}.minOfOrNull{it.first}
        if(best.first>.11f || (runner!=null && runner-best.first<.012f)) return null
        dx=best.second;dy=best.third
        return SubjectFraming.Box(initial.left+dx/32f,initial.top+dy/32f,initial.right+dx/32f,initial.bottom+dy/32f)
    }
}
