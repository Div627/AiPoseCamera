package com.aipose.camera.camera

object PortraitDetection {
    fun visible(x:Float,y:Float,visibility:Float,presence:Float)=x in 0f..1f && y in 0f..1f && visibility>=.45f && presence>=.45f
    fun guidable(points:Map<Int,Pair<Float,Float>>)=points.containsKey(11) && points.containsKey(12) && listOf(0,11,12,23,24).count{points.containsKey(it)}>=3
    /** Partial poses count as people; do not fabricate missing joints for automatic capture. */
    fun count(poseCount:Int?,faceCount:Int?):Int?=if(poseCount==null && faceCount==null) null else maxOf(poseCount ?: 0,faceCount ?: 0)
}
