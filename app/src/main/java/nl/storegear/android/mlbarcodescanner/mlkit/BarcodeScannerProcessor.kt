package nl.storegear.android.mlbarcodescanner.mlkit

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/** Barcode Detector Demo.  */
class BarcodeScannerProcessor(
    private val callback: CameraXBarcodeCallback,
    private val focusBoxSize: Int
) : VisionProcessorBase<List<Barcode>>() {

    // Note that if you know which format of barcode your app is dealing with, detection will be
    // faster to specify the supported barcode formats one by one, e.g.
    // BarcodeScannerOptions.Builder()
    //     .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
    //     .build();
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()

    override fun stop() {
        super.stop()
        barcodeScanner.close()
    }

    override fun detectInImage(image: InputImage): Task<List<Barcode>> {
        return barcodeScanner.process(image)
    }

    override fun onSuccess(results: List<Barcode>, graphicOverlay: GraphicOverlay) {
        for (barcode in results) {
            graphicOverlay.add(BarcodeGraphic(graphicOverlay, barcode, focusBoxSize) { isInFocus ->
                val displayValue = barcode.displayValue
                val rawValue = barcode.rawValue
                if (isInFocus && displayValue != null && rawValue != null) {
                    callback.onNewBarcodeScanned(displayValue, rawValue)
                }
            })
        }
    }

    override fun onFailure(e: Exception) {
        // do nothing
    }
}

fun interface CameraXBarcodeCallback {
    fun onNewBarcodeScanned(displayValue: String, rawValue: String)
}
