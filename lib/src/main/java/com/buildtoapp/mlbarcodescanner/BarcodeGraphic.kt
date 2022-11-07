package com.buildtoapp.mlbarcodescanner

import android.graphics.*
import com.google.mlkit.vision.barcode.common.Barcode
import kotlin.math.max
import kotlin.math.min

/** Graphic instance for rendering Barcode position and content information in an overlay view. */
internal class BarcodeGraphic(
    private val barcode: Barcode?,
    private val overlay: GraphicOverlay,
    private val drawOverlay: Boolean,
    private val drawBanner: Boolean,
    private val focusBoxSize: Int,
    private val result: BarcodeGraphicFocusResult,
) : GraphicOverlay.Graphic(overlay) {
    private val rectPaint: Paint = Paint()
    private val barcodePaint: Paint
    private val labelPaint: Paint

    init {
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = STROKE_WIDTH
        barcodePaint = Paint()
        barcodePaint.color = Color.BLACK
        barcodePaint.textSize = TEXT_SIZE
        labelPaint = Paint()
        labelPaint.color = Color.WHITE
        labelPaint.style = Paint.Style.FILL
    }

    /**
     * Draws the barcode block annotations for position, size, and raw value on the supplied canvas.
     */
    override fun draw(canvas: Canvas) {
        checkNotNull(barcode) { "Attempting to draw a null barcode." }
        // Draws the bounding box around the BarcodeBlock.
        val rect = RectF(barcode.boundingBox)
        // If the image is flipped, the left will be translated to right, and the right to left.
        val x0 = translateX(rect.left)
        val x1 = translateX(rect.right)
        rect.left = min(x0, x1)
        rect.right = max(x0, x1)
        rect.top = translateY(rect.top)
        rect.bottom = translateY(rect.bottom)
        val focusArea = calculateFocusArea()
        val isInFocusArea = targetIsInFocusArea(focusArea, rect)
        if (isInFocusArea) {
            rectPaint.color = Color.WHITE
        } else {
            rectPaint.color = Color.RED
        }
        if (isInFocusArea && drawBanner) {
            // Draws other object info.
            val lineHeight = TEXT_SIZE + 2 * STROKE_WIDTH
            val textWidth = barcodePaint.measureText(barcode.displayValue)
            canvas.drawRect(
                rect.left - STROKE_WIDTH,
                rect.top - lineHeight,
                rect.left + textWidth + 2 * STROKE_WIDTH,
                rect.top,
                labelPaint
            )
            // Renders the barcode at the bottom of the box.
            canvas.drawText(barcode.displayValue!!, rect.left, rect.top - STROKE_WIDTH, barcodePaint)
        }
        if (drawOverlay) {
            canvas.drawRect(rect, rectPaint)
        }
        result.onGraphicDrawnInFocusArea(isInFocusArea)
    }

    private fun calculateFocusArea(): RectF {
        val height = overlay.measuredHeight.toFloat()
        val width = overlay.measuredWidth.toFloat()
        val left = (width / 2) - (focusBoxSize / 2)
        val right = (width / 2) + (focusBoxSize / 2)
        val top = (height / 2) - (focusBoxSize / 2)
        val bottom = (height / 2) + (focusBoxSize / 2)
        return RectF(left, top, right, bottom)
    }

    private fun targetIsInFocusArea(focus: RectF, target: RectF): Boolean {
        return (focus.left < target.left) && (target.right < focus.right)
                && (focus.top < target.top) && (focus.bottom > target.bottom)
    }

    companion object {
        private const val TEXT_SIZE = 54.0f
        private const val STROKE_WIDTH = 4.0f
    }
}

fun interface BarcodeGraphicFocusResult {
    fun onGraphicDrawnInFocusArea(isInFocus: Boolean)
}
