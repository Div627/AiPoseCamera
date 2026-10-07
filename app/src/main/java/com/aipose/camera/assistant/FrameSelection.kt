package com.aipose.camera.assistant

import kotlin.math.abs
import kotlin.math.ln

/** Quality heuristics, not a learned aesthetic or motion score. */
object FrameSelection {
    data class Quality(val score: Float, val signature: Long, val brightness: Float)
    fun quality(pixels: IntArray, width: Int, height: Int): Quality {
        require(width >= 8 && height >= 8 && pixels.size == width * height)
        val light = FloatArray(pixels.size) { i ->
            val p = pixels[i]
            .2126f * (p ushr 16 and 255) + .7152f * (p ushr 8 and 255) + .0722f * (p and 255)
        }
        val mean = light.average().toFloat()
        var detail = 0.0
        for (y in 1 until height - 1) for (x in 1 until width - 1) {
            val i = y * width + x
            val edge = light[i - 1] + light[i + 1] + light[i - width] + light[i + width] - 4 * light[i]
            detail += edge * edge
        }
        val sharp = (ln(1 + detail / ((width - 2) * (height - 2))) / 9).toFloat().coerceIn(0f, 1f)
        val clipped = light.count { it < 12 || it > 245 }.toFloat() / light.size
        val exposure = (1f - abs(mean - 128) / 128).coerceIn(0f, 1f)
        val cells = FloatArray(64) { cell ->
            var sum = 0f; var count = 0
            for (y in cell / 8 * height / 8 until (cell / 8 + 1) * height / 8)
                for (x in cell % 8 * width / 8 until (cell % 8 + 1) * width / 8) {sum += light[y * width + x]; count++}
            sum / count
        }
        val average = cells.average()
        var hash = 0L
        cells.forEachIndexed { i, value -> if (value > average) hash = hash or (1L shl i) }
        return Quality((sharp * .65f + exposure * .25f + (1 - clipped) * .1f), hash, mean / 255)
    }
    fun photoCandidates(frames: List<FrameCandidate>, limit: Int = 8): List<FrameCandidate> {
        val result = mutableListOf<FrameCandidate>()
        for (frame in frames.sortedByDescending { it.score }) {
            if (result.none {java.lang.Long.bitCount(it.signature xor frame.signature) <= 5}) result += frame
            if (result.size >= limit) break
        }
        return result
    }
    fun range(frame: FrameCandidate, frames: List<FrameCandidate>, durationMs: Long): LongRange {
        val ordered=frames.sortedBy {it.timeMs}
        val index=ordered.indexOfFirst {it.timeMs==frame.timeMs}
        require(index>=0 && durationMs>0)
        fun eligible(f:FrameCandidate)=f.score>=.5f && (f.eyesOpen==null || f.eyesOpen>=.35f)
        fun same(a:FrameCandidate,b:FrameCandidate)=eligible(b) && (a.faces>0)==(b.faces>0) &&
            (a.labels.isEmpty() || b.labels.isEmpty() || a.labels.intersect(b.labels.toSet()).isNotEmpty())
        var left=index; var right=index
        while(left>0 && same(frame,ordered[left-1])) left--
        while(right<ordered.lastIndex && same(frame,ordered[right+1])) right++
        val lower=if(left==0) 0 else ordered[left].timeMs
        val upper=if(right==ordered.lastIndex) durationMs else ordered[right].timeMs+1
        val length=minOf(4000,upper-lower).coerceAtLeast(0)
        val start=(frame.timeMs-1500).coerceIn(lower,(upper-length).coerceAtLeast(lower))
        return start until start+length
    }
    fun segments(frames: List<FrameCandidate>, durationMs: Long): List<FrameCandidate> {
        if(durationMs < 2000) return emptyList()
        val selected = mutableListOf<FrameCandidate>()
        for(frame in frames.filter {it.score >= .5f && (it.eyesOpen == null || it.eyesOpen >= .35f)}.sortedByDescending {it.score}) {
            val range=range(frame,frames,durationMs)
            if(range.isEmpty() || range.last-range.first+1<1000) continue
            if(selected.none {other ->
                    val b=range(other,frames,durationMs)
                    (range.first<=b.last && b.first<=range.last) || java.lang.Long.bitCount(other.signature xor frame.signature)<=5
                }) selected += frame
            if(selected.size == 6) break
        }
        return selected.sortedBy {it.timeMs}
    }
}

data class FrameCandidate(
    val timeMs: Long, val score: Float, val signature: Long, val brightness: Float,
    val labels: List<String> = emptyList(), val faces: Int = 0, val eyesOpen: Float? = null,
    val centerX: Float = .5f, val centerY: Float = .5f, val preview: String = ""
)
data class VideoStudy(val source: String, val durationMs: Long, val width: Int, val height: Int,
    val frames: List<FrameCandidate>, val candidates: List<FrameCandidate>, val modelAvailable: Boolean)
