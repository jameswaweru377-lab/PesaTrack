package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test
import java.io.InputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class DocumentCopySessionTest {
    @Test fun cancellationClosesStalledInputAndDeletesPartialFile() {
        val reading = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        val failure = AtomicReference<Throwable>()
        val file = File.createTempFile("synthetic-copy-", ".pdf")
        val input = object : InputStream() {
            override fun read(): Int { reading.countDown(); closed.await(); return -1 }
            override fun close() { closed.countDown() }
        }
        val session = DocumentCopySession({ input })
        Thread {
            try { session.copyTo(file) } catch (e: Throwable) { failure.set(e) } finally { stopped.countDown() }
        }.start()
        assertTrue(reading.await(2, TimeUnit.SECONDS))
        session.cancel()
        assertTrue(stopped.await(2, TimeUnit.SECONDS))
        assertNotNull(failure.get())
        assertFalse(file.exists())
    }
    @Test fun cancelledBeforeOpenCannotCreateOrRetainDocument() {
        var opened = false
        val file = File.createTempFile("synthetic-copy-", ".pdf")
        val session = DocumentCopySession({ opened = true; "%PDF-test".byteInputStream() })
        session.cancel()
        assertThrows(java.util.concurrent.CancellationException::class.java) { session.copyTo(file) }
        assertFalse(opened)
        assertFalse(file.exists())
    }
    @Test fun validBytesAreCopiedButOversizeOrWrongSignatureFail() {
        val file = File.createTempFile("synthetic-copy-", ".pdf")
        try {
            DocumentCopySession({ "%PDF-test".byteInputStream() }).copyTo(file)
            assertEquals("%PDF-test", file.readText())
            assertThrows(IllegalArgumentException::class.java) { DocumentCopySession({ "not a PDF".byteInputStream() }).copyTo(file) }
            val large=ByteArray(3 * 1024 * 1024 + 1).also { "%PDF-".toByteArray().copyInto(it) }
            val error=assertThrows(PdfReadException::class.java) { DocumentCopySession({ large.inputStream() }).copyTo(file) }
            assertEquals(PdfFailure.TOO_LARGE,error.failure)
        } finally { file.delete() }
    }
}
