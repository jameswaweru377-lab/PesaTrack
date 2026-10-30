package com.kedevjimmy.pesatrack.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfilePhotoCropPolicyTest {
    @Test
    fun squareImageFitsWithoutDiscardingPixels() {
        assertEquals(CropRect(0, 0, 640), ProfilePhotoCropPolicy.cropRect(640, 640, 1f, 0f, 0f))
    }

    @Test
    fun landscapeAndPortraitImagesUseCenteredSquare() {
        assertEquals(CropRect(200, 0, 400), ProfilePhotoCropPolicy.cropRect(800, 400, 1f, 0f, 0f))
        assertEquals(CropRect(0, 200, 400), ProfilePhotoCropPolicy.cropRect(400, 800, 1f, 0f, 0f))
    }

    @Test
    fun zoomAndPanStayWithinSourceBounds() {
        assertEquals(CropRect(300, 100, 200), ProfilePhotoCropPolicy.cropRect(800, 400, 2f, 0f, 0f))
        assertEquals(CropRect(600, 0, 200), ProfilePhotoCropPolicy.cropRect(800, 400, 2f, 30f, -30f))
        assertEquals(CropRect(0, 200, 200), ProfilePhotoCropPolicy.cropRect(800, 400, 2f, -30f, 30f))
    }

    @Test
    fun badZoomFallsBackToUnzoomedImage() {
        assertEquals(CropRect(200, 0, 400), ProfilePhotoCropPolicy.cropRect(800, 400, Float.NaN, 0f, 0f))
    }

    @Test
    fun invalidDimensionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ProfilePhotoCropPolicy.cropRect(0, 400, 1f, 0f, 0f)
        }
    }
}
