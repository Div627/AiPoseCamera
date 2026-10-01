package com.aipose.camera.camera

import kotlin.math.pow

class SceneOptimizer {
    data class Reading(val mean: Float, val highlights: Float)
    data class Choice(val style: PhotoStyle, val exposureEv: Float)
    private var candidate: Choice? = null
    private var count=0
    fun reset() {candidate=null;count=0}
    fun update(reading:Reading,portrait:Boolean,currentEv:Float):Choice? {
        val baseline=(reading.mean / 2.0.pow(currentEv.toDouble())).toFloat().coerceIn(0f,1f)
        val requested=PhotoStyle.exposureEv(baseline,reading.highlights)
        // Avoid undoing highlight protection immediately after exposure has reduced clipping.
        val ev=if(currentEv<0 && requested==0f && (reading.mean>.55f || reading.highlights>.015f)) currentEv else requested
        val next=Choice(PhotoStyle.choose(reading.mean,reading.highlights,portrait),ev)
        if(next==candidate) count++ else {candidate=next;count=1}
        return if(count>=3) next else null
    }
    companion object {
        fun measure(pixels:IntArray):Reading {
            if(pixels.isEmpty()) return Reading(.5f,0f)
            var sum=0f;var highlights=0
            for(pixel in pixels) {
                val value=(.213f*(pixel ushr 16 and 255)+.715f*(pixel ushr 8 and 255)+.072f*(pixel and 255))/255f
                sum+=value;if(value>.94f) highlights++
            }
            return Reading(sum/pixels.size,highlights.toFloat()/pixels.size)
        }
    }
}
