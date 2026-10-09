package com.kedevjimmy.pesatrack.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Invented fixtures independently calculated with Python hashlib.pbkdf2_hmac. */
public final class Pbkdf2Sha256Vectors {
    private static final Object[][] VECTORS = {
        {"password", "73616c74", 1, 256, "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b"},
        {"password", "73616c74", 2, 256, "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43"},
        {"password", "73616c74", 4096, 320, "c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134af7ad98c1b458ce3f"},
        {"p\u00e1ssphrase\ud83d\udd12", "696e76656e7465642d73616c74", 2, 320, "430af8069d9735473bfc8352ab38fd9f5c82b9534f773ca905483bdd5192b10634438054157d3053"},
        {"246810", "000102030405060708090a0b0c0d0e0f", 120000, 256, "05e793e95caa62590a88efe48675ad842d471c8754bb387be1030fc683d36106"},
        {"invented-strong-passphrase", "101112131415161718191a1b1c1d1e1f", 180000, 256, "fe5782d77afff15316f0fe274e204966d6bee40901172b1509e5ac2d2ed7ebd4"},
        {"", "73616c74", 2, 256, "62384466264daadc4144018c6bd864648272b34da8980d31521ffcce92ae003b"},
        {"long-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "73616c74", 2, 320, "8fe9b10d50e3806eefd07b7c1b23e82d05fe6b77ecfd344d93ae1a7b82cc8f27a868892b940483e3"},
    };

    public static void knownVectors() throws Exception {
        for (Object[] vector : VECTORS) {
            byte[] result = Pbkdf2Sha256.derive(((String) vector[0]).toCharArray(), hex((String) vector[1]), (int) vector[2], (int) vector[3]);
            equal(hex((String) vector[4]), result, "known vector iterations=" + vector[2] + " bits=" + vector[3]);
        }
    }

    public static void nativeEquivalence() throws Exception {
        for (Object[] vector : VECTORS) {
            char[] password = ((String) vector[0]).toCharArray();
            byte[] salt = hex((String) vector[1]);
            PBEKeySpec spec = new PBEKeySpec(password, salt, (int) vector[2], (int) vector[3]);
            try {
                byte[] nativeKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
                equal(nativeKey, Pbkdf2Sha256.derive(password, salt, (int) vector[2], (int) vector[3]), "native equivalence");
            } finally { spec.clearPassword(); Arrays.fill(password, '\0'); }
        }
    }

    public static void preservesCallerInputs() throws Exception {
        char[] password = "invented-password".toCharArray();
        byte[] salt = "invented-salt".getBytes(StandardCharsets.UTF_8);
        char[] passwordCopy = password.clone();
        byte[] saltCopy = salt.clone();
        Pbkdf2Sha256.derive(password, salt, 2, 256);
        if (!Arrays.equals(passwordCopy, password) || !Arrays.equals(saltCopy, salt)) throw new AssertionError("caller inputs mutated");
    }

    public static void rejectsInvalidParameters() throws Exception {
        invalid(() -> Pbkdf2Sha256.derive("password".toCharArray(), new byte[] {1}, 0, 256));
        invalid(() -> Pbkdf2Sha256.derive("password".toCharArray(), new byte[] {1}, -1, 256));
        invalid(() -> Pbkdf2Sha256.derive("password".toCharArray(), new byte[] {1}, 2, 0));
        invalid(() -> Pbkdf2Sha256.derive("password".toCharArray(), new byte[] {1}, 2, 255));
    }

    public static void main(String[] args) throws Exception {
        knownVectors(); nativeEquivalence(); preservesCallerInputs(); rejectsInvalidParameters();
        System.out.println("PBKDF2-SHA256 checks passed: 8 known vectors, 8 native-equivalence cases, caller-input preservation, 4 invalid-parameter cases.");
    }
    private interface Action { void run() throws Exception; }
    private static void invalid(Action action) throws Exception {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("invalid parameters accepted");
    }
    private static void equal(byte[] expected, byte[] actual, String name) {
        if (!Arrays.equals(expected, actual)) throw new AssertionError(name);
    }
    private static byte[] hex(String value) {
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        return bytes;
    }
}
