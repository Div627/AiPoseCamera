package com.aipose.camera.diagnostics

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.aipose.camera.BuildConfig
import java.io.File
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.aipose.camera.diagnostics.DiagnosticStore.Event
import com.aipose.camera.diagnostics.DiagnosticStore.Field

object Diagnostics {
    private var store:DiagnosticStore?=null
    private val worker=Executors.newSingleThreadExecutor()
    @Synchronized fun install(context:Context) {
        if(store!=null) return
        store=DiagnosticStore(File(context.filesDir,"diagnostics"))
        val previous=Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler {thread,error->
            runCatching {store?.append(Event.CRASH,error=error)}
            previous?.uncaughtException(thread,error) ?: android.os.Process.killProcess(android.os.Process.myPid())
        }
        event(Event.APP_START,Field.SDK to Build.VERSION.SDK_INT,Field.VERSION to BuildConfig.VERSION_CODE,Field.PAGE_SIZE to android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE))
    }
    fun event(event:Event,vararg fields:Pair<Field,Number>) {worker.execute {runCatching {store?.append(event,fields.toMap())}}}
    fun error(event:Event,error:Throwable,vararg fields:Pair<Field,Number>) {worker.execute {runCatching {store?.append(event,fields.toMap(),error)}}}
    suspend fun export(context:Context) {
        try {
            val report=withContext(Dispatchers.IO) {
                val dir=File(context.cacheDir,"diagnostics").apply {mkdirs()}
                dir.listFiles()?.filter {it.name.endsWith(".zip") && System.currentTimeMillis()-it.lastModified()>86_400_000}?.forEach {it.delete()}
                val file=File(dir,"yingke-diagnostics-${System.currentTimeMillis()}.zip")
                val snapshot=worker.submit<Map<String,String>> {store?.snapshot().orEmpty()}.get()
                ZipOutputStream(file.outputStream()).use {zip->
                    fun entry(name:String,text:String) {zip.putNextEntry(ZipEntry(name));zip.write(text.toByteArray());zip.closeEntry()}
                    entry("device.txt","映刻相机 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT}\nABI ${Build.SUPPORTED_ABIS.joinToString()}\n\nMODEL: 1=pose, 2=face, 3=tap segmenter\nMODE: 0=portrait, 1=landscape\nOnly numeric events and exception classes/stacks. No photos, API keys, URLs or model responses.\n")
                    snapshot.forEach { (name,body)->entry(name,body) }
                }
                file
            }
            val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",report)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type="application/zip";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },"导出诊断日志"))
        } catch(_:Exception) {Toast.makeText(context,"日志导出失败，请重试",Toast.LENGTH_SHORT).show()}
    }
}
