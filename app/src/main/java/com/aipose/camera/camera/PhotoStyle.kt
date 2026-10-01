package com.aipose.camera.camera

import kotlin.math.roundToInt

/** Small, deterministic transforms shared by live preview and full-resolution JPEG processing. */
enum class PhotoStyle(val label: String, val contrast: Float, val saturation: Float, val lift: Float, val warmth: Float, val family:StyleFamily=StyleFamily.NATURAL, val description:String="自然调整") {
    ORIGINAL("原色",1f,1f,0f,0f), SCENIC("通透风景",1.012f,1.025f,0f,0f), SOFT("柔和提亮",.985f,.99f,3f,0f),
    PORTRAIT("自然人像",1.008f,1.015f,.5f,.5f), HIGHLIGHT("高光柔化",.97f,.99f,0f,0f), ICELAND("冰岛冷调",1.02f,.95f,0f,-2.5f),
    F_CHROME("Classic Chrome",1f,1f,0f,0f,StyleFamily.FUJI,"低饱和 · 纪实"),
    F_PROVIA("PROVIA",1f,1f,0f,0f,StyleFamily.FUJI,"均衡 · 通用"),
    F_VELVIA("Velvia",1f,1f,0f,0f,StyleFamily.FUJI,"鲜艳 · 风景"),
    F_ASTIA("ASTIA",1f,1f,0f,0f,StyleFamily.FUJI,"柔和 · 肤色"),
    F_NEGATIVE("Classic Negative",1f,1f,0f,0f,StyleFamily.FUJI,"复古 · 层次"),
    K_PORTRA("Portra 400",1f,1f,0f,0f,StyleFamily.KODAK,"柔和肤色"),
    K_GOLD("Gold 200",1f,1f,0f,0f,StyleFamily.KODAK,"暖调 · 日常"),
    K_EKTAR("Ektar 100",1f,1f,0f,0f,StyleFamily.KODAK,"明艳 · 通透"),
    R_POSITIVE("Positive Film",1f,1f,0f,0f,StyleFamily.RICOH,"浓郁 · 街拍"),
    R_NEGATIVE("Negative Film",1f,1f,0f,0f,StyleFamily.RICOH,"柔彩 · 怀旧"),
    L_NATURAL("Natural",1f,1f,0f,0f,StyleFamily.LEICA,"自然 · 深邃"),
    L_VIVID("Vivid",1f,1f,0f,0f,StyleFamily.LEICA,"鲜活 · 清晰"),
    A_APX("APX 100",1f,1f,0f,0f,StyleFamily.AGFA,"细腻黑白"),
    F_ACROS("ACROS",1f,1f,0f,0f,StyleFamily.MONO,"丰富灰阶"),
    R_HARD_MONO("GR 高反差",1f,1f,0f,0f,StyleFamily.MONO,"硬调黑白");
    fun matrix(): FloatArray {
        val luma=floatArrayOf(.213f,.715f,.072f)
        return FloatArray(20).also { m ->
            for (row in 0..2) {
                for (col in 0..2) m[row*5+col]=contrast*((1-saturation)*luma[col]+if(row==col) saturation else 0f)
                m[row*5+4]=128*(1-contrast)+lift+when(row){0->warmth;2->-warmth;else->0f}
            }
            m[18]=1f
        }
    }
    companion object {
        fun choose(mean: Float, highlights: Float, portrait: Boolean): PhotoStyle = when {
            highlights > .12f -> HIGHLIGHT
            mean < .30f -> SOFT
            portrait -> PORTRAIT
            else -> SCENIC
        }
        fun exposureEv(mean: Float, highlights: Float): Float = when {
            highlights > .12f -> -.33f
            mean < .24f -> .33f
            else -> 0f
        }
        fun transform(pixel: Int, matrix: FloatArray): Int {
            val r=(pixel ushr 16 and 255).toFloat();val g=(pixel ushr 8 and 255).toFloat();val b=(pixel and 255).toFloat()
            fun channel(i:Int)=(r*matrix[i]+g*matrix[i+1]+b*matrix[i+2]+matrix[i+4]).roundToInt().coerceIn(0,255)
            return (pixel and -0x1000000) or (channel(0) shl 16) or (channel(5) shl 8) or channel(10)
        }
    }
}
