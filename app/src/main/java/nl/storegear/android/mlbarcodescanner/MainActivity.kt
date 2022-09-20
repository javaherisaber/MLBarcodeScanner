package nl.storegear.android.mlbarcodescanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.common.MlKitException
import nl.storegear.android.mlbarcodescanner.databinding.ActivityMainBinding
import nl.storegear.android.mlbarcodescanner.mlkit.BarcodeScannerProcessor
import nl.storegear.android.mlbarcodescanner.mlkit.CameraXBarcodeCallback
import nl.storegear.android.mlbarcodescanner.mlkit.CameraXViewModel
import nl.storegear.android.mlbarcodescanner.mlkit.VisionImageProcessor
import nl.storegear.android.mlbarcodescanner.util.MetricUtils.dpToPx

class MainActivity : AppCompatActivity(), CameraXBarcodeCallback {
    lateinit var binding: ActivityMainBinding

    private lateinit var cameraXViewModel: CameraXViewModel
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraSelector: CameraSelector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions()
        }
        cameraXViewModel = ViewModelProvider(this).get(CameraXViewModel::class.java)

        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        cameraXViewModel.processCameraProvider.observe(this) { provider: ProcessCameraProvider? ->
            cameraProvider = provider
            bindAllCameraUseCases()
        }
    }

    override fun onPause() {
        super.onPause()
        imageProcessor?.stop()
    }

    override fun onResume() {
        super.onResume()
        bindAllCameraUseCases()
    }

    override fun onDestroy() {
        imageProcessor?.stop()
        super.onDestroy()
    }

    override fun onNewBarcodeScanned(displayValue: String, rawValue: String) {
        // todo: you can process your barcode here
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
        previewUseCase?.setSurfaceProvider(binding.previewViewCameraScanning.surfaceProvider)
        cameraProvider?.bindToLifecycle(this, cameraSelector, previewUseCase)
    }

    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        cameraProvider?.unbind(analysisUseCase)
        imageProcessor?.stop()
        imageProcessor = BarcodeScannerProcessor(this, offset = dpToPx(16), focusBoxSize = dpToPx(264))

        val builder = ImageAnalysis.Builder()
        analysisUseCase = builder.build()

        needUpdateGraphicOverlayImageSourceInfo = true

        analysisUseCase?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just runs the analyzer itself on main thread.
            ContextCompat.getMainExecutor(this)
        ) { imageProxy: ImageProxy ->
            if (needUpdateGraphicOverlayImageSourceInfo) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                if (rotationDegrees == 0 || rotationDegrees == 180) {
                    binding.graphicOverlay.setImageSourceInfo(imageProxy.width, imageProxy.height, false)
                } else {
                    binding.graphicOverlay.setImageSourceInfo(imageProxy.height, imageProxy.width, false)
                }
                needUpdateGraphicOverlayImageSourceInfo = false
            }

            try {
                imageProcessor?.processImageProxy(imageProxy, binding.graphicOverlay)
            } catch (e: MlKitException) {
                Log.e("TAG", "Failed to process image. Error: " + e.localizedMessage)
            }
        }
        cameraProvider?.bindToLifecycle(this, cameraSelector, analysisUseCase)
    }


    // GET PERMISSIONS //
    private fun allRuntimePermissionsGranted(): Boolean {
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(this, it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUESTS)
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }

    companion object {
        private const val TAG = "MLBarcodeScanner"
        private const val PERMISSION_REQUESTS = 1

        private val REQUIRED_RUNTIME_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
}