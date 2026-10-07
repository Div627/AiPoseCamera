package com.aipose.camera.assistant

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

/** Deterministic on-device rough cut. No uploads, synthetic footage or claims of AI judging. */
@UnstableApi
class LocalVideoEditor(private val context: Context) {
    private var transformer: Transformer? = null
    private var temporary: File? = null
    fun export(project: ShootingProject, completed: (File?) -> Unit) {
        check(transformer==null)
        val plan=project.plan ?: return completed(null)
        val clips=plan.shots.mapNotNull { shot -> project.clips.firstOrNull {it.shotId==shot.id && File(it.file).exists()} }
        if(clips.size<2) return completed(null)
        val directory=ProjectMedia.directory(context,project.id)
        if(directory.usableSpace<160L*1024*1024) return completed(null)
        try {
        val output=File(directory,"${newId()}.pending.mp4")
        temporary=output
        val items=clips.map { clip ->
            val media=MediaItem.Builder().setUri(Uri.fromFile(File(clip.file)))
                .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs).setEndPositionMs(clip.startMs+minOf(5000,clip.durationMs)).build()).build()
            EditedMediaItem.Builder(media).setEffects(Effects(emptyList(),listOf(Presentation.createForWidthAndHeight(plan.videoFormat.width,plan.videoFormat.height,Presentation.LAYOUT_SCALE_TO_FIT)))).build()
        }
        val composition=Composition.Builder(EditedMediaItemSequence(items)).experimentalSetForceAudioTrack(true).build()
        transformer=Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object: Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    transformer=null; temporary=null
                    val target=File(directory,"${newId()}.mp4")
                    if(output.exists() && output.renameTo(target)) completed(target) else {output.delete();completed(null)}
                }
                override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                    transformer=null;temporary=null;output.delete();completed(null)
                }
            }).build()
        transformer!!.start(composition,output.absolutePath)
        } catch(_:Exception) {cancel();completed(null)}
    }
    fun cancel() {transformer?.cancel();transformer=null;temporary?.delete();temporary=null}
}
