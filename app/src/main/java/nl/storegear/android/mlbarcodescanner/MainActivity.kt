package nl.storegear.android.mlbarcodescanner

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.buildtoapp.mlbarcodescanner.MLBarcodeCallback
import com.buildtoapp.mlbarcodescanner.MLBarcodeScanner
import nl.storegear.android.mlbarcodescanner.databinding.ActivityMainBinding
import nl.storegear.android.mlbarcodescanner.util.MetricUtils
import nl.storegear.android.mlbarcodescanner.util.PermissionUtils

class MainActivity : AppCompatActivity(), MLBarcodeCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var barcodeScanner: MLBarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!PermissionUtils.allRuntimePermissionsGranted(this, REQUIRED_RUNTIME_PERMISSIONS)) {
            PermissionUtils.getRuntimePermissions(this, REQUIRED_RUNTIME_PERMISSIONS)
        }
        initBarcodeScanner()
    }

    private fun initBarcodeScanner() {
        barcodeScanner = MLBarcodeScanner(
            callback = this,
            focusBoxSize = MetricUtils.dpToPx(264),
            graphicOverlay = binding.graphicOverlay,
            previewView = binding.previewViewCameraScanning,
            lifecycleOwner = this,
            context = this,
            drawOverlay = true, // show rectangle around detected barcode
            drawBanner = true // show detected barcode value on top of it
        )
    }

    override fun onNewBarcodeScanned(displayValue: String, rawValue: String) {
        // todo: you can process your barcode here
    }

    companion object {
        private val REQUIRED_RUNTIME_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}