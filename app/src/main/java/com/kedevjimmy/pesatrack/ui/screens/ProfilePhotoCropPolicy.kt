package com.kedevjimmy.pesatrack.ui.screens

import kotlin.math.min
import kotlin.math.roundToInt

/** Source-pixel rectangle for a square profile photo. Pan is normalized to -1..1. */
internal data class CropRect(val left: Int, val top: Int, val size: Int)

internal object ProfilePhotoCropPolicy {
    fun cropRect(imageWidth: Int, imageHeight: Int, zoom: Float, panX: Float, panY: Float): CropRect {
        require(imageWidth > 0 && imageHeight > 0) { "Image dimensions must be positive." }
        val safeZoom = zoom.takeIf { it.isFinite() }?.coerceIn(1f, 4f) ?: 1f
        val size = (min(imageWidth, imageHeight) / safeZoom).roundToInt().coerceAtLeast(1)
        val horizontalRange = imageWidth - size
        val verticalRange = imageHeight - size
        val horizontalFraction = (panX.takeIf(Float::isFinite) ?: 0f).coerceIn(-1f, 1f)
        val verticalFraction = (panY.takeIf(Float::isFinite) ?: 0f).coerceIn(-1f, 1f)
        val left = (horizontalRange * (horizontalFraction + 1f) / 2f).roundToInt()
            .coerceIn(0, horizontalRange)
        val top = (verticalRange * (verticalFraction + 1f) / 2f).roundToInt()
            .coerceIn(0, verticalRange)
        return CropRect(left, top, size)
    }
}
