package com.kedevjimmy.pesatrack.ui.screens

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

/** Guards the approved historic P silhouette, isolated to the brown Home design. */
class BrownHomeBrandMarkContractTest {
    @Test
    fun homeBrandMarkKeepsTheOriginalPurplePGeometryWithBrownPalette() {
        val relativePath = "src/main/res/drawable/ic_pesatrack_home_mark.xml"
        val asset = sequenceOf(File(relativePath), File("app/$relativePath"))
            .firstOrNull { it.isFile }
            ?: error("Home-specific brand mark is missing")

        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(asset)
        val paths = document.getElementsByTagName("path")
        assertEquals(3, paths.length)

        val originalGeometry = listOf(
            "M14,38 L14,31 C14,19 23,12 35,12 L61,12 C74,12 83,21 83,35 L83,38 Z",
            "M14,39 C23,48 32,56 42,64 L42,80 C42,84 40,86 36,86 L21,86 C17,86 14,83 14,79 Z",
            "M42,64 C42,51 54,43 83,39 C83,55 72,64 55,64 Z"
        )
        val brownColors = listOf("#68351D", "#92502D", "#A86F4B")
        val androidNamespace = "http://schemas.android.com/apk/res/android"
        for (index in originalGeometry.indices) {
            val attributes = paths.item(index).attributes
            assertEquals(originalGeometry[index],
                attributes.getNamedItemNS(androidNamespace, "pathData").nodeValue)
            assertEquals(brownColors[index],
                attributes.getNamedItemNS(androidNamespace, "fillColor").nodeValue)
        }
        assertEquals("100", document.documentElement.getAttributeNS(androidNamespace, "viewportWidth"))
    }
}
