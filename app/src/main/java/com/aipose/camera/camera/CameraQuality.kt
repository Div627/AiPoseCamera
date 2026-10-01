package com.aipose.camera.camera

import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy

object CameraQuality {
    /** Prefer the highest native resolution CameraX exposes, including high-resolution streams. */
    fun capture(highResolution: Boolean = true) = ImageCapture.Builder()
        .setResolutionSelector(ResolutionSelector.Builder().setAllowedResolutionMode(if(highResolution) ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE else ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION).setResolutionStrategy(
            if(highResolution) ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY else ResolutionStrategy(Size(4032,3024),ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build())
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).setJpegQuality(98).build()
}
