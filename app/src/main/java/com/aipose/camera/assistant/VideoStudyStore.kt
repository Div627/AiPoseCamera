package com.aipose.camera.assistant

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class VideoStudyStore(context: Context, projectId: String) {
    val directory = ProjectMedia.directory(context, projectId)
    private val file = AtomicFile(File(directory, "video-study.json"))
    fun save(study: VideoStudy) {
        val body = JSONObject().apply {
            put("source", study.source); put("duration", study.durationMs)
            put("width", study.width); put("height", study.height); put("models", study.modelAvailable)
            fun encode(frames: List<FrameCandidate>) = JSONArray().apply {frames.forEach {f ->
                put(JSONObject().apply {
                    put("time", f.timeMs); put("score", f.score); put("hash", f.signature); put("brightness", f.brightness)
                    put("labels", JSONArray(f.labels)); put("faces", f.faces); f.eyesOpen?.let {put("eyes", it)}
                    put("x", f.centerX); put("y", f.centerY); put("preview", f.preview)
                })
            }}
            put("frames", encode(study.frames)); put("candidates", encode(study.candidates))
        }.toString().toByteArray()
        val output = file.startWrite()
        try {output.write(body); file.finishWrite(output)} catch(e: Exception) {file.failWrite(output); throw e}
    }
    fun load(): VideoStudy? = runCatching {
        val o = file.openRead().use {JSONObject(it.bufferedReader().readText())}
        fun frames(key: String): List<FrameCandidate> {
            val a = o.getJSONArray(key); require(a.length() <= 90)
            return (0 until a.length()).map {i -> a.getJSONObject(i).let {f ->
                val labels = f.getJSONArray("labels")
                FrameCandidate(f.getLong("time"), f.getDouble("score").toFloat(), f.getLong("hash"), f.getDouble("brightness").toFloat(),
                    (0 until labels.length()).map {labels.getString(it)}, f.getInt("faces"),
                    if(f.has("eyes")) f.getDouble("eyes").toFloat() else null,
                    f.getDouble("x").toFloat(), f.getDouble("y").toFloat(), f.getString("preview"))
            }}
        }
        VideoStudy(o.getString("source").also {require(File(it).isFile && File(it).canonicalFile.parentFile == directory.canonicalFile)},
            o.getLong("duration"), o.getInt("width"), o.getInt("height"), frames("frames"), frames("candidates"), o.getBoolean("models"))
    }.getOrNull()
}
