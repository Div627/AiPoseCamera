package com.aipose.camera.camera

import android.graphics.*
import android.os.Build
import android.view.View
import com.aipose.camera.diagnostics.Diagnostics
import com.aipose.camera.diagnostics.DiagnosticStore.Event

/** Same 17³ cube and trilinear sampling as the JPEG and thumbnail paths. */
class PreviewColorRenderer {
    private var previous:StyleLut?=null
    fun apply(view:View,lut:StyleLut) {
        if(previous===lut) return
        previous=lut
        if(Build.VERSION.SDK_INT<33) return
        try {
            val shader=RuntimeShader("""
                uniform shader content;
                uniform shader cube;
                uniform float n;
                float3 sampleCube(float3 p) { return cube.eval(float2(p.b*n+p.r+.5,p.g+.5)).rgb; }
                half4 main(float2 xy) {
                    float4 src=content.eval(xy);
                    float3 p=clamp(src.rgb/max(src.a,0.0001),0.0,1.0)*(n-1.0);
                    float3 lo=floor(p), hi=min(lo+1.0,n-1.0), t=p-lo;
                    float3 a=mix(mix(sampleCube(lo),sampleCube(float3(hi.r,lo.g,lo.b)),t.r),
                                 mix(sampleCube(float3(lo.r,hi.g,lo.b)),sampleCube(float3(hi.r,hi.g,lo.b)),t.r),t.g);
                    float3 b=mix(mix(sampleCube(float3(lo.r,lo.g,hi.b)),sampleCube(float3(hi.r,lo.g,hi.b)),t.r),
                                 mix(sampleCube(float3(lo.r,hi.g,hi.b)),sampleCube(hi),t.r),t.g);
                    return half4(mix(a,b,t.b)*src.a,src.a);
                }
            """.trimIndent())
            val bitmap=Bitmap.createBitmap(lut.colors,lut.size*lut.size,lut.size,Bitmap.Config.ARGB_8888)
            val sampler=BitmapShader(bitmap,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP)
            sampler.setFilterMode(BitmapShader.FILTER_MODE_NEAREST)
            shader.setInputShader("cube",sampler);shader.setFloatUniform("n",lut.size.toFloat())
            view.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader,"content"))
        } catch(e:Exception) {view.setRenderEffect(null);Diagnostics.error(Event.FRAME_ERROR,e)}
    }
}
