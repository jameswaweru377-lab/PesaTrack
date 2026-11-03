package com.kedevjimmy.pesatrack.data.profile

import kotlin.math.roundToInt

/** Center-crop a selected photo to the private profile's wide cover ratio. */
internal data class CoverCropRect(val left: Int, val top: Int, val width: Int, val height: Int)

internal fun profileCoverCropRect(width: Int, height: Int): CoverCropRect? {
    if (width <= 0 || height <= 0) return null
    val coverRatio = 1024f / 440f
    val sourceRatio = width.toFloat() / height
    return if (sourceRatio > coverRatio) {
        val cropWidth = (height * coverRatio).roundToInt().coerceIn(1, width)
        CoverCropRect((width - cropWidth) / 2, 0, cropWidth, height)
    } else {
        val cropHeight = (width / coverRatio).roundToInt().coerceIn(1, height)
        CoverCropRect(0, (height - cropHeight) / 2, width, cropHeight)
    }
}
