package com.buildtoapp.mlbarcodescanner

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * Delegate class to wrap all functionalities of barcode scanning and handling it's resources in a lifecycle aware manner
 *
 * @param callback listen to new scanned barcode
 * @param focusBoxSize width/height of the focus box in pixels (box must be square)
 * @param graphicOverlay overlay graphic to be drawn on screen to recognize barcode
 * @param previewView camera preview object in your view
 * @param lifecycleOwner lifecycle owner of your view (viewLifecycleOwner in fragment and this in activity)
 * @param context ui context
 * @param drawOverlay if set to true, will display a rectangle around detected barcode (default to true)
 * @param drawBanner if set to true, will display detected barcode value on top of it's rectangle (default to false)
 * @param targetResolution resolution of the camera view (default to 768 * 1024)
 * @param supportedBarcodeFormats list of all supported barcode formats (default to all)
 */
class MLBarcodeScanner(
    private val callback: MLBarcodeCallback,
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val focusBoxSize: Int,
    private val graphicOverlay: GraphicOverlay,
    private val previewView: PreviewView,
    private val drawOverlay: Boolean = true,
    private val drawBanner: Boolean = false,
    private val targetResolution: Size = Size(768, 1024),
    private val supportedBarcodeFormats: List<Int> = listOf(Barcode.FORMAT_ALL_FORMATS)
) : DefaultLifecycleObserver {

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private val cameraXViewModel: CameraXViewModel by lazy {
        ViewModelProvider(lifecycleOwner as ViewModelStoreOwner)[CameraXViewModel::class.java]
    }
    private lateinit var cameraSelector: CameraSelector
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var imageProcessor: VisionImageProcessor? = null

    init {
        initialize()
    }

    /**
     * initialize instance members
     */
    fun initialize() {
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        cameraXViewModel.processCameraProvider.observe(lifecycleOwner) { provider: ProcessCameraProvider? ->
            cameraProvider = provider
            bindAllCameraUseCases()
        }
    }

    /**
     * Stop processing images in camera
     */
    fun stop() {
        imageProcessor?.stop()
    }

    private fun bindAllCameraUseCases() {
        // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
        cameraProvider?.unbindAll()
        bindPreviewUseCase()
        bindAnalysisUseCase()
    }

    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }
        cameraProvider?.unbind(previewUseCase)
        val builder = Preview.Builder()
        previewUseCase = builder.build()
        previewUseCase?.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        cameraProvider?.unbind(analysisUseCase)
        imageProcessor?.stop()
        imageProcessor = BarcodeScannerProcessor(
            callback, drawOverlay, drawBanner, focusBoxSize, supportedBarcodeFormats
        )

        val builder = ImageAnalysis.Builder()
        builder.setTargetResolution(targetResolution)
        analysisUseCase = builder.build()

        needUpdateGraphicOverlayImageSourceInfo = true

        analysisUseCase?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just runs the analyzer itself on main thread.
            ContextCompat.getMainExecutor(context)
        ) { imageProxy: ImageProxy ->
            if (needUpdateGraphicOverlayImageSourceInfo) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                if (rotationDegrees == 0 || rotationDegrees == 180) {
                    graphicOverlay.setImageSourceInfo(imageProxy.width, imageProxy.height, false)
                } else {
                    graphicOverlay.setImageSourceInfo(imageProxy.height, imageProxy.width, false)
                }
                needUpdateGraphicOverlayImageSourceInfo = false
            }

            try {
                imageProcessor?.processImageProxy(imageProxy, graphicOverlay)
            } catch (e: MlKitException) {
                Log.e("TAG", "Failed to process image. Error: " + e.localizedMessage)
            }
        }
        cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, analysisUseCase)
    }

    override fun onPause(owner: LifecycleOwner) {
        imageProcessor?.stop()
        super.onPause(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        bindAllCameraUseCases()
        super.onResume(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        imageProcessor?.stop()
        lifecycleOwner.lifecycle.removeObserver(this)
        super.onDestroy(owner)
    }
}

/**
 * Listen to new scanned barcodes
 */
fun interface MLBarcodeCallback {
    /**
     * @param displayValue Returns barcode value in a user-friendly format.
     *  This method may omit some of the information encoded in the barcode. For example, if getRawValue() returns 'MEBKM:TITLE:Google;URL://www.google.com;;', the display value might be '//www.google.com'.
     *  This value may be multiline, for example, when line breaks are encoded into the original TEXT barcode value. May include the supplement value.
     *
     * @param rawValue Returns barcode value as it was encoded in the barcode. Structured values are not parsed, for example: 'MEBKM:TITLE:Google;URL://www.google.com;;'.
     */
    fun onNewBarcodeScanned(displayValue: String, rawValue: String)
}
