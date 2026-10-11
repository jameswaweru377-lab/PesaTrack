package com.kedevjimmy.pesatrack.domain.evidence

import org.junit.Assert.*
import org.junit.Test

class PdfTextChunkAssemblerTest {
    @Test fun orderedChunksCanCarryHalfMillionCharactersWithoutOneLargeBinderMessage() {
        val a=PdfTextChunkAssembler()
        val big="p".repeat(500_000)
        big.chunked(PdfTextChunkAssembler.CHUNK_CHARACTERS).forEach {a.add(0,it)}
        val result=a.finish(1)
        assertEquals(500_000,result.pages.single().length)
        assertThrows(PdfReadException::class.java) {a.add(0,"x")}
    }
    @Test fun multiPageOrderAndCompletenessAreMandatory() {
        val a=PdfTextChunkAssembler()
        a.add(0,"page ")
        a.add(0,"one")
        a.add(1,"")
        a.add(2,"page three")
        val review=a.finish(3)
        assertEquals(listOf("page one","","page three"),review.pages)
        assertEquals(listOf(2),review.emptyPageNumbers)
        assertThrows(IllegalArgumentException::class.java) {a.finish(4)}
        assertThrows(IllegalArgumentException::class.java) {a.add(0,"late")}
    }
    @Test fun oversizedChunkOrPageIndexFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfTextChunkAssembler().add(0,"x".repeat(PdfTextChunkAssembler.CHUNK_CHARACTERS+1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PdfTextChunkAssembler().add(PdfTextReviewPolicy.MAX_PAGES,"x")
        }
    }
}
