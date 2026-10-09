package com.kedevjimmy.pesatrack.data.remote

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityEnrichmentHttpClientResourceTest {
    @Test
    fun oversizedUnknownLengthResponseFailsBeforeParsing() {
        val connection = FakeConnection(-1L)
        val result = EntityEnrichmentHttpClient(
            endpoint = "https://example.test/v1/entity/resolve",
            openConnection = { connection }
        ).fetch("ACME HARDWARE LTD")
        assertEquals(EntityEnrichmentFetchResult.Retryable("io"), result)
        assertTrue(connection.disconnected)
        assertTrue(connection.request.toString("UTF-8").contains("ACME HARDWARE LTD"))
    }

    @Test
    fun declaredOversizedResponseIsRejectedWithoutReading() {
        val connection = FakeConnection(65_537L)
        val result = EntityEnrichmentHttpClient(
            endpoint = "https://example.test/v1/entity/resolve",
            openConnection = { connection }
        ).fetch("ACME HARDWARE LTD")
        assertEquals(EntityEnrichmentFetchResult.PermanentFailure("response_too_large"), result)
        assertEquals(0, connection.bodyReads)
        assertTrue(connection.disconnected)
    }

    private class FakeConnection(private val length: Long) : HttpURLConnection(
        URL("https://example.test/v1/entity/resolve")
    ) {
        val request = ByteArrayOutputStream()
        var disconnected = false
        var bodyReads = 0
        override fun getResponseCode() = 200
        override fun getContentLengthLong() = length
        override fun getOutputStream() = request
        override fun getInputStream(): ByteArrayInputStream {
            bodyReads++
            return ByteArrayInputStream(ByteArray(65_537))
        }
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun connect() = Unit
    }
}
