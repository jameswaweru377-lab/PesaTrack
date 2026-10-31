package com.kedevjimmy.pesatrack.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature

class OneShotCryptoProofTest {
    private fun keyPair(): KeyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(256) }.generateKeyPair()

    private fun signer(keyPair: KeyPair): Signature =
        Signature.getInstance("SHA256withECDSA").apply { initSign(keyPair.private) }

    @Test fun missingCryptoObjectCannotAuthorize() {
        val pair = keyPair()
        val proof = OneShotCryptoProof(signer(pair), pair.public)
        assertFalse(proof.verify(null))
    }

    @Test fun aDifferentSignatureObjectCannotAuthorize() {
        val pair = keyPair()
        val proof = OneShotCryptoProof(signer(pair), pair.public)
        assertFalse(proof.verify(signer(pair)))
    }

    @Test fun validOperationAuthorizesExactlyOnce() {
        val pair = keyPair()
        val signature = signer(pair)
        val proof = OneShotCryptoProof(signature, pair.public)
        assertTrue(proof.verify(signature))
        assertFalse(proof.verify(signature))
    }

    @Test fun aSignatureWithoutTheMatchingPublicKeyCannotAuthorize() {
        val pair = keyPair()
        val signature = signer(pair)
        assertFalse(OneShotCryptoProof(signature, keyPair().public).verify(signature))
    }

    @Test fun anUninitializedSignatureCannotAuthorize() {
        val signature = Signature.getInstance("SHA256withECDSA")
        assertFalse(OneShotCryptoProof(signature, keyPair().public).verify(signature))
    }

    @Test fun aMismatchedResultConsumesTheAuthorizationAttempt() {
        val pair = keyPair()
        val signature = signer(pair)
        val proof = OneShotCryptoProof(signature, pair.public)
        assertFalse(proof.verify(null))
        assertFalse(proof.verify(signature))
    }
}
