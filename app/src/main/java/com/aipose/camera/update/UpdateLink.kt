package com.aipose.camera.update

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Accept a real HTTPS APK path, including signed URLs with query parameters. */
object UpdateLink {
    fun parse(value: String?): String? {
        val url = value?.trim()?.removePrefix("\uFEFF")?.trim()?.toHttpUrlOrNull() ?: return null
        if (url.scheme != "https" || url.username.isNotEmpty() || url.password.isNotEmpty()) return null
        if (!url.pathSegments.last().endsWith(".apk", ignoreCase = true)) return null
        return url.newBuilder().fragment(null).build().toString()
    }
}
