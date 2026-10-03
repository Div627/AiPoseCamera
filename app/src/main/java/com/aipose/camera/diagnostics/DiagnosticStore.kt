package com.aipose.camera.diagnostics

import java.io.File
import org.json.JSONObject

/** Numeric, explicitly named measurements only. Never accepts URLs, image data or credentials. */
class DiagnosticStore(private val directory:File, private val limit:Long=256*1024) {
    enum class Event { APP_START, CAMERA_BIND, CAMERA_ERROR, MODEL_READY, MODEL_ERROR, FRAME, FRAME_ERROR, TAP_START, TAP_RESULT, TAP_ERROR, STYLE, CAPTURE, CAPTURE_ERROR, SCANNER_OPEN, SCANNER_FRAME, SCANNER_ERROR, SCAN_RESULT, UPDATE, UPDATE_ERROR, CRASH }
    enum class Field { SDK, VERSION, PAGE_SIZE, MODE, MODEL, WIDTH, HEIGHT, ROTATION, DURATION_MS, RAW_PEOPLE, RELIABLE_PEOPLE, FACES, COUNT, POINTS, STALE, ACCEPTED, MASKS, STATUS, BYTES, STYLE, PERMISSION, MIN_ZOOM, MAX_ZOOM }
    @Synchronized fun append(event:Event, values:Map<Field,Number> = emptyMap(),error:Throwable?=null) {
        directory.mkdirs()
        val file=File(directory,"events.jsonl")
        if(file.length()>=limit) {val previous=File(directory,"previous.jsonl");previous.delete();file.renameTo(previous)}
        val row=JSONObject().put("timeMs",System.currentTimeMillis()).put("event",event.name)
        values.forEach {(key,value)->if(value.toDouble().isFinite()) row.put(key.name,value)}
        error?.let {
            row.put("errorClass",it.javaClass.name)
            row.put("stack",it.stackTrace.take(8).joinToString("\n") {frame->"${frame.className}.${frame.methodName}:${frame.lineNumber}"})
            // Exception messages can include server responses or tokens, so are deliberately excluded.
            row.put("causeClass",it.cause?.javaClass?.name ?: "")
        }
        file.appendText(row.toString()+"\n")
    }
    @Synchronized fun snapshot():Map<String,String> = listOf("previous.jsonl","events.jsonl").associateWith {File(directory,it).takeIf {f->f.exists()}?.readText().orEmpty()}
}
