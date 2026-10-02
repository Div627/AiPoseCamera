package com.aipose.camera.camera

/** Explicit user intent. Detection never changes this value. Each mode owns a fresh keyed session. */
enum class CameraMode(val label:String) {
    PORTRAIT("人像"), LANDSCAPE("风景");
    val intents get()=when(this) {
        PORTRAIT->listOf(TravelIntent.AUTO,TravelIntent.ENVIRONMENT,TravelIntent.TOGETHER)
        LANDSCAPE->listOf(TravelIntent.AUTO,TravelIntent.MOUNTAIN,TravelIntent.WATERFALL,TravelIntent.COAST,TravelIntent.AURORA)
    }
    val analyzesFrames get()=true
    val usesPoseModel get()=this==PORTRAIT
    val allowsSubjectSelection get()=this==LANDSCAPE
    fun effectiveCount(detected:Int?):Int?=if(this==LANDSCAPE) 0 else detected
    fun permitsAutoCapture(detected:Int?):Boolean=when(this){PORTRAIT->detected in 1..4;LANDSCAPE->true}
}
enum class CameraPanel { NONE, TOOLS, POSES, FILTERS, REVIEW }
