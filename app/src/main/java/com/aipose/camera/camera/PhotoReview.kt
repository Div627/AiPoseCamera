package com.aipose.camera.camera

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import com.aipose.camera.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sample before decode; EXIF transformations apply to originals and edited copies. */
object PhotoImage {
    suspend fun load(context: Context, uri: Uri, maximum: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maximum) sample *= 2
            val image = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 })
            } ?: return@withContext null
            val orientation = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
                }
            }
            val upright = if (matrix.isIdentity) image else Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
            if (upright !== image) image.recycle()
            upright.asImageBitmap()
        } catch (_: Exception) { null }
        catch (_: OutOfMemoryError) { null }
    }
}

@Composable
fun RecentPhotoButton(photo: RecentPhoto?, enabled: Boolean = true, onClick: () -> Unit) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, photo?.display) {
        value = null
        value = photo?.display?.let { PhotoImage.load(context, it, 192) }
    }
    Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceElevated)
        .border(1.dp, CameraDesign.Border, RoundedCornerShape(12.dp))
        .semantics { contentDescription = if (photo == null) "拍摄后在这里查看照片" else "查看最近照片"; role = Role.Button }
        .clickable(enabled = enabled && photo != null, onClick = onClick), contentAlignment = Alignment.Center) {
        if (image != null) Image(image!!, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Outlined.Photo, null, tint = TextSecondary)
        if (photo?.processing == true) CircularProgressIndicator(Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
    }
}

@Composable
fun PhotoReview(photo: RecentPhoto, onClose: () -> Unit) {
    val context = LocalContext.current
    var original by remember { mutableStateOf(false) }
    val uri = if (original) photo.original else photo.display
    var loaded by remember(uri) { mutableStateOf(false) }
    val image by produceState<ImageBitmap?>(null, uri) { value = null; loaded = false; value = PhotoImage.load(context, uri, 2048); loaded = true }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(BgDark).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "返回拍摄") }
                Text(if (original) "原片" else if (photo.processing) "正在生成成片" else "最近照片", style = MaterialTheme.typography.titleMedium)
                IconButton(enabled = loaded && image != null, onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"; putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri("照片", uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { context.startActivity(Intent.createChooser(share, "分享照片")) }
                }) { Icon(Icons.Outlined.Share, "分享照片") }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (image != null) Image(image!!, if (original) "原片" else "成片", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else if (!loaded) CircularProgressIndicator(color = TextPrimary)
                else Text("照片暂时无法读取，可能已从相册删除", Modifier.padding(24.dp), color = TextSecondary)
            }
            if (photo.dimensions.isNotBlank()) Text(photo.dimensions, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            if (photo.processing || photo.error != null) Text(photo.error ?: "原片已保存，可以继续拍摄", Modifier.padding(12.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (photo.edited != null) TextButton(onClick = { original = !original }) { Text(if (original) "查看成片" else "对比原片") }
                TextButton(onClick = onClose) { Text("继续拍摄") }
            }
        }
    }
}
