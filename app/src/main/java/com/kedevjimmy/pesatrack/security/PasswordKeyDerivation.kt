package com.kedevjimmy.pesatrack.security

import android.os.Build
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Keep the current SHA-256 KDF, password encoding and stored formats on every API. */
internal object PasswordKeyDerivation {
    fun derive(password: String, salt: ByteArray, iterations: Int, keyBits: Int): ByteArray {
        val characters = password.toCharArray()
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return Pbkdf2Sha256.derive(characters, salt, iterations, keyBits)
            }
            val spec = PBEKeySpec(characters, salt, iterations, keyBits)
            return try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        } finally {
            characters.fill('\u0000')
        }
    }
}
