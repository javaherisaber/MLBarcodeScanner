package com.buildtoapp.mlbarcodescanner

import androidx.camera.core.ImageProxy
import com.google.mlkit.common.MlKitException

/**
 * An interface to process the images with different vision detectors and custom image models.
 */
internal interface VisionImageProcessor {
    /**
     * Processes ImageProxy image data, e.g. used for CameraX live preview case.
     */
    @Throws(MlKitException::class)
    fun processImageProxy(image: ImageProxy, graphicOverlay: GraphicOverlay)

    /**
     * Stops the underlying machine learning model and release resources.
     */
    fun stop()
}