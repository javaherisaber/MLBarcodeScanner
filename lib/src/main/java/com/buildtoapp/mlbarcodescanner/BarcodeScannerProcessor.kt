package com.buildtoapp.mlbarcodescanner

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/** Barcode Detector Demo.  */
internal class BarcodeScannerProcessor(
    private val callback: MLBarcodeCallback,
    private val drawOverlay: Boolean,
    private val drawBanner: Boolean,
    private val focusBoxSize: Int,
    supportedBarcodeFormats: List<Int> = listOf(Barcode.FORMAT_ALL_FORMATS)
) : VisionProcessorBase<List<Barcode>>() {

    // Note that if you know which format of barcode your app is dealing with, detection will be
    // faster to specify the supported barcode formats one by one
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(
        if (supportedBarcodeFormats.size == 1) {
            BarcodeScannerOptions.Builder().setBarcodeFormats(supportedBarcodeFormats.first()).build()
        } else {
            val moreFormats = supportedBarcodeFormats.subList(1, supportedBarcodeFormats.size).toIntArray()
            BarcodeScannerOptions.Builder().setBarcodeFormats(supportedBarcodeFormats.first(), *moreFormats).build()
        }
    )

    override fun stop() {
        super.stop()
        barcodeScanner.close()
    }

    override fun detectInImage(image: InputImage): Task<List<Barcode>> {
        return barcodeScanner.process(image)
    }

    override fun onSuccess(results: List<Barcode>, graphicOverlay: GraphicOverlay) {
        for (barcode in results) {
            val graphic = BarcodeGraphic(barcode, graphicOverlay, drawOverlay, drawBanner, focusBoxSize) { isInFocus ->
                val displayValue = barcode.displayValue
                val rawValue = barcode.rawValue
                if (isInFocus && displayValue != null && rawValue != null) {
                    callback.onNewBarcodeScanned(displayValue, rawValue)
                }
            }
            graphicOverlay.add(graphic)
        }
    }

    override fun onFailure(e: Exception) {
        // do nothing
    }
}
