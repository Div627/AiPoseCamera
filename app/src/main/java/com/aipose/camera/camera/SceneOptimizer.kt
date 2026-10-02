package com.aipose.camera.camera

import kotlin.math.pow

class SceneOptimizer {
    data class Reading(val mean: Float, val highlights: Float, val faceMean: Float? = null)
    data class Choice(val style: PhotoStyle, val exposureEv: Float, val shadows: Float = 0f, val highlights: Float = 0f)
    private var candidate: Choice? = null
    private var count=0
    fun reset() {candidate=null;count=0}
    fun update(reading:Reading,portrait:Boolean,currentEv:Float):Choice? {
        val baseline=(reading.mean / 2.0.pow(currentEv.toDouble())).toFloat().coerceIn(0f,1f)
        val face=reading.faceMean?.takeIf {portrait && it.isFinite() && it in 0f..1f}
        val faceBaseline=face?.let {(it/2.0.pow(currentEv.toDouble())).toFloat()}
        val requested=if(faceBaseline!=null && faceBaseline<.28f && reading.highlights<.04f) .33f else PhotoStyle.exposureEv(baseline,reading.highlights)
        // Avoid undoing highlight protection immediately after exposure has reduced clipping.
        val ev=if(currentEv<0 && requested==0f && (reading.mean>.55f || reading.highlights>.015f)) currentEv else requested
        val backlit=face!=null && face<.38f && reading.mean-face>.16f
        val next=Choice(PhotoStyle.choose(reading.mean,reading.highlights,portrait),ev,
            shadows=if(backlit) .22f else 0f,highlights=if(reading.highlights>.12f) -.12f else 0f)
        if(next==candidate) count++ else {candidate=next;count=1}
        return if(count>=3) next else null
    }
    companion object {
        fun measure(pixels:IntArray,faces:List<FaceRegion> = emptyList()):Reading {
            if(pixels.isEmpty()) return Reading(.5f,0f)
            var sum=0f;var highlights=0;var faceSum=0f;var faceSamples=0
            for((index,pixel) in pixels.withIndex()) {
                val value=(.213f*(pixel ushr 16 and 255)+.715f*(pixel ushr 8 and 255)+.072f*(pixel and 255))/255f
                sum+=value;if(value>.94f) highlights++
                if(pixels.size==1024 && faces.any {face->face.valid && (index%32+.5f)/32 in face.left..face.right && (index/32+.5f)/32 in face.top..face.bottom}) {
                    faceSum+=value;faceSamples++
                }
            }
            return Reading(sum/pixels.size,highlights.toFloat()/pixels.size,if(faceSamples>=6) faceSum/faceSamples else null)
        }
    }
}
