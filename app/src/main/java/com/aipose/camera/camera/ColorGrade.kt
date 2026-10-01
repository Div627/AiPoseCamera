package com.aipose.camera.camera

import kotlin.math.pow
import kotlin.math.roundToInt

/** Pixel adjustments, not sensor exposure or camera white balance. Originals remain untouched. */
data class ColorGrade(val strength:Float=1f,val postEv:Float=0f,val contrast:Float=1f,val saturation:Float=1f,
    val warmth:Float=0f,val tint:Float=0f,val highlights:Float=0f,val shadows:Float=0f) {
    fun needsCopy(style:PhotoStyle)=style!=PhotoStyle.ORIGINAL && strength>0f || copy(strength=1f)!=ColorGrade()
    fun matrix(style:PhotoStyle):FloatArray {
        val base=style.matrix();val blend=strength.coerceIn(0f,1f)
        val mixed=FloatArray(20){i->val identity=if(i in listOf(0,6,12,18)) 1f else 0f;identity+(base[i]-identity)*blend}
        val gain=2.0.pow(postEv.coerceIn(-2f,2f).toDouble()).toFloat()
        val sat=saturation.coerceIn(0f,2f);val cont=contrast.coerceIn(.5f,1.5f)
        val luma=floatArrayOf(.213f,.715f,.072f);val result=FloatArray(20)
        for(row in 0..2) {
            for(col in 0..3) for(k in 0..2) result[row*5+col]+=gain*cont*((1-sat)*luma[k]+if(row==k) sat else 0f)*mixed[k*5+col]
            result[row*5+4]=128*(1-cont)+when(row){0->warmth*12+tint*4;1->-tint*8;else->-warmth*12+tint*4}
            for(k in 0..2) result[row*5+4]+=gain*cont*((1-sat)*luma[k]+if(row==k) sat else 0f)*mixed[k*5+4]
        }
        result[18]=1f;return result
    }
    fun curve(value:Float):Float {
        val v=value.coerceIn(0f,1f)
        return (v+shadows.coerceIn(-1f,1f)*.18f*(1-v)*(1-v)+highlights.coerceIn(-1f,1f)*.18f*v*v).coerceIn(0f,1f)
    }
    fun lut()=IntArray(256){(curve(it/255f)*255).roundToInt()}
    fun transform(pixel:Int,matrix:FloatArray,lut:IntArray):Int {
        val p=PhotoStyle.transform(pixel,matrix)
        return (p and -0x1000000) or (lut[p ushr 16 and 255] shl 16) or (lut[p ushr 8 and 255] shl 8) or lut[p and 255]
    }
}
