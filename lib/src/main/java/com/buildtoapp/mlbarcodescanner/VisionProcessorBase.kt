package com.buildtoapp.mlbarcodescanner

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.GuardedBy
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.android.gms.tasks.Tasks
import com.google.android.odml.image.MediaMlImageBuilder
import com.google.android.odml.image.MlImage
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer

/**
 * Abstract base class for ML Kit frame processors. Subclasses need to implement {@link
 * #onSuccess(T, FrameMetadata, GraphicOverlay)} to define what they want to with the detection
 * results and {@link #detectInImage(VisionImage)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
internal abstract class VisionProcessorBase<T> : VisionImageProcessor {

    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    // Whether this processor is already shut down
    private var isShutdown = false

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private var latestImage: ByteBuffer? = null
    @GuardedBy("this")
    private var latestImageMetaData: FrameMetadata? = null

    // To keep the images and metadata in process.
    @GuardedBy("this")
    private var processingImage: ByteBuffer? = null
    @GuardedBy("this")
    private var processingMetaData: FrameMetadata? = null

    @ExperimentalGetImage
    override fun processImageProxy(image: ImageProxy, graphicOverlay: GraphicOverlay) {
        if (isShutdown) {
            return
        }
        val bitmap: Bitmap? = BitmapUtils.getBitmap(image)
        val mediaImage = image.image ?: error("image should not be null here")

        if (isMlImageEnabled(graphicOverlay.context)) {
            val mlImage =
                MediaMlImageBuilder(mediaImage).setRotation(image.imageInfo.rotationDegrees).build()
            requestDetectInImage(
                mlImage,
                graphicOverlay,
                bitmap
            )
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                // Currently MlImage doesn't support ImageProxy directly, so we still need to call
                // ImageProxy.close() here.
                .addOnCompleteListener { image.close() }

            return
        }

        requestDetectInImage(
            InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees),
            graphicOverlay,
            bitmap
        )
            // When the image is from CameraX analysis use case, must call image.close() on received
            // images when finished using them. Otherwise, new images may not be received or the camera
            // may stall.
            .addOnCompleteListener { image.close() }
    }

    private fun requestDetectInImage(
        image: InputImage,
        graphicOverlay: GraphicOverlay,
        originalCameraImage: Bitmap?
    ): Task<T> {
        return setUpListener(detectInImage(image), graphicOverlay, originalCameraImage)
    }

    private fun requestDetectInImage(
        image: MlImage,
        graphicOverlay: GraphicOverlay,
        originalCameraImage: Bitmap?
    ): Task<T> {
        return setUpListener(detectInImage(image), graphicOverlay, originalCameraImage)
    }

    private fun setUpListener(
        task: Task<T>,
        graphicOverlay: GraphicOverlay,
        originalCameraImage: Bitmap?
    ): Task<T> {
        return task
            .addOnSuccessListener(executor) { results: T ->
                graphicOverlay.clear()
                if (originalCameraImage != null) {
                    graphicOverlay.add(CameraImageGraphic(graphicOverlay, originalCameraImage))
                }
                this@VisionProcessorBase.onSuccess(results, graphicOverlay)
                graphicOverlay.postInvalidate()
            }
            .addOnFailureListener(executor) { e: Exception ->
                graphicOverlay.clear()
                graphicOverlay.postInvalidate()
                this@VisionProcessorBase.onFailure(e)
            }
    }

    override fun stop() {
        executor.shutdown()
        isShutdown = true
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    protected open fun detectInImage(image: MlImage): Task<T> {
        return Tasks.forException(
            MlKitException(
                "MlImage is currently not demonstrated for this feature",
                MlKitException.INVALID_ARGUMENT
            )
        )
    }

    protected abstract fun onSuccess(results: T, graphicOverlay: GraphicOverlay)

    protected abstract fun onFailure(e: Exception)

    protected open fun isMlImageEnabled(context: Context?): Boolean {
        return false
    }
}
