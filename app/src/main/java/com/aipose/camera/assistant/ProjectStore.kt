package com.aipose.camera.assistant

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aipose.camera.camera.CameraMode
import com.aipose.camera.camera.PhotoStyle
import org.json.JSONArray
import org.json.JSONObject

/** All writes occur on the ViewModel's IO dispatcher. SQLite transactions are process-safe. */
class ProjectStore(context: Context) : SQLiteOpenHelper(context, "shooting-projects.db", null, 1) {
    init { setWriteAheadLoggingEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE projects (id TEXT PRIMARY KEY, title TEXT NOT NULL, updated INTEGER NOT NULL, body TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun list(): List<ProjectSummary> = readableDatabase.rawQuery(
        "SELECT id,title,updated FROM projects ORDER BY updated DESC", null
    ).use { c -> buildList { while(c.moveToNext()) add(ProjectSummary(c.getString(0),c.getString(1),c.getLong(2))) } }
    fun load(id: String): ShootingProject? = readableDatabase.rawQuery(
        "SELECT body FROM projects WHERE id=?", arrayOf(id)
    ).use { c -> if(c.moveToFirst()) runCatching {ProjectCodec.decode(c.getString(0))}.getOrNull() else null }
    fun save(project: ShootingProject) {
        val values = ContentValues().apply {
            put("id",project.id); put("title",project.title); put("updated",project.updated); put("body",ProjectCodec.encode(project))
        }
        writableDatabase.insertWithOnConflict("projects",null,values,SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun delete(id: String) { writableDatabase.delete("projects","id=?",arrayOf(id)) }
}

object ProjectCodec {
    fun encode(p: ShootingProject): String = JSONObject().apply {
        put("id",p.id); put("title",p.title); put("updated",p.updated)
        put("messages",JSONArray().apply { p.messages.forEach { put(JSONObject().put("id",it.id).put("role",it.role).put("text",it.text)) } })
        p.plan?.let { put("plan",planJson(it)) }
        put("clips",JSONArray().apply { p.clips.forEach { put(JSONObject().put("shot",it.shotId).put("file",it.file).put("duration",it.durationMs).put("start",it.startMs)) } })
        put("exports",JSONArray(p.exports)); put("photos",JSONArray(p.photos))
    }.toString()
    fun planJson(p: ShootingPlan): JSONObject = JSONObject().apply {
        put("id",p.id); put("kind",p.kind); put("title",p.title); put("description",p.description)
        put("style",p.style.name); put("mode",p.mode.name); put("videoFormat",p.videoFormat.name)
        put("shots",JSONArray().apply { p.shots.forEach { put(JSONObject().put("id",it.id).put("title",it.title).put("direction",it.direction)) } })
    }
    fun plan(o: JSONObject): ShootingPlan {
        val kind = o.getString("kind"); require(kind in listOf("photo","video"))
        val style = PhotoStyle.entries.firstOrNull { it.name==o.optString("style") } ?: PhotoStyle.SCENIC
        val mode = CameraMode.entries.firstOrNull { it.name==o.optString("mode") } ?: CameraMode.LANDSCAPE
        val shots=o.optJSONArray("shots") ?: JSONArray()
        require(shots.length()<=8)
        val list=(0 until shots.length()).map { i -> shots.getJSONObject(i).let { Shot(it.getString("id").take(80),it.getString("title").take(80),it.getString("direction").take(300)) } }
        require(list.map { it.id }.distinct().size==list.size)
        require(kind!="video" || list.size in 2..8)
        return ShootingPlan(id=o.optString("id").ifBlank {newId()},kind=kind,title=o.getString("title").take(80),
            description=o.getString("description").take(800),style=style,mode=mode,shots=list,
            videoFormat=VideoFormat.entries.firstOrNull {it.name==o.optString("videoFormat")} ?: VideoFormat.PORTRAIT)
    }
    fun decode(text: String): ShootingProject {
        val o=JSONObject(text)
        fun strings(key: String): List<String> = o.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
        val messages=o.getJSONArray("messages"); val clips=o.getJSONArray("clips")
        return ShootingProject(o.getString("id"),o.getString("title"),o.getLong("updated"),
            (0 until messages.length()).map { messages.getJSONObject(it).let { m -> ChatMessage(m.getString("id"),m.getString("role"),m.getString("text")) } },
            o.optJSONObject("plan")?.let(::plan),
            (0 until clips.length()).map { clips.getJSONObject(it).let { c -> LocalClip(c.getString("shot"),c.getString("file"),c.getLong("duration"),c.optLong("start",0).also {require(it>=0)}) } },
            strings("exports"),strings("photos"))
    }
}
