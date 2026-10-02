package com.aipose.camera.camera

import kotlin.math.roundToInt

/** Shared 3D color cube for saved pixels, thumbnails and GPU preview. */
class StyleLut(val colors:IntArray,val size:Int=17) {
    init {require(size>=2 && colors.size==size*size*size)}
    private val lower=IntArray(256){(it*(size-1)/255f).toInt()}
    private val upper=IntArray(256){minOf(lower[it]+1,size-1)}
    private val fraction=FloatArray(256){it*(size-1)/255f-lower[it]}
    fun apply(pixel:Int):Int {
        val r=pixel ushr 16 and 255;val g=pixel ushr 8 and 255;val b=pixel and 255
        val r0=lower[r];val g0=lower[g];val b0=lower[b];val r1=upper[r];val g1=upper[g];val b1=upper[b]
        val x=fraction[r];val y=fraction[g];val z=fraction[b]
        val p000=colors[g0*size*size+b0*size+r0];val p100=colors[g0*size*size+b0*size+r1]
        val p010=colors[g1*size*size+b0*size+r0];val p110=colors[g1*size*size+b0*size+r1]
        val p001=colors[g0*size*size+b1*size+r0];val p101=colors[g0*size*size+b1*size+r1]
        val p011=colors[g1*size*size+b1*size+r0];val p111=colors[g1*size*size+b1*size+r1]
        fun channel(shift:Int):Int {
            fun v(p:Int)=(p ushr shift and 255).toFloat()
            fun mix(a:Float,b:Float,t:Float)=a+(b-a)*t
            return mix(mix(mix(v(p000),v(p100),x),mix(v(p010),v(p110),x),y),mix(mix(v(p001),v(p101),x),mix(v(p011),v(p111),x),y),z).roundToInt().coerceIn(0,255)
        }
        return (pixel and -0x1000000) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
    companion object {
        fun create(style:PhotoStyle,grade:ColorGrade=ColorGrade(),size:Int=17):StyleLut {
            val film=FilmProfiles.profile(style)!=null
            val matrix=grade.matrix(if(film) PhotoStyle.ORIGINAL else style);val curve=grade.lut()
            val pixels=IntArray(size*size*size) {i->
                val r=(i%size)/(size-1f);val b=(i/size%size)/(size-1f);val g=(i/(size*size))/(size-1f)
                val rgb=if(film) FilmProfiles.apply(style,r,g,b) else floatArrayOf(r,g,b)
                val strength=if(film) grade.strength.coerceIn(0f,1f) else 0f
                fun mix(a:Float,v:Float)=((a+(v-a)*strength)*255).roundToInt().coerceIn(0,255)
                val input=0xff000000.toInt() or (mix(r,rgb[0]) shl 16) or (mix(g,rgb[1]) shl 8) or mix(b,rgb[2])
                grade.transform(input,matrix,curve)
            }
            return StyleLut(pixels,size)
        }
    }
}
