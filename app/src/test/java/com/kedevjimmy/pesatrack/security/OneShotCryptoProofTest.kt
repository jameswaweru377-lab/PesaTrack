package com.kedevjimmy.pesatrack.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

class OneShotCryptoProofTest {
    private fun readyCipher(): Cipher {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    @Test fun missingCryptoObjectCannotAuthorize() {
        val proof = OneShotCryptoProof(readyCipher())
        assertFalse(proof.verify(null))
    }

    @Test fun aDifferentCipherCannotAuthorize() {
        val proof = OneShotCryptoProof(readyCipher())
        assertFalse(proof.verify(readyCipher()))
    }

    @Test fun validOperationAuthorizesExactlyOnce() {
        val cipher = readyCipher()
        val proof = OneShotCryptoProof(cipher)
        assertTrue(proof.verify(cipher))
        assertFalse(proof.verify(cipher))
    }

    @Test fun failedCryptoOperationCannotAuthorize() {
        val cipher = readyCipher()
        cipher.doFinal(byteArrayOf(1)) // The operation has already been consumed.
        assertFalse(OneShotCryptoProof(cipher).verify(cipher))
    }

    @Test fun aMismatchedResultConsumesTheAuthorizationAttempt() {
        val cipher = readyCipher()
        val proof = OneShotCryptoProof(cipher)
        assertFalse(proof.verify(null))
        assertFalse(proof.verify(cipher))
    }
}
