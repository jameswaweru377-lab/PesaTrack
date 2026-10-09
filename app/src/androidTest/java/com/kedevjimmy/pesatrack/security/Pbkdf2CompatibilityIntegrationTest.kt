package com.kedevjimmy.pesatrack.security

import android.os.Build
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Pbkdf2CompatibilityIntegrationTest {
    @Test fun apiSelectorMatchesFixedProductionPinVector() {
        assertArrayEquals(
            hex("05e793e95caa62590a88efe48675ad842d471c8754bb387be1030fc683d36106"),
            PasswordKeyDerivation.derive("246810", ByteArray(16) { it.toByte() }, 120_000, 256)
        )
    }

    @Test fun apiSelectorMatchesFixedUtf8UnicodeMultiBlockVector() {
        assertArrayEquals(
            hex("430af8069d9735473bfc8352ab38fd9f5c82b9534f773ca905483bdd5192b10634438054157d3053"),
            PasswordKeyDerivation.derive("p\u00e1ssphrase\ud83d\udd12", "invented-salt".toByteArray(Charsets.UTF_8), 2, 320)
        )
    }

    @Test fun nativeAndFallbackAgreeOnSupportedAndroidApis() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val salt = ByteArray(16) { (16 + it).toByte() }
        assertArrayEquals(
            PasswordKeyDerivation.derive("invented-strong-passphrase", salt, 180_000, 256),
            Pbkdf2Sha256.derive("invented-strong-passphrase".toCharArray(), salt, 180_000, 256)
        )
    }

    @Test fun apiSelectorDecryptsAndReproducesModernBackupWireCiphertext() {
        // Generated independently with JDK 17's native PBKDF2WithHmacSHA256 + AES/GCM.
        // Fixed salt/IV are test fixtures only. Payload contains no ledger or real data.
        val fixture = Base64.decode(
            "UEVTQUJLMDEAAr8gAAAAEBAREhMUFRYXGBkaGxwdHh8AAAAMAAECAwQFBgcICQoLAAAAWZ8MuYSQjGDpB0LAY9r9RCqhVj6DURBA4OWuj07E7IkupohY7Qj1LaolSURiNUR1PJxDASV1nRL32ZIpUZC9kAkAfGbG5GNtZDHhUiGb+jHxXXsDlHFmOV7P",
            Base64.NO_WRAP
        )
        val input = DataInputStream(ByteArrayInputStream(fixture))
        val magic = ByteArray(8).also { input.readFully(it) }
        assertEquals("PESABK01", String(magic, Charsets.US_ASCII))
        val iterations = input.readInt()
        assertEquals(180_000, iterations)
        val salt = ByteArray(input.readInt()).also { input.readFully(it) }
        val iv = ByteArray(input.readInt()).also { input.readFully(it) }
        val ciphertext = ByteArray(input.readInt()).also { input.readFully(it) }
        val key = SecretKeySpec(PasswordKeyDerivation.derive("invented-strong-passphrase", salt, iterations, 256), "AES")
        val plaintext = "{\"formatVersion\":1,\"schemaVersion\":33,\"fixture\":\"invented compatibility\"}".toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        assertArrayEquals(plaintext, cipher.doFinal(ciphertext))
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        assertArrayEquals(ciphertext, cipher.doFinal(plaintext))
    }

    private fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
