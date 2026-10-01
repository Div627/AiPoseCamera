package com.aipose.camera.camera

import kotlin.math.ln
import kotlin.math.exp

object ZoomScale {
    fun position(ratio:Float,min:Float,max:Float):Float = if(max<=min || min<=0) 0f else (ln(ratio.coerceIn(min,max)/min)/ln(max/min)).coerceIn(0f,1f)
    fun ratio(position:Float,min:Float,max:Float):Float = if(max<=min || min<=0) min else (min*exp(position.coerceIn(0f,1f)*ln(max/min))).coerceIn(min,max)
    fun presets(min:Float,max:Float):List<Float> = (listOf(min)+listOf(1f,2f,5f,10f).filter {it in min..max}).distinct().filter {it<=10f || it==min}
}
