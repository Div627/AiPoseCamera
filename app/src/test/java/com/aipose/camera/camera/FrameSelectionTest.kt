package com.aipose.camera.camera

import com.aipose.camera.assistant.*
import org.junit.Assert.*
import org.junit.Test

class FrameSelectionTest {
    private fun frame(time: Long, score: Float = .8f, hash: Long = time) = FrameCandidate(time,score,hash,.5f)
    @Test fun sharpStructureOutranksUniformImage() {
        val flat=IntArray(32*32) {0xff808080.toInt()}
        val edges=IntArray(32*32) {i -> if((i/32+i%32)%2==0) 0xff303030.toInt() else 0xffb0b0b0.toInt()}
        assertTrue(FrameSelection.quality(edges,32,32).score>FrameSelection.quality(flat,32,32).score)
    }
    @Test fun clippedFrameScoresBelowBalancedFrame() {
        assertTrue(FrameSelection.quality(IntArray(64){0xff808080.toInt()},8,8).score>
            FrameSelection.quality(IntArray(64){-1},8,8).score)
    }
    @Test fun duplicateCandidatesKeepHigherScore() {
        val selected=FrameSelection.photoCandidates(listOf(frame(0,.5f,42),frame(1000,.9f,42)))
        assertEquals(listOf(1000L),selected.map {it.timeMs})
    }
    @Test fun selectedVideoRangesNeverOverlapOrExceedSource() {
        val duration=18000L
        val points=(0..17).map {frame(it*1000L,.8f)}
        val selected=FrameSelection.segments(points,duration)
        assertTrue(selected.size>=2)
        val ranges=selected.map {val range=FrameSelection.range(it,points,duration);range.first to range.last+1}
        ranges.forEach {assertTrue(it.first>=0 && it.second<=duration && it.second>it.first)}
        ranges.zipWithNext().forEach {(a,b)->assertTrue(a.second<=b.first)}
    }
    @Test fun lowQualityAndLikelyClosedEyesAreExcludedFromVideoSegments() {
        val frames=listOf(frame(0,.3f),frame(5000,.9f).copy(eyesOpen=.1f),frame(10000,.8f))
        assertEquals(listOf(10000L),FrameSelection.segments(frames,15000).map {it.timeMs})
    }
    @Test fun trimStaysInsidePortraitSceneAndAvoidsDuplicateScenes() {
        val frames=(0..19).map {i ->frame(i*500L,.8f,if(i<10) 0 else -1).copy(faces=if(i<10) 1 else 0,labels=if(i<10) listOf("Person") else listOf("Mountain"))}
        val selected=FrameSelection.segments(frames,10000)
        assertEquals(2,selected.size)
        val portrait=FrameSelection.range(selected.first(),frames,10000)
        val landscape=FrameSelection.range(selected.last(),frames,10000)
        assertTrue(portrait.last<4750)
        assertTrue(landscape.first>=4750)
    }
    @Test fun shortSourceCannotManufactureTwoShots() {
        assertTrue(FrameSelection.segments(listOf(frame(0)),800).isEmpty())
    }
    @Test fun videoOutputFormatSurvivesLocalSaveAndLegacyProjectsStayPortrait() {
        val p=ShootingProject(plan=LocalShootingPlanner.respond("vlog").plan.copy(videoFormat=VideoFormat.LANDSCAPE))
        assertEquals(VideoFormat.LANDSCAPE,ProjectCodec.decode(ProjectCodec.encode(p)).plan!!.videoFormat)
        val body=org.json.JSONObject(ProjectCodec.encode(p));body.getJSONObject("plan").remove("videoFormat")
        assertEquals(VideoFormat.PORTRAIT,ProjectCodec.decode(body.toString()).plan!!.videoFormat)
    }
    @Test fun clipTrimRoundTripAndLegacyDefaultsAreCompatible() {
        val p=ShootingProject(clips=listOf(LocalClip("shot","/clip.mp4",3500,8500)))
        assertEquals(p,ProjectCodec.decode(ProjectCodec.encode(p)))
        val body=org.json.JSONObject(ProjectCodec.encode(p))
        body.getJSONArray("clips").getJSONObject(0).remove("start")
        assertEquals(0L,ProjectCodec.decode(body.toString()).clips.first().startMs)
    }
}
