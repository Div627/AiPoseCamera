package com.aipose.camera.camera

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.abs

/** Optional real-image replay. Uses the same production meter and LUT, not a reimplementation. */
class ReplayFixturesTest {
    @Test fun replayPublicFixturesThroughProductionColorPipeline() {
        val root=File("../.local-validation")
        assumeTrue(File(root,"portrait.ppm").exists() && File(root,"mountain.ppm").exists())
        val results=JSONArray()
        for(name in listOf("portrait","mountain")) for(condition in listOf("normal","dark","backlit")) {
            val bytes=File(root,"$name.ppm").readBytes()
            val lines=bytes.indices.filter{bytes[it]==10.toByte()}.take(3)
            check(lines.size==3 && String(bytes,0,lines[0])=="P6")
            val dimensions=String(bytes,lines[0]+1,lines[1]-lines[0]-1).trim().split(" ")
            val width=dimensions[0].toInt();val height=dimensions[1].toInt();val start=lines[2]+1
            check(bytes.size-start==width*height*3)
            val input=IntArray(width*height){i->0xff000000.toInt() or ((bytes[start+i*3].toInt() and 255) shl 16) or ((bytes[start+i*3+1].toInt() and 255) shl 8) or (bytes[start+i*3+2].toInt() and 255)}
            for(i in input.indices) {
                val x=(i%width+.5f)/width;val y=(i/width+.5f)/height
                val gain=when(condition){"dark"->.08f;"backlit"->if(x in .28f.. .64f && y in .05f.. .40f) .28f else 1f;else->1f}
                fun c(shift:Int)=((input[i] ushr shift and 255)*gain).toInt()
                input[i]=0xff000000.toInt() or (c(16) shl 16) or (c(8) shl 8) or c(0)
            }
            val samples=IntArray(1024){i->input[((i/32+.5f)*height/32).toInt().coerceAtMost(height-1)*width+((i%32+.5f)*width/32).toInt().coerceAtMost(width-1)]}
            val faces=if(name=="portrait") listOf(FaceRegion(.28f,.05f,.64f,.4f)) else emptyList()
            val reading=SceneOptimizer.measure(samples,faces)
            val meter=SceneOptimizer();var choice:SceneOptimizer.Choice?=null
            repeat(3){choice=meter.update(reading,name=="portrait",0f)}
            val selected=choice!!
            val lut=StyleLut.create(selected.style,ColorGrade(strength=.6f,shadows=selected.shadows,highlights=selected.highlights))
            val started=System.nanoTime();val output=IntArray(input.size){lut.apply(input[it])};val elapsed=(System.nanoTime()-started)/1_000_000
            fun clipped(pixels:IntArray)=pixels.count{p->listOf(0,8,16).any {p ushr it and 255==255}}.toFloat()/pixels.size
            val before=clipped(input);val after=clipped(output)
            assertTrue("New clipping $name/$condition: $before -> $after",after<=before+.002f)
            if(condition=="dark") assertEquals(PhotoStyle.ORIGINAL,selected.style)
            var delta=0.0
            for(i in input.indices) for(shift in listOf(0,8,16)) delta+=abs((input[i] ushr shift and 255)-(output[i] ushr shift and 255))
            delta/=input.size*3
            assertTrue("Excessive global alteration $name/$condition",delta<10)
            fun writePixels(pixels:IntArray,suffix:String) {
                File(root,"$name-$condition-$suffix.ppm").outputStream().use {stream->
                    stream.write("P6\n$width $height\n255\n".toByteArray())
                    val rgb=ByteArray(pixels.size*3)
                    for(i in pixels.indices) {rgb[i*3]=(pixels[i] ushr 16).toByte();rgb[i*3+1]=(pixels[i] ushr 8).toByte();rgb[i*3+2]=pixels[i].toByte()}
                    stream.write(rgb)
                }
            }
            writePixels(input,"before");writePixels(output,"after")
            results.put(JSONObject().put("sample",name).put("condition",condition).put("width",width).put("height",height)
                .put("style",selected.style.name).put("sensorEv",selected.exposureEv).put("shadows",selected.shadows).put("highlights",selected.highlights)
                .put("clippedBefore",before).put("clippedAfter",after).put("meanChannelChange",delta).put("lutMs",elapsed))
        }
        File(root,"replay-metrics.json").writeText(results.toString(2))
    }
}
