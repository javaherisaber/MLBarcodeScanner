package nl.storegear.android.mlbarcodescanner.mlkit

import android.graphics.Rect
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import nl.storegear.android.mlbarcodescanner.util.BarcodeGraphic
import nl.storegear.android.mlbarcodescanner.util.MetricUtils.dpToPx

/** Barcode Detector Demo.  */
class BarcodeScannerProcessor(private val callback: CameraXBarcodeCallback, private val offset: Int) : VisionProcessorBase<List<Barcode>>() {

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
            val height = graphicOverlay.measuredHeight
            val width = graphicOverlay.measuredWidth
            val area = dpToPx(264)
            val left = (width / 2) - (area / 2)
            val right = (width / 2) + (area / 2)
            val top = (height / 2) - (area / 2)
            val bottom = (height / 2) + (area / 2)
            val focusArea = Rect(left, top, right, bottom)
            val box = barcode.boundingBox ?: return
            val barcodeRect = Rect(dpToPx(box.left), dpToPx(box.top), dpToPx(box.right), dpToPx(box.bottom))
            val displayValue = barcode.displayValue
            val rawValue = barcode.rawValue
            if (barcodeIsInFocusArea(focusArea, barcodeRect) && displayValue != null && rawValue != null) {
                callback.onNewBarcodeScanned(displayValue, rawValue)
            }

            // todo: if you don't want to draw the banner remove this line and [BarcodeGraphic] class
            graphicOverlay.add(BarcodeGraphic(graphicOverlay, barcode))
        }
    }

    private fun barcodeIsInFocusArea(focus: Rect, target: Rect): Boolean {
        return (focus.left < target.left - target.width() + offset) && (target.right < focus.right + offset)
                && (focus.top < target.top + offset) && (focus.bottom > target.bottom + target.height() - offset)
    }

    override fun onFailure(e: Exception) {
        // do nothing
    }
}

fun interface CameraXBarcodeCallback {
    fun onNewBarcodeScanned(displayValue: String, rawValue: String)
}
