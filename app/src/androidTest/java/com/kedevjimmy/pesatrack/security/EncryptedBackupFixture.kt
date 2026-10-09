package com.kedevjimmy.pesatrack.security

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Re-authenticates deliberately malformed JSON using the exported PESABK01 wire format. */
internal object EncryptedBackupFixture {
    fun rewrite(
        exported: ByteArray,
        passphrase: String,
        mutate: (JsonObject) -> Unit
    ): ByteArray {
        val input = DataInputStream(ByteArrayInputStream(exported))
        val magic = ByteArray(8).also { input.readFully(it) }
        require(String(magic, Charsets.US_ASCII) == "PESABK01")
        val iterations = input.readInt()
        val salt = ByteArray(input.readInt()).also { input.readFully(it) }
        val iv = ByteArray(input.readInt()).also { input.readFully(it) }
        val ciphertext = ByteArray(input.readInt()).also { input.readFully(it) }
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, 256)
        val key = try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val payload = JsonParser.parseString(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
            .asJsonObject
        mutate(payload)
        val replacementIv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, replacementIv))
        val rewritten = cipher.doFinal(payload.toString().toByteArray(Charsets.UTF_8))
        return ByteArrayOutputStream().also { buffer ->
            DataOutputStream(buffer).use { output ->
                output.write(magic)
                output.writeInt(iterations)
                output.writeInt(salt.size)
                output.write(salt)
                output.writeInt(replacementIv.size)
                output.write(replacementIv)
                output.writeInt(rewritten.size)
                output.write(rewritten)
            }
        }.toByteArray()
    }
}

