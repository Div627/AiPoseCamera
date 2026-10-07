package com.aipose.camera.assistant

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

@UnstableApi
@Composable
fun VideoPlayerScreen(path:String,startMs:Long=0,durationMs:Long?=null,onBack:()->Unit) {
    val context=LocalContext.current
    val player=remember(path,startMs,durationMs) {ExoPlayer.Builder(context).build().apply {
        val item=MediaItem.Builder().setUri(Uri.fromFile(File(path)))
        durationMs?.let {item.setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setStartPositionMs(startMs).setEndPositionMs(startMs+it).build())}
        setMediaItem(item.build());prepare();playWhenReady=true
    }}
    val lifecycle=LocalLifecycleOwner.current
    DisposableEffect(player,lifecycle) {
        val observer=LifecycleEventObserver {_,event -> if(event==Lifecycle.Event.ON_PAUSE) player.pause()}
        lifecycle.lifecycle.addObserver(observer)
        onDispose {lifecycle.lifecycle.removeObserver(observer);player.release()}
    }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        TextButton(onClick=onBack){Text("返回项目")}
        AndroidView(factory={PlayerView(it).apply {this.player=player}},modifier=Modifier.fillMaxWidth().weight(1f))
        Text("本机视频 · 原素材保留",Modifier.padding(20.dp))
    }
}
