package com.aipose.camera.camera

import kotlin.math.*

enum class StyleFamily(val label:String) { NATURAL("自然"), FUJI("富士"), KODAK("柯达"), RICOH("理光"), LEICA("徕卡"), AGFA("Agfa"), MONO("黑白") }

/** Authored approximations of published tonal/color characteristics, not manufacturer LUTs.
 * References and calibration limits: docs/camera-style-references.md. */
object FilmProfiles {
    data class Profile(val curve:FloatArray,val saturation:Float=1f,val greenSaturation:Float=1f,val blueSaturation:Float=1f,
        val greenHue:Float=0f,val blueHue:Float=0f,val shadow:FloatArray=floatArrayOf(0f,0f,0f),
        val highlight:FloatArray=floatArrayOf(0f,0f,0f),val skinProtection:Float=.5f,val mono:FloatArray?=null)
    private fun curve(vararg y:Float)=floatArrayOf(*y)
    private val profiles=mapOf(
        PhotoStyle.F_PROVIA to Profile(curve(0f,.105f,.24f,.51f,.78f,.925f,1f),1.06f,1.03f,1.04f),
        PhotoStyle.F_VELVIA to Profile(curve(0f,.072f,.19f,.51f,.82f,.95f,1f),1.22f,1.12f,1.12f,greenHue=-.006f,blueHue=-.008f,skinProtection=.75f),
        PhotoStyle.F_ASTIA to Profile(curve(.008f,.14f,.27f,.52f,.755f,.895f,.987f),1.05f,1.04f,1.03f,highlight=floatArrayOf(.008f,.002f,-.006f),skinProtection=.85f),
        PhotoStyle.F_CHROME to Profile(curve(.012f,.085f,.205f,.475f,.735f,.9f,.985f),.78f,.85f,.82f,.018f,-.022f,floatArrayOf(-.008f,.008f,.014f),floatArrayOf(.01f,.003f,-.008f)),
        PhotoStyle.F_NEGATIVE to Profile(curve(.025f,.08f,.19f,.48f,.79f,.935f,.99f),.9f,.8f,.92f,.025f,-.008f,floatArrayOf(-.012f,.012f,.002f),floatArrayOf(.02f,.004f,-.01f),.7f),
        PhotoStyle.K_PORTRA to Profile(curve(.018f,.145f,.28f,.53f,.765f,.90f,.987f),.93f,.96f,.94f,shadow=floatArrayOf(.005f,.003f,-.004f),highlight=floatArrayOf(.013f,.004f,-.008f),skinProtection=.9f),
        PhotoStyle.K_GOLD to Profile(curve(.012f,.105f,.245f,.535f,.80f,.935f,.995f),1.09f,.96f,.97f,shadow=floatArrayOf(.012f,.004f,-.009f),highlight=floatArrayOf(.023f,.01f,-.014f),skinProtection=.75f),
        PhotoStyle.K_EKTAR to Profile(curve(0f,.075f,.205f,.51f,.82f,.952f,1f),1.2f,1.08f,1.1f,blueHue=-.005f,skinProtection=.75f),
        PhotoStyle.R_POSITIVE to Profile(curve(.004f,.07f,.195f,.485f,.795f,.94f,.995f),1.13f,.92f,1.08f,.01f,-.012f,floatArrayOf(-.004f,.004f,.009f)),
        PhotoStyle.R_NEGATIVE to Profile(curve(.026f,.155f,.285f,.53f,.76f,.90f,.987f),.86f,.92f,.91f,shadow=floatArrayOf(.01f,.006f,-.005f),highlight=floatArrayOf(.012f,.004f,-.005f),skinProtection=.8f),
        PhotoStyle.L_NATURAL to Profile(curve(0f,.09f,.225f,.50f,.765f,.925f,1f),.97f,.98f,.98f,skinProtection=.8f),
        PhotoStyle.L_VIVID to Profile(curve(0f,.085f,.22f,.515f,.805f,.945f,1f),1.13f,1.05f,1.07f,skinProtection=.8f),
        PhotoStyle.A_APX to Profile(curve(.004f,.105f,.245f,.51f,.79f,.935f,.995f),mono=floatArrayOf(.30f,.59f,.11f)),
        PhotoStyle.F_ACROS to Profile(curve(.004f,.085f,.205f,.485f,.775f,.93f,.995f),mono=floatArrayOf(.34f,.55f,.11f)),
        PhotoStyle.R_HARD_MONO to Profile(curve(0f,.035f,.135f,.48f,.87f,.98f,1f),mono=floatArrayOf(.27f,.60f,.13f))
    )
    fun profile(style:PhotoStyle)=profiles[style]
    private val x=floatArrayOf(0f,.125f,.25f,.5f,.75f,.9f,1f)
    fun tone(value:Float,points:FloatArray):Float {
        val v=value.coerceIn(0f,1f);val i=(0..5).firstOrNull {v<=x[it+1]} ?: 5
        return points[i]+(points[i+1]-points[i])*(v-x[i])/(x[i+1]-x[i])
    }
    fun apply(style:PhotoStyle,r:Float,g:Float,b:Float):FloatArray {
        val p=profiles[style] ?: return floatArrayOf(r,g,b)
        p.mono?.let {weights->val y=tone(r*weights[0]+g*weights[1]+b*weights[2],p.curve);return floatArrayOf(y,y,y)}
        val max=maxOf(r,g,b);val min=minOf(r,g,b);val delta=max-min
        var h=if(delta<.00001f) 0f else when(max) {r->((g-b)/delta/6+1)%1;g->((b-r)/delta+2)/6;else->((r-g)/delta+4)/6}
        fun weight(center:Float,width:Float):Float {val d=abs(h-center).let {minOf(it,1-it)};return (1-d/width).coerceIn(0f,1f)}
        val green=weight(.33f,.18f);val blue=weight(.62f,.17f)
        val skin=weight(.075f,.10f)*(delta/.15f).coerceIn(0f,1f)
        val saturation=1+(p.saturation-1)*(1-skin*p.skinProtection)
        val s=(if(max>0) delta/max else 0f)*saturation*(1+(p.greenSaturation-1)*green+(p.blueSaturation-1)*blue)
        h=(h+p.greenHue*green+p.blueHue*blue+1)%1
        val hh=h*6;val c=max*s.coerceIn(0f,1f);val z=c*(1-abs(hh%2-1));val m=max-c
        val rgb=when(hh.toInt()) {0->floatArrayOf(c,z,0f);1->floatArrayOf(z,c,0f);2->floatArrayOf(0f,c,z);3->floatArrayOf(0f,z,c);4->floatArrayOf(z,0f,c);else->floatArrayOf(c,0f,z)}
        val y=.213f*r+.715f*g+.072f*b
        for(i in 0..2) rgb[i]=(tone(rgb[i]+m,p.curve)+p.shadow[i]*4*y*(1-y)*(1-y)+p.highlight[i]*4*y*y*(1-y)).coerceIn(0f,1f)
        return rgb
    }
}
