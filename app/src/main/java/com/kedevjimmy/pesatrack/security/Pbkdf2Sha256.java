package com.kedevjimmy.pesatrack.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * PBKDF2-HMAC-SHA256 compatibility for Android API 24/25.
 * Implements RFC 8018 section 5.2 using platform HmacSHA256, with UTF-8 passwords.
 * https://www.rfc-editor.org/rfc/rfc8018#section-5.2
 * This is the same KDF and parameters as PBKDF2WithHmacSHA256, not a weaker fallback.
 */
public final class Pbkdf2Sha256 {
    private Pbkdf2Sha256() {}

    public static byte[] derive(char[] password, byte[] salt, int iterations, int keyBits)
            throws GeneralSecurityException {
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(salt, "salt");
        if (salt.length == 0 || iterations <= 0 || keyBits <= 0 || keyBits % 8 != 0) {
            throw new IllegalArgumentException("Invalid PBKDF2 parameters");
        }
        byte[] passwordBytes = new String(password).getBytes(StandardCharsets.UTF_8);
        // An empty HMAC key and a one-zero-byte key pad to the identical zero block.
        // SecretKeySpec rejects empty arrays, while PBKDF2 permits empty passwords.
        byte[] macKey = passwordBytes.length == 0 ? new byte[1] : passwordBytes;
        byte[] blockInput = Arrays.copyOf(salt, salt.length + 4);
        byte[] derived = new byte[keyBits / 8];
        byte[] u = new byte[32];
        byte[] nextU = new byte[32];
        byte[] block = new byte[32];
        boolean success = false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(macKey, "HmacSHA256"));
            int blockCount = (derived.length + 31) / 32;
            for (int i = 1; i <= blockCount; i++) {
                // INT(i) is four octets, most significant octet first.
                blockInput[salt.length] = (byte) (i >>> 24);
                blockInput[salt.length + 1] = (byte) (i >>> 16);
                blockInput[salt.length + 2] = (byte) (i >>> 8);
                blockInput[salt.length + 3] = (byte) i;
                mac.update(blockInput);
                mac.doFinal(u, 0);
                System.arraycopy(u, 0, block, 0, 32);
                for (int round = 1; round < iterations; round++) {
                    mac.update(u);
                    mac.doFinal(nextU, 0);
                    byte[] previousU = u;
                    u = nextU;
                    nextU = previousU;
                    for (int j = 0; j < 32; j++) block[j] ^= u[j];
                }
                int offset = (i - 1) * 32;
                System.arraycopy(block, 0, derived, offset, Math.min(32, derived.length - offset));
            }
            success = true;
            return derived;
        } finally {
            Arrays.fill(passwordBytes, (byte) 0);
            Arrays.fill(macKey, (byte) 0);
            Arrays.fill(blockInput, (byte) 0);
            Arrays.fill(u, (byte) 0);
            Arrays.fill(nextU, (byte) 0);
            Arrays.fill(block, (byte) 0);
            if (!success) Arrays.fill(derived, (byte) 0);
        }
    }
}
