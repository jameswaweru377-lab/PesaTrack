package com.kedevjimmy.pesatrack.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileCoverCropPolicyTest {
    @Test fun portraitSourceCropsVerticallyAtCenter() {
        val crop = profileCoverCropRect(600, 1200)
        assertEquals(CoverCropRect(0, 471, 600, 258), crop)
    }

    @Test fun wideSourceCropsHorizontallyAtCenter() {
        val crop = profileCoverCropRect(1600, 500)
        assertEquals(CoverCropRect(218, 0, 1164, 500), crop)
    }

    @Test fun invalidDimensionsAreRejected() {
        assertNull(profileCoverCropRect(0, 500))
        assertNull(profileCoverCropRect(600, -1))
    }
}
